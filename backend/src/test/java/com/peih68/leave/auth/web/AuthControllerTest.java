package com.peih68.leave.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peih68.leave.auth.service.AuthService;
import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.auth.web.dto.RefreshRequest;
import com.peih68.leave.auth.web.dto.TokenPairResponse;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean AuthService authService;
    @MockBean UserRepository userRepository;
    @MockBean JwtService jwtService;

    @Test
    void login_returnsTokenPair() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willReturn(new TokenPairResponse("ACCESS", "REFRESH", 900L));

        mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@b.com", "12345678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("ACCESS"))
                .andExpect(jsonPath("$.data.refreshToken").value("REFRESH"))
                .andExpect(jsonPath("$.data.accessExpiresInSeconds").value(900));
    }

    @Test
    void login_rejectsInvalidEmail() throws Exception {
        mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("not-email", "12345678"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_rejectsShortPassword() throws Exception {
        mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@b.com", "1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new ApiException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));

        mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@b.com", "12345678"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void refresh_returnsNewPair() throws Exception {
        given(authService.refresh(any()))
                .willReturn(new TokenPairResponse("NEW_A", "NEW_R", 900L));

        mvc.perform(post("/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshRequest("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("NEW_A"));
    }

    @Test
    void logout_returns200AndInvokesService() throws Exception {
        mvc.perform(post("/auth/logout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshRequest("some-refresh"))))
                .andExpect(status().isOk());
        verify(authService).logout("some-refresh");
    }
}
