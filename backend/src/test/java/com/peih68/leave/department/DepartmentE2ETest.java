package com.peih68.leave.department;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.department.repository.DepartmentRepository;
import com.peih68.leave.department.web.dto.DepartmentRequest;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
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
class DepartmentE2ETest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private String adminToken;
    private String employeeToken;
    private Long engId;

    @BeforeEach
    void setup() {
        wipeUsersFkSafe(jdbc);
        // Wipe any departments we added in previous runs (keep seeded ENG/SALES/HR)
        jdbc.update("DELETE FROM departments WHERE code NOT IN ('ENG','SALES','HR')");

        engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        UserEntity admin = userRepository.save(UserEntity.builder()
                .employeeCode("D0001").email("dept.admin@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("Dept Admin").role(Role.ADMIN).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        UserEntity employee = userRepository.save(UserEntity.builder()
                .employeeCode("D0002").email("dept.emp@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("Dept Employee").role(Role.EMPLOYEE).departmentId(engId)
                .managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        adminToken = login("dept.admin@demo.local");
        employeeToken = login("dept.emp@demo.local");
        assertThat(employee.getId()).isNotNull();
    }

    @Test
    void admin_fullCrudLifecycle() {
        // Create
        ResponseEntity<JsonNode> created = rest.exchange(
                "/departments", HttpMethod.POST,
                authed(adminToken, new DepartmentRequest("QA", "QA Team", null, true)),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = created.getBody().path("data").path("id").asLong();
        assertThat(created.getBody().path("data").path("code").asText()).isEqualTo("QA");

        // Read
        ResponseEntity<JsonNode> got = rest.exchange(
                "/departments/" + id, HttpMethod.GET, authed(adminToken, null), JsonNode.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(got.getBody().path("data").path("name").asText()).isEqualTo("QA Team");

        // Update
        ResponseEntity<JsonNode> updated = rest.exchange(
                "/departments/" + id, HttpMethod.PUT,
                authed(adminToken, new DepartmentRequest("QA", "Quality Assurance", null, true)),
                JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("data").path("name").asText()).isEqualTo("Quality Assurance");

        // List active includes it
        ResponseEntity<JsonNode> listed = rest.exchange(
                "/departments?activeOnly=true&size=100", HttpMethod.GET,
                authed(employeeToken, null), JsonNode.class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody().path("data").path("content").toString()).contains("\"QA\"");

        // Soft-delete
        ResponseEntity<Void> deleted = rest.exchange(
                "/departments/" + id, HttpMethod.DELETE, authed(adminToken, null), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // List default (activeOnly=true) hides it
        ResponseEntity<JsonNode> afterDelete = rest.exchange(
                "/departments?size=100", HttpMethod.GET, authed(employeeToken, null), JsonNode.class);
        assertThat(afterDelete.getBody().path("data").path("content").toString()).doesNotContain("\"id\":" + id + ",");

        // But activeOnly=false reveals it
        ResponseEntity<JsonNode> withInactive = rest.exchange(
                "/departments?activeOnly=false&size=100", HttpMethod.GET,
                authed(adminToken, null), JsonNode.class);
        assertThat(withInactive.getBody().path("data").path("content").toString()).contains("\"id\":" + id + ",");
    }

    @Test
    void employee_cannotCreate() {
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/departments", HttpMethod.POST,
                authed(employeeToken, new DepartmentRequest("BLOCKED", "Blocked", null, true)),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_duplicateCode_returns409() {
        rest.exchange("/departments", HttpMethod.POST,
                authed(adminToken, new DepartmentRequest("DUP", "First", null, true)), JsonNode.class);
        ResponseEntity<JsonNode> dup = rest.exchange("/departments", HttpMethod.POST,
                authed(adminToken, new DepartmentRequest("dup", "Second", null, true)), JsonNode.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dup.getBody().path("error").path("code").asText()).isEqualTo("CONFLICT");
    }

    @Test
    void unauthenticatedRequestReturns401() {
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/departments", HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
