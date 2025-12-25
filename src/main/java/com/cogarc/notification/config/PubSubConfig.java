package com.cogarc.notification.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
public class PubSubConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.emulator-host:}")
    private String emulatorHost;

    // Set emulator host as early as possible
    static {
        // Check if environment variable is set, if not, use default
        String envHost = System.getenv("PUBSUB_EMULATOR_HOST");
        if (envHost == null || envHost.isEmpty()) {
            // Default to localhost:8085 if not set
            String defaultHost = "localhost:8085";
            System.setProperty("PUBSUB_EMULATOR_HOST", defaultHost);
            // Try to set as environment variable (may not work, but try)
            try {
                // This is a workaround - the env var should be set before JVM starts
                // But we set the system property which some libraries also check
            } catch (Exception e) {
                // Ignore - system property should be sufficient
            }
        } else {
            // Ensure system property matches environment variable
            System.setProperty("PUBSUB_EMULATOR_HOST", envHost);
        }
    }

    @PostConstruct
    public void setEmulatorHost() {
        // Ensure PUBSUB_EMULATOR_HOST system property is set for Camel component
        // The Google Cloud libraries check both environment variables and system properties
        String hostToUse = emulatorHost;
        if (hostToUse == null || hostToUse.isEmpty()) {
            hostToUse = System.getenv("PUBSUB_EMULATOR_HOST");
            if (hostToUse == null || hostToUse.isEmpty()) {
                hostToUse = "localhost:8085"; // Default
            }
        }
        System.setProperty("PUBSUB_EMULATOR_HOST", hostToUse);
        System.out.println("=== PubSubConfig: Set PUBSUB_EMULATOR_HOST to: " + hostToUse + " ===");
    }

    @Bean
    public CredentialsProvider credentialsProvider() {
        // Use NoCredentialsProvider when using emulator
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            return NoCredentialsProvider.create();
        }
        // For real GCP, use default credentials
        return () -> null;
    }

    // Topic names as beans for easy injection
    @Bean("orderStatusTopic")
    public String orderStatusTopic(@Value("${app.pubsub.topics.order-status}") String topic) {
        return topic;
    }

    @Bean("uccTopic")
    public String uccTopic(@Value("${app.pubsub.topics.ucc}") String topic) {
        return topic;
    }

    @Bean("tourAppointmentTopic")
    public String tourAppointmentTopic(@Value("${app.pubsub.topics.tour-appointment}") String topic) {
        return topic;
    }

    @Bean("truckloadTopic")
    public String truckloadTopic(@Value("${app.pubsub.topics.truckload}") String topic) {
        return topic;
    }
}

