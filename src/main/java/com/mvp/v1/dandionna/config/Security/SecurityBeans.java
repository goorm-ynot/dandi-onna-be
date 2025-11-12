package com.mvp.v1.dandionna.config.Security;

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
	 * JWE 암복호화에 공용으로 사용하는 대칭 AES 키를 생성한다.
	 * 실제 키 문자열은 설정/환경 변수에 존재하며 이곳에서만 디코딩된다.
	 */
	@Bean
	public SecretKey jweSecretKey(JwtProps props) {
		byte[] key = Decoders.BASE64.decode(props.jwe().secretBase64());
		return new SecretKeySpec(key, "AES"); // A256GCM에 맞는 256-bit 키
	}

	/**
	 * 토큰 서비스가 설정 파싱에 의존하지 않도록 AEAD 알고리즘을 빈으로 제공한다.
	 */
	@Bean
	public AeadAlgorithm jweEncAlg(JwtProps props) {
		// enc는 A128GCM/A192GCM/A256GCM 중 선택. 기본 A256GCM 권장
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
