package com.peih68.leave.leavetype;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.leavetype.web.dto.LeaveTypeRequest;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static com.peih68.leave.config.E2ECleanup.wipeUsersFkSafe;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeaveTypeE2ETest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setup() {
        wipeUsersFkSafe(jdbc);
        // Keep the 4 seeded types; remove anything we added in earlier runs
        jdbc.update("DELETE FROM leave_types WHERE code NOT IN ('ANNUAL','SICK','PERSONAL','UNPAID')");
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        UserEntity admin = userRepository.save(UserEntity.builder()
                .employeeCode("L0001").email("lt.admin@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("LT Admin").role(Role.ADMIN).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        userRepository.save(UserEntity.builder()
                .employeeCode("L0002").email("lt.emp@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("LT Emp").role(Role.EMPLOYEE).departmentId(engId)
                .managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        adminToken = login("lt.admin@demo.local");
        employeeToken = login("lt.emp@demo.local");
    }

    @Test
    void admin_fullCrudLifecycle() {
        // Create
        ResponseEntity<JsonNode> created = rest.exchange("/leave-types", HttpMethod.POST,
                authed(adminToken, new LeaveTypeRequest("WFH", "Work From Home", "Remote work",
                        new BigDecimal("5.0"), true, true)), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = created.getBody().path("data").path("id").asLong();
        assertThat(created.getBody().path("data").path("code").asText()).isEqualTo("WFH");

        // Read
        ResponseEntity<JsonNode> got = rest.exchange("/leave-types/" + id, HttpMethod.GET,
                authed(employeeToken, null), JsonNode.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Update
        ResponseEntity<JsonNode> updated = rest.exchange("/leave-types/" + id, HttpMethod.PUT,
                authed(adminToken, new LeaveTypeRequest("WFH", "Work From Home (updated)", null,
                        new BigDecimal("6.5"), true, true)), JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("data").path("defaultQuotaDays").asDouble()).isEqualTo(6.5);

        // List active includes it (plus 4 seeded)
        ResponseEntity<JsonNode> list = rest.exchange("/leave-types?activeOnly=true", HttpMethod.GET,
                authed(employeeToken, null), JsonNode.class);
        assertThat(list.getBody().path("data").toString()).contains("\"WFH\"");

        // Soft-delete
        ResponseEntity<Void> deleted = rest.exchange("/leave-types/" + id, HttpMethod.DELETE,
                authed(adminToken, null), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Hidden from active list, present when activeOnly=false
        ResponseEntity<JsonNode> active = rest.exchange("/leave-types?activeOnly=true", HttpMethod.GET,
                authed(adminToken, null), JsonNode.class);
        assertThat(active.getBody().path("data").toString()).doesNotContain("\"id\":" + id + ",");
        ResponseEntity<JsonNode> all = rest.exchange("/leave-types?activeOnly=false", HttpMethod.GET,
                authed(adminToken, null), JsonNode.class);
        assertThat(all.getBody().path("data").toString()).contains("\"id\":" + id + ",");
    }

    @Test
    void employee_cannotCreate() {
        ResponseEntity<JsonNode> resp = rest.exchange("/leave-types", HttpMethod.POST,
                authed(employeeToken, new LeaveTypeRequest("BLOCK", "Blocked", null,
                        new BigDecimal("1.0"), true, true)), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void duplicateCode_returns409() {
        rest.exchange("/leave-types", HttpMethod.POST,
                authed(adminToken, new LeaveTypeRequest("DUP", "First", null,
                        new BigDecimal("1.0"), true, true)), JsonNode.class);
        ResponseEntity<JsonNode> dup = rest.exchange("/leave-types", HttpMethod.POST,
                authed(adminToken, new LeaveTypeRequest("dup", "Second", null,
                        new BigDecimal("1.0"), true, true)), JsonNode.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void quotaStepValidation() {
        ResponseEntity<JsonNode> resp = rest.exchange("/leave-types", HttpMethod.POST,
                authed(adminToken, new LeaveTypeRequest("STEP", "Step", null,
                        new BigDecimal("5.3"), true, true)), JsonNode.class);
        // 5.3 has one fraction digit (passes @Digits) but is not a multiple of 0.5
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    private String login(String email) {
        ResponseEntity<JsonNode> resp = rest.postForEntity(
                "/auth/login", new LoginRequest(email, "Admin@12345"), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().path("data").path("accessToken").asText();
    }

    private static <T> HttpEntity<T> authed(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.add("Content-Type", "application/json");
        return new HttpEntity<>(body, headers);
    }
}
