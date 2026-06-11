package com.peih68.leave.leaverequest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceAdjustRequest;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.web.dto.ApprovalDecisionRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
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
class LeaveRequestE2ETest {

    private static final LocalDate MON = LocalDate.of(2026, 7, 6);
    private static final LocalDate FRI = LocalDate.of(2026, 7, 10); // 5 working days

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private String adminToken;
    private String mgr1Token;
    private String mgr2Token;
    private String empToken;
    private Long employeeId;
    private Long annualTypeId;

    @BeforeEach
    void setup() {
        wipe();
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        annualTypeId = jdbc.queryForObject("SELECT id FROM leave_types WHERE code = 'ANNUAL'", Long.class);

        UserEntity admin = userRepository.save(user("LR0001", "lr.admin@demo.local", "Admin", Role.ADMIN, engId, null));
        Long mgr1Id = userRepository.save(user("LR0002", "lr.mgr1@demo.local", "Mgr1", Role.MANAGER, engId, admin.getId())).getId();
        userRepository.save(user("LR0003", "lr.mgr2@demo.local", "Mgr2", Role.MANAGER, engId, admin.getId()));
        employeeId = userRepository.save(user("LR0004", "lr.emp@demo.local", "Emp", Role.EMPLOYEE, engId, mgr1Id)).getId();

        adminToken = login("lr.admin@demo.local");
        mgr1Token = login("lr.mgr1@demo.local");
        mgr2Token = login("lr.mgr2@demo.local");
        empToken = login("lr.emp@demo.local");

        // Initialize 2026 balances for all active users × balance-requiring leave types.
        rest.exchange("/leave-balances/initialize?year=2026", HttpMethod.POST,
                authed(adminToken, null), JsonNode.class);
    }

    // E2E tests commit (no @Transactional rollback). Clean up so the committed
    // leave_requests/approval_actions don't break other E2E suites whose setup
    // does DELETE FROM users (FK on leave_requests.user_id).
    @AfterEach
    void tearDown() {
        wipe();
    }

    private void wipe() {
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM approval_actions");
        jdbc.update("DELETE FROM leave_requests");
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM leave_balances");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void goldenPath_submitApproveConsumesBalanceAndRecordsHistory() {
        long id = submitAnnual(empToken, MON, FRI);

        // Manager sees it in the pending inbox
        ResponseEntity<JsonNode> inbox = rest.exchange("/leave-requests?status=PENDING", HttpMethod.GET,
                authed(mgr1Token, null), JsonNode.class);
        assertThat(inbox.getBody().path("meta").path("totalElements").asInt()).isGreaterThanOrEqualTo(1);

        double remainingBefore = annualBalance().path("remainingDays").asDouble();

        ResponseEntity<JsonNode> approve = rest.exchange("/leave-requests/" + id + "/approve", HttpMethod.POST,
                authed(mgr1Token, new ApprovalDecisionRequest("ok")), JsonNode.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approve.getBody().path("data").path("status").asText()).isEqualTo("APPROVED");

        JsonNode annual = annualBalance();
        assertThat(annual.path("usedDays").asDouble()).isEqualTo(5.0);
        assertThat(annual.path("remainingDays").asDouble()).isEqualTo(remainingBefore - 5.0);

        ResponseEntity<JsonNode> history = rest.exchange("/leave-requests/" + id + "/history", HttpMethod.GET,
                authed(empToken, null), JsonNode.class);
        assertThat(history.getBody().path("data").size()).isEqualTo(2); // CREATED + APPROVED
        assertThat(history.getBody().path("data").get(1).path("action").asText()).isEqualTo("APPROVED");
    }

    @Test
    void reject_doesNotConsumeBalance() {
        long id = submitAnnual(empToken, MON, FRI);
        ResponseEntity<JsonNode> reject = rest.exchange("/leave-requests/" + id + "/reject", HttpMethod.POST,
                authed(mgr1Token, new ApprovalDecisionRequest("not this week")), JsonNode.class);
        assertThat(reject.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reject.getBody().path("data").path("status").asText()).isEqualTo("REJECTED");
        assertThat(annualBalance().path("usedDays").asDouble()).isEqualTo(0.0);
    }

    @Test
    void cancelApproved_restoresBalance() {
        long id = submitAnnual(empToken, MON, FRI);
        rest.exchange("/leave-requests/" + id + "/approve", HttpMethod.POST,
                authed(mgr1Token, new ApprovalDecisionRequest("ok")), JsonNode.class);
        assertThat(annualBalance().path("usedDays").asDouble()).isEqualTo(5.0);

        ResponseEntity<JsonNode> cancel = rest.exchange("/leave-requests/" + id + "/cancel", HttpMethod.POST,
                authed(mgr1Token, new ApprovalDecisionRequest("plans changed")), JsonNode.class);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancel.getBody().path("data").path("status").asText()).isEqualTo("CANCELLED");
        assertThat(annualBalance().path("usedDays").asDouble()).isEqualTo(0.0);
    }

    @Test
    void crossTeamManagerCannotApprove() {
        long id = submitAnnual(empToken, MON, FRI);
        ResponseEntity<JsonNode> resp = rest.exchange("/leave-requests/" + id + "/approve", HttpMethod.POST,
                authed(mgr2Token, new ApprovalDecisionRequest("ok")), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void employeeCannotApproveOwnRequest() {
        long id = submitAnnual(empToken, MON, FRI);
        ResponseEntity<JsonNode> resp = rest.exchange("/leave-requests/" + id + "/approve", HttpMethod.POST,
                authed(empToken, new ApprovalDecisionRequest("self")), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void approveWithDrainedBalanceReturnsConflict() {
        long id = submitAnnual(empToken, MON, FRI); // soft check OK (remaining 12 >= 5)

        // HR/ADMIN drains the balance after submission so the hard check at approval fails.
        long annualId = annualBalance().path("id").asLong();
        rest.exchange("/leave-balances/" + annualId + "/adjust", HttpMethod.PATCH,
                authed(adminToken, new LeaveBalanceAdjustRequest(new BigDecimal("-8.0"), "reclaim")), JsonNode.class);

        ResponseEntity<JsonNode> approve = rest.exchange("/leave-requests/" + id + "/approve", HttpMethod.POST,
                authed(mgr1Token, new ApprovalDecisionRequest("ok")), JsonNode.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(approve.getBody().path("error").path("code").asText()).isEqualTo("INSUFFICIENT_BALANCE");
    }

    // --- helpers ---

    private long submitAnnual(String token, LocalDate start, LocalDate end) {
        ResponseEntity<JsonNode> r = rest.exchange("/leave-requests", HttpMethod.POST,
                authed(token, new LeaveRequestCreateRequest(
                        annualTypeId, start, end, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, "trip")),
                JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().path("data").path("id").asLong();
    }

    private JsonNode annualBalance() {
        ResponseEntity<JsonNode> mine = rest.exchange(
                "/users/" + employeeId + "/leave-balances?year=2026", HttpMethod.GET,
                authed(adminToken, null), JsonNode.class);
        for (JsonNode b : mine.getBody().path("data")) {
            if ("ANNUAL".equals(b.path("leaveTypeCode").asText())) {
                return b;
            }
        }
        throw new AssertionError("ANNUAL balance not found");
    }

    private UserEntity user(String code, String email, String name, Role role, Long deptId, Long managerId) {
        return UserEntity.builder()
                .employeeCode(code).email(email).passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName(name).role(role).departmentId(deptId).managerId(managerId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build();
    }

    private String login(String email) {
        ResponseEntity<JsonNode> resp = rest.postForEntity(
                "/auth/login", new LoginRequest(email, "Admin@12345"), JsonNode.class);
        return resp.getBody().path("data").path("accessToken").asText();
    }

    private static <T> HttpEntity<T> authed(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.add("Content-Type", "application/json");
        return new HttpEntity<>(body, headers);
    }
}
