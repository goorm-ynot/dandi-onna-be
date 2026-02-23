package com.mvp.v1.dandionna.config.Security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code application.yaml} 에서 주입되는 시큐리티 설정을 강한 타입으로 보관한다.
 * 애플리케이션이 뜰 때 한 번만 값을 읽어 두면, 나머지 보안 계층은 환경 변수 파싱 없이
 * 검증된 불변 값에만 의존할 수 있다. 전체 흐름은 doc/security-flow.md 를 참고한다.
 *
 * @author rua
 */
@ConfigurationProperties(prefix = "security")
public record JwtProps(Jwt jwt, Jwe jwe) {
	/**
	 * Access/Refresh 토큰 공통으로 쓰이는 발급 정보.
	 */
	public record Jwt(String issuer, int accessMinutes, int refreshDays) {}

	/**
	 * JWE 암호화에 필요한 알고리즘/대칭키 설정.
	 * - secretBase64: 현재 활성 키 (토큰 발급에 사용)
	 * - previousSecretBase64: 이전 키 (토큰 검증에만 사용, 키 로테이션 시)
	 * - kid: 현재 키 식별자 (토큰 헤더에 포함)
	 */
	public record Jwe(String enc, String secretBase64, String previousSecretBase64, String kid) {
		public Jwe {
			if (kid == null || kid.isBlank()) kid = "default";
		}
	}
}
