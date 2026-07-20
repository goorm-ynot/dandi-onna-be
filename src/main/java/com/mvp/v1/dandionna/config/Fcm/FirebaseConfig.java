package com.mvp.v1.dandionna.config.Fcm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * @author rua
 */
@Configuration
public class FirebaseConfig {
	@Value("${firebase.key-path}")
	private String firebaseKeyPath;

	@Bean
	public FirebaseApp firebaseApp() throws IOException {
		// 파일 존재 여부 검증
		if (!new File(firebaseKeyPath).exists()) {
			throw new FileNotFoundException("Firebase key file not found: " + firebaseKeyPath);
		}

		List<FirebaseApp> apps = FirebaseApp.getApps();
		if (!apps.isEmpty()) {
			return FirebaseApp.getInstance();
		}

		try (FileInputStream serviceAccount = new FileInputStream(firebaseKeyPath)) {
			FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.build();
			return FirebaseApp.initializeApp(options);
		}
	}

	@Bean
	public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
		return FirebaseMessaging.getInstance(app);
	}
}
