package com.edutech.center.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring @Async support for center-svc.
 * AI insight calls are non-blocking optional features.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
