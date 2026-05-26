package com.peih68.leave.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI leaveOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Leave Management System API")
                .description("REST API for the leave management system")
                .version("v0.0.1")
                .license(new License().name("MIT")));
    }
}
