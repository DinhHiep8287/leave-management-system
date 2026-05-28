package com.peih68.leave.leavebalance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceAdjustRequest;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeaveBalanceE2ETest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private String adminToken;
    private String hrToken;
    private String employeeToken;
    private Long employeeId;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM leave_balances");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM users");
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);

        UserEntity admin = userRepository.save(UserEntity.builder()
                .employeeCode("LB0001").email("lb.admin@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("LB Admin").role(Role.ADMIN).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        userRepository.save(UserEntity.builder()
                .employeeCode("LB0002").email("lb.hr@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("LB HR").role(Role.HR).departmentId(engId)
                .managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        employeeId = userRepository.save(UserEntity.builder()
                .employeeCode("LB0003").email("lb.emp@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("LB Emp").role(Role.EMPLOYEE).departmentId(engId)
                .managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build()).getId();

        adminToken = login("lb.admin@demo.local");
        hrToken = login("lb.hr@demo.local");
        employeeToken = login("lb.emp@demo.local");
    }

    @Test
    void bulkInitialize_thenAdjust_remainingCorrect() {
        // Seeded leave types that require balance: ANNUAL, SICK, PERSONAL (3) — UNPAID has requiresBalance=false
        long requiresBalanceTypes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM leave_types WHERE is_active = TRUE AND requires_balance = TRUE", Long.class);
        long activeUsers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE is_active = TRUE", Long.class);

        ResponseEntity<JsonNode> init = rest.exchange(
                "/leave-balances/initialize?year=2026", HttpMethod.POST,
                authed(adminToken, null), JsonNode.class);
        assertThat(init.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(init.getBody().path("data").path("created").asLong())
                .isEqualTo(requiresBalanceTypes * activeUsers);

        // Employee reads own balances
        ResponseEntity<JsonNode> mine = rest.exchange(
                "/users/" + employeeId + "/leave-balances?year=2026", HttpMethod.GET,
                authed(employeeToken, null), JsonNode.class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody().path("data").size()).isEqualTo((int) requiresBalanceTypes);

        // Find the ANNUAL balance id
        JsonNode annual = null;
        for (JsonNode b : mine.getBody().path("data")) {
            if ("ANNUAL".equals(b.path("leaveTypeCode").asText())) annual = b;
        }
        assertThat(annual).isNotNull();
        long annualId = annual.path("id").asLong();
        double totalBefore = annual.path("totalDays").asDouble();

        // HR adjusts +2 days
        ResponseEntity<JsonNode> adjusted = rest.exchange(
                "/leave-balances/" + annualId + "/adjust", HttpMethod.PATCH,
                authed(hrToken, new LeaveBalanceAdjustRequest(new BigDecimal("2.0"), "extra grant")),
                JsonNode.class);
        assertThat(adjusted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adjusted.getBody().path("data").path("adjustedDays").asDouble()).isEqualTo(2.0);
        assertThat(adjusted.getBody().path("data").path("remainingDays").asDouble())
                .isEqualTo(totalBefore + 2.0);

        // Audit row written
        Long auditCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'LEAVE_BALANCE_ADJUST' AND target_id = ?",
                Long.class, annualId);
        assertThat(auditCount).isEqualTo(1L);

        // Re-run initialize is idempotent (creates 0)
        ResponseEntity<JsonNode> reinit = rest.exchange(
                "/leave-balances/initialize?year=2026", HttpMethod.POST,
                authed(adminToken, null), JsonNode.class);
        assertThat(reinit.getBody().path("data").path("created").asLong()).isZero();
    }

    @Test
    void employee_cannotReadOthers() {
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/users/999999/leave-balances?year=2026", HttpMethod.GET,
                authed(employeeToken, null), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void employee_cannotInitialize() {
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/leave-balances/initialize?year=2026", HttpMethod.POST,
                authed(employeeToken, null), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adjustBelowZero_returns400() {
        rest.exchange("/leave-balances/initialize?year=2026", HttpMethod.POST,
                authed(adminToken, null), JsonNode.class);
        ResponseEntity<JsonNode> mine = rest.exchange(
                "/users/" + employeeId + "/leave-balances?year=2026", HttpMethod.GET,
                authed(adminToken, null), JsonNode.class);
        long firstId = mine.getBody().path("data").get(0).path("id").asLong();

        ResponseEntity<JsonNode> resp = rest.exchange(
                "/leave-balances/" + firstId + "/adjust", HttpMethod.PATCH,
                authed(adminToken, new LeaveBalanceAdjustRequest(new BigDecimal("-999.0"), "too much")),
                JsonNode.class);
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
