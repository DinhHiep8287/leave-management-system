package com.peih68.leave.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test_secret_for_unit_tests_only_at_least_32_chars_long";
    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        principal = new UserPrincipal(42L, "x@y.z", "hash", Role.EMPLOYEE, true);
    }

    @Test
    void issueAndParseAccessToken() {
        JwtService.IssuedToken issued = jwtService.issueAccess(principal);
        Claims claims = jwtService.parse(issued.token());

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email")).isEqualTo("x@y.z");
        assertThat(claims.get("role")).isEqualTo("EMPLOYEE");
        assertThat(jwtService.typeOf(claims)).isEqualTo(JwtService.TokenType.ACCESS);
        assertThat(claims.getId()).isEqualTo(issued.jti());
    }

    @Test
    void issueRefreshDifferentiatedByType() {
        Claims claims = jwtService.parse(jwtService.issueRefresh(principal).token());
        assertThat(jwtService.typeOf(claims)).isEqualTo(JwtService.TokenType.REFRESH);
    }

    @Test
    void rejectsInvalidSignature() {
        String token = jwtService.issueAccess(principal).token();
        JwtService other = new JwtService("a_different_secret_value_at_least_32_bytes_long_!!", 900_000L, 1L);
        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1L, 1L);
        String token = shortLived.issueAccess(principal).token();
        Thread.sleep(50);
        assertThatThrownBy(() -> shortLived.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void sha256Deterministic() {
        assertThat(JwtService.sha256("abc")).isEqualTo(JwtService.sha256("abc"));
        assertThat(JwtService.sha256("abc")).isNotEqualTo(JwtService.sha256("abd"));
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("short", 1L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
