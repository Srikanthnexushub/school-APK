package com.edutech.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
    List<String> trustedProxyCidrs
) {}
