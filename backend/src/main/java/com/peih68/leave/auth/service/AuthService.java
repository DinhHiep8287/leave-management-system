package com.peih68.leave.auth.service;

import com.peih68.leave.auth.domain.RefreshTokenEntity;
import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.auth.repository.RefreshTokenRepository;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.auth.web.dto.TokenPairResponse;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Account disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(UserPrincipal.from(user));
    }

    @Transactional
    public TokenPairResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (JwtException ex) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid refresh token");
        }
        if (jwtService.typeOf(claims) != JwtService.TokenType.REFRESH) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Not a refresh token");
        }
        String hash = JwtService.sha256(refreshToken);
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token revoked"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!stored.isActive(now)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token revoked or expired");
        }
        Long userId = Long.valueOf(claims.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "User not found"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Account disabled");
        }
        // Rotate: revoke old, issue new pair
        refreshTokenRepository.revokeByTokenHash(hash, now);
        return issueTokens(UserPrincipal.from(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = JwtService.sha256(refreshToken);
        refreshTokenRepository.revokeByTokenHash(hash, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private TokenPairResponse issueTokens(UserPrincipal principal) {
        JwtService.IssuedToken access = jwtService.issueAccess(principal);
        JwtService.IssuedToken refresh = jwtService.issueRefresh(principal);
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .userId(principal.getId())
                .tokenHash(JwtService.sha256(refresh.token()))
                .expiresAt(toOffset(refresh.expiresAt()))
                .build();
        refreshTokenRepository.save(entity);
        return new TokenPairResponse(
                access.token(),
                refresh.token(),
                jwtService.getAccessExpirationMs() / 1000);
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
