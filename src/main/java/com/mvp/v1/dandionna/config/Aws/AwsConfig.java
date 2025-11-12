package com.mvp.v1.dandionna.config.Aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author rua
 */
@Configuration
public class AwsConfig {

	// ✅ Presigner (기존)
	@Bean
	S3Presigner s3Presigner(
		@Value("${app.s3.region}") String region,
		@Value("${app.s3.user}") String user
	) {
		return S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(ProfileCredentialsProvider.create(user))
			.build();
	}

	// ✅ Client (신규 추가) - ETag 검증에 필수!
	@Bean
	S3Client s3Client(
		@Value("${app.s3.region}") String region,
		@Value("${app.s3.user}") String user
	) {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(ProfileCredentialsProvider.create(user))
			.build();
	}
}