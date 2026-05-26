package com.peih68.leave.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Enables @PreAuthorize in @WebMvcTest slices without pulling in the full SecurityConfig. */
@TestConfiguration
@EnableMethodSecurity
public class MethodSecurityTestConfig {
}
