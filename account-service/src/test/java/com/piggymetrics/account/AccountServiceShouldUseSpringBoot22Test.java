package com.piggymetrics.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountServiceShouldUseSpringBoot22Test {
    @Test
    void shouldBeUsingSpringBoot22() {
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version should not be null");
        assertTrue(version.startsWith("2.2."),
                "Expected Spring Boot 2.2.x but got: " + version);
    }
}
