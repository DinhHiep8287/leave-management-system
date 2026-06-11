package com.peih68.leave.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async for after-commit side effects (e.g. email notifications). */
@Configuration
@EnableAsync
public class AsyncConfig {}
