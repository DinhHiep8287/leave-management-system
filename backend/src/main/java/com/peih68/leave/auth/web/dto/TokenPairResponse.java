package com.peih68.leave.auth.web.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds) {}
