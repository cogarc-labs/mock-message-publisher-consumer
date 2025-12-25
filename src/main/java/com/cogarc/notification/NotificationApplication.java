package com.cogarc.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationApplication {

    // Set PUBSUB_EMULATOR_HOST as early as possible, before Spring Boot initializes
    static {
        // Check if environment variable is set, if not, use default
        String envHost = System.getenv("PUBSUB_EMULATOR_HOST");
        if (envHost == null || envHost.isEmpty()) {
            // Default to localhost:8085 if not set
            String defaultHost = "localhost:8085";
            System.setProperty("PUBSUB_EMULATOR_HOST", defaultHost);
            // Try to set as environment variable via reflection (may not work on all JVMs)
            try {
                java.lang.reflect.Field field = System.class.getDeclaredField("env");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> env = (java.util.Map<String, String>) field.get(null);
                env.put("PUBSUB_EMULATOR_HOST", defaultHost);
                System.out.println("=== NotificationApplication: Set PUBSUB_EMULATOR_HOST to: " + defaultHost + " ===");
            } catch (Exception e) {
                // If reflection fails, at least set the system property
                System.setProperty("PUBSUB_EMULATOR_HOST", defaultHost);
                System.out.println("=== NotificationApplication: Set PUBSUB_EMULATOR_HOST system property to: " + defaultHost + " ===");
                System.out.println("=== WARNING: Could not set environment variable. Please set PUBSUB_EMULATOR_HOST before starting JVM ===");
            }
        } else {
            // Ensure system property matches environment variable
            System.setProperty("PUBSUB_EMULATOR_HOST", envHost);
            System.out.println("=== NotificationApplication: Using PUBSUB_EMULATOR_HOST from environment: " + envHost + " ===");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}

