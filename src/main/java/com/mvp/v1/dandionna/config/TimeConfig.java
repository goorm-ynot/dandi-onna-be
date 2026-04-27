package com.mvp.v1.dandionna.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mvp.v1.dandionna.noshow_post.NoShowConstants;

@Configuration
public class TimeConfig {

	@Bean
	public Clock applicationClock() {
		return Clock.system(NoShowConstants.DB_ZONE);
	}
}
