package com.peih68.leave.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import com.peih68.leave.user.web.dto.ChangePasswordRequest;
import com.peih68.leave.user.web.dto.UpdateMeRequest;
import com.peih68.leave.user.web.dto.UserCreateRequest;
import com.peih68.leave.user.web.dto.UserUpdateRequest;
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
class UserE2ETest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private String adminToken;
    private Long adminId;
    private Long engId;

    @BeforeEach
    void setup() {
        wipeUsersFkSafe(jdbc);
        engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        UserEntity admin = userRepository.save(UserEntity.builder()
                .employeeCode("U0001").email("u.admin@demo.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .fullName("U Admin").role(Role.ADMIN).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        adminId = admin.getId();
        adminToken = login("u.admin@demo.local", "Admin@12345");
    }

    @Test
    void adminCreatesUser_userCanLogin_thenDeactivateBlocks() {
        // Admin creates an employee
        UserCreateRequest create = new UserCreateRequest(
                "U0010", "alice@demo.local", "Alice", "Welcome@1",
                Role.EMPLOYEE, engId, adminId, LocalDate.of(2025, 1, 1));
        ResponseEntity<JsonNode> created = rest.exchange(
                "/users", HttpMethod.POST, authed(adminToken, create), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long newId = created.getBody().path("data").path("id").asLong();
        assertThat(created.getBody().path("data").has("passwordHash")).isFalse();

        // User logs in
        String userToken = login("alice@demo.local", "Welcome@1");

        // User can read /users/me
        ResponseEntity<JsonNode> me = rest.exchange(
                "/users/me", HttpMethod.GET, authed(userToken, null), JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().path("data").path("email").asText()).isEqualTo("alice@demo.local");
        // /me resolves department + manager names (not just ids)
        assertThat(me.getBody().path("data").path("departmentName").asText()).isEqualTo("Engineering");
        assertThat(me.getBody().path("data").path("managerName").asText()).isEqualTo("U Admin");

        // User CANNOT read another user
        ResponseEntity<JsonNode> other = rest.exchange(
                "/users/" + adminId, HttpMethod.GET, authed(userToken, null), JsonNode.class);
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // User CANNOT list
        ResponseEntity<JsonNode> listAttempt = rest.exchange(
                "/users", HttpMethod.GET, authed(userToken, null), JsonNode.class);
        assertThat(listAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // User self-update changes only fullName
        ResponseEntity<JsonNode> patched = rest.exchange(
                "/users/me", HttpMethod.PATCH,
                authed(userToken, new UpdateMeRequest("Alice Updated")), JsonNode.class);
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patched.getBody().path("data").path("fullName").asText()).isEqualTo("Alice Updated");

        // Admin deactivates
        ResponseEntity<JsonNode> deact = rest.exchange(
                "/users/" + newId + "/deactivate", HttpMethod.POST,
                authed(adminToken, null), JsonNode.class);
        assertThat(deact.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deact.getBody().path("data").path("active").asBoolean()).isFalse();

        // User can no longer log in
        ResponseEntity<JsonNode> loginAfter = rest.postForEntity(
                "/auth/login", new LoginRequest("alice@demo.local", "Welcome@1"), JsonNode.class);
        assertThat(loginAfter.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateEmail_returns409() {
        UserCreateRequest first = new UserCreateRequest(
                "U0020", "bob@demo.local", "Bob", "Welcome@1",
                Role.EMPLOYEE, engId, adminId, LocalDate.of(2025, 1, 1));
        rest.exchange("/users", HttpMethod.POST, authed(adminToken, first), JsonNode.class);
        UserCreateRequest dup = new UserCreateRequest(
                "U0021", "BOB@demo.local", "Bob 2", "Welcome@1",
                Role.EMPLOYEE, engId, adminId, LocalDate.of(2025, 1, 1));
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/users", HttpMethod.POST, authed(adminToken, dup), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void admin_updateUser_canChangeRole() {
        UserCreateRequest create = new UserCreateRequest(
                "U0030", "carol@demo.local", "Carol", "Welcome@1",
                Role.EMPLOYEE, engId, adminId, LocalDate.of(2025, 1, 1));
        ResponseEntity<JsonNode> created = rest.exchange(
                "/users", HttpMethod.POST, authed(adminToken, create), JsonNode.class);
        long id = created.getBody().path("data").path("id").asLong();

        UserUpdateRequest upd = new UserUpdateRequest(
                "U0030", "carol@demo.local", "Carol Promoted",
                Role.MANAGER, engId, adminId, LocalDate.of(2025, 1, 1));
        ResponseEntity<JsonNode> updated = rest.exchange(
                "/users/" + id, HttpMethod.PUT, authed(adminToken, upd), JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("data").path("role").asText()).isEqualTo("MANAGER");
    }

    @Test
    void changePassword_revokesOldRefreshToken() {
        UserCreateRequest create = new UserCreateRequest(
                "U0040", "dan@demo.local", "Dan", "Welcome@1",
                Role.EMPLOYEE, engId, adminId, LocalDate.of(2025, 1, 1));
        rest.exchange("/users", HttpMethod.POST, authed(adminToken, create), JsonNode.class);

        ResponseEntity<JsonNode> loginResp = rest.postForEntity(
                "/auth/login", new LoginRequest("dan@demo.local", "Welcome@1"), JsonNode.class);
        String access = loginResp.getBody().path("data").path("accessToken").asText();
        String refresh = loginResp.getBody().path("data").path("refreshToken").asText();

        ResponseEntity<JsonNode> changed = rest.exchange(
                "/users/me/password", HttpMethod.POST,
                authed(access, new ChangePasswordRequest("Welcome@1", "NewPass@1")), JsonNode.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Old refresh token is now revoked
        ResponseEntity<JsonNode> reused = rest.postForEntity(
                "/auth/refresh", new java.util.HashMap<String, String>() {{ put("refreshToken", refresh); }},
                JsonNode.class);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Login with new password works
        ResponseEntity<JsonNode> loginNew = rest.postForEntity(
                "/auth/login", new LoginRequest("dan@demo.local", "NewPass@1"), JsonNode.class);
        assertThat(loginNew.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String login(String email, String password) {
        ResponseEntity<JsonNode> resp = rest.postForEntity(
                "/auth/login", new LoginRequest(email, password), JsonNode.class);
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
