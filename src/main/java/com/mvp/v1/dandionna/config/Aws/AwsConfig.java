package com.mvp.v1.dandionna.config.Aws;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;



import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author rua
 */
@Configuration
public class AwsConfig {

	private StaticCredentialsProvider credentialsProvider(String accessKey, String secretKey) {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
	}

	private S3Configuration s3Configuration(boolean pathStyle) {
		return S3Configuration.builder()
			.pathStyleAccessEnabled(pathStyle) // MinIO 호환을 위해 Path-Style 강제
			.checksumValidationEnabled(false) // Response validation disable
			.build();
	}

	// ✅ Presigner - 프론트엔드용 (Public Endpoint 사용)
	@Bean
	S3Presigner s3Presigner(
		@Value("${app.s3.region}") String region,
		@Value("${app.s3.public-endpoint:}") String publicEndpoint, // Public Endpoint 우선
		@Value("${app.s3.endpoint:}") String endpoint, // Fallback
		@Value("${app.s3.access-key}") String accessKey,
		@Value("${app.s3.secret-key}") String secretKey,
		@Value("${app.s3.path-style:true}") boolean pathStyle
	) {
		S3Presigner.Builder builder = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider(accessKey, secretKey))
			.serviceConfiguration(s3Configuration(pathStyle));

		// 프론트엔드가 접근할 수 있는 Public Endpoint 설정
		if (StringUtils.hasText(publicEndpoint)) {
			builder.endpointOverride(URI.create(publicEndpoint));
		} else if (StringUtils.hasText(endpoint)) {
			builder.endpointOverride(URI.create(endpoint));
		}
		return builder.build();
	}

	// ✅ Client - 백엔드용 (Internal Endpoint 사용)
	@Bean
	S3Client s3Client(
		@Value("${app.s3.region}") String region,
		@Value("${app.s3.endpoint:}") String endpoint,
		@Value("${app.s3.access-key}") String accessKey,
		@Value("${app.s3.secret-key}") String secretKey,
		@Value("${app.s3.path-style:true}") boolean pathStyle,
		SdkHttpClient minioCompatibleHttpClient
	) {
		S3ClientBuilder builder = S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider(accessKey, secretKey))
			.serviceConfiguration(s3Configuration(pathStyle))
			.httpClient(minioCompatibleHttpClient);

		// 백엔드가 접근할 수 있는 Internal Endpoint 설정 (localhost 권장)
		if (StringUtils.hasText(endpoint)) {
			builder.endpointOverride(URI.create(endpoint));
		}
		return builder.build();
	}
}
