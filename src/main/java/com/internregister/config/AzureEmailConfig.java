package com.internregister.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureEmailConfig {

    @Value("${azure.communication.connection-string}")
    private String connectionString;

    @Bean
    public EmailClient emailClient() {
        if (connectionString == null || connectionString.trim().isEmpty() || connectionString.contains("your-")) {
            System.err.println("⚠ Azure Communication Services connection string is not configured.");
            return null;
        }

        try {
            return new EmailClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        } catch (Exception e) {
            System.err.println("❌ Failed to create Azure EmailClient: " + e.getMessage());
            return null;
        }
    }
}
