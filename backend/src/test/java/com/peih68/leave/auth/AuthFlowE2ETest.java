package com.peih68.leave.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.auth.web.dto.RefreshRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import static com.peih68.leave.config.E2ECleanup.wipeUsersFkSafe;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowE2ETest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private static final String EMAIL = "test.admin@demo.local";
    private static final String PASSWORD = "Admin@12345";

    @BeforeEach
    void seedUser() {
        wipeUsersFkSafe(jdbc);
        Long deptId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        UserEntity admin = UserEntity.builder()
                .employeeCode("T0001")
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Test Admin")
                .role(Role.ADMIN)
                .departmentId(deptId)
                .joinDate(LocalDate.of(2024, 1, 1))
                .isActive(true)
                .build();
        userRepository.save(admin);
    }

    @Test
    void loginThenCallProtectedEndpoint() {
        ResponseEntity<JsonNode> login = rest.postForEntity(
                "/auth/login", new LoginRequest(EMAIL, PASSWORD), JsonNode.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String access = login.getBody().path("data").path("accessToken").asText();
        String refresh = login.getBody().path("data").path("refreshToken").asText();
        assertThat(access).isNotBlank();
        assertThat(refresh).isNotBlank();

        // Call /auth/me with bearer
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(access);
        ResponseEntity<JsonNode> me = rest.exchange(
                "/auth/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().path("data").path("email").asText()).isEqualTo(EMAIL);
        assertThat(me.getBody().path("data").path("role").asText()).isEqualTo("ADMIN");

        // Refresh -> new pair, old refresh becomes invalid (rotation)
        ResponseEntity<JsonNode> refreshed = rest.postForEntity(
                "/auth/refresh", new RefreshRequest(refresh), JsonNode.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccess = refreshed.getBody().path("data").path("accessToken").asText();
        assertThat(newAccess).isNotBlank();

        ResponseEntity<JsonNode> reuse = rest.postForEntity(
                "/auth/refresh", new RefreshRequest(refresh), JsonNode.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidCredentialsReturns401() {
        ResponseEntity<JsonNode> resp = rest.postForEntity(
                "/auth/login", new LoginRequest(EMAIL, "wrong-password"), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() {
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/auth/me", HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("not-a-real-jwt");
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/auth/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
