package com.mvp.v1.dandionna.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.service.UploadService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DANDI_RUN_INFRA_TESTS", matches = "true")
public class PresignedUrlVerificationTest {

    @Autowired
    private UploadService uploadService;

    @Value("${app.s3.public-endpoint}")
    private String publicEndpoint;

    @Test
    @DisplayName("Presigned URL should use the configured public endpoint")
    void verifyPresignedUrlEndpoint() {
        // Given
        String testKey = "test-image.jpg";

        // When
        PresignedUrlResponse response = uploadService.presignDownload(testKey);
        String generatedUrl = response.url();

        // Then
        System.out.println("Generated URL: " + generatedUrl);
        System.out.println("Expected Endpoint: " + publicEndpoint);

        assertThat(generatedUrl).startsWith(publicEndpoint);
    }
}
