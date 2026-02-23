package com.mvp.v1.dandionna.config.Security;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.AeadAlgorithm;

/**
 * 암호 키(SecretKey)와 AEAD 알고리즘을 생성하는 전용 설정 클래스.
 * 민감한 설정을 다루는 빈을 분리해 두면 {@link SecurityConfig} 가 가벼워지고,
 * 어떤 부분이 보안 설정에 의존하는지 명확해진다. 전체 구조는 doc/security-flow.md 참고.
 *
 * @author rua
 */
@Configuration
@EnableConfigurationProperties(JwtProps.class)
public class SecurityBeans {

	/**
	 * JWE 암복호화에 사용하는 현재 활성 대칭 AES 키.
	 * 토큰 발급에 사용된다.
	 */
	@Bean
	public SecretKey jweSecretKey(JwtProps props) {
		byte[] key = Decoders.BASE64.decode(props.jwe().secretBase64());
		return new SecretKeySpec(key, "AES");
	}

	/**
	 * 키 로테이션을 위한 키 맵.
	 * kid -> SecretKey 매핑. 현재 키 + 이전 키(있을 경우)를 포함한다.
	 * 토큰 검증 시 kid 헤더를 보고 적절한 키를 선택한다.
	 */
	@Bean
	public Map<String, SecretKey> jweKeyMap(JwtProps props) {
		Map<String, SecretKey> keyMap = new LinkedHashMap<>();

		// 현재 활성 키
		byte[] currentKeyBytes = Decoders.BASE64.decode(props.jwe().secretBase64());
		keyMap.put(props.jwe().kid(), new SecretKeySpec(currentKeyBytes, "AES"));

		// 이전 키 (로테이션 시 과도기 동안 이전 토큰도 검증 가능)
		if (props.jwe().previousSecretBase64() != null && !props.jwe().previousSecretBase64().isBlank()) {
			byte[] prevKeyBytes = Decoders.BASE64.decode(props.jwe().previousSecretBase64());
			keyMap.put("prev-" + props.jwe().kid(), new SecretKeySpec(prevKeyBytes, "AES"));
		}

		return keyMap;
	}

	/**
	 * 토큰 서비스가 설정 파싱에 의존하지 않도록 AEAD 알고리즘을 빈으로 제공한다.
	 */
	@Bean
	public AeadAlgorithm jweEncAlg(JwtProps props) {
		return switch (props.jwe().enc()) {
			case "A128GCM" -> Jwts.ENC.A128GCM;
			case "A192GCM" -> Jwts.ENC.A192GCM;
			default -> Jwts.ENC.A256GCM;
		};
	}

	/**
	 * 회원가입/로그인 시 사용할 패스워드 인코더.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
