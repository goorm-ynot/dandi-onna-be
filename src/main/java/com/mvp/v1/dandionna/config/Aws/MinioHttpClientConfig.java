package com.mvp.v1.dandionna.config.Aws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

@Slf4j
@Configuration
public class MinioHttpClientConfig {

	@Bean
	public SdkHttpClient minioCompatibleHttpClient() {
		return ApacheHttpClient.builder()
			.build();
	}
}
