package com.edutech.notification;

import org.junit.jupiter.api.Test;

/**
 * Smoke test — verifies the module compiles and the application context
 * can be loaded in test scope (no actual Spring context started here;
 * full integration tests require Testcontainers).
 */
class NotificationServiceApplicationTest {

    @Test
    void mainClassExists() {
        // Verifies the entry point compiles cleanly.
        // Full context test is in NotificationServiceIntegrationTest.
        Class<?> clazz = NotificationServiceApplication.class;
        assert clazz != null;
    }
}
