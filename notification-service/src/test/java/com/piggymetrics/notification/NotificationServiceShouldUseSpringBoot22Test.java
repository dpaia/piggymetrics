package com.piggymetrics.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationServiceShouldUseSpringBoot22Test {
    @Test
    void shouldBeUsingSpringBoot22() {
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version should not be null");
        assertTrue(version.startsWith("2.2."),
                "Expected Spring Boot 2.2.x but got: " + version);
    }
}
