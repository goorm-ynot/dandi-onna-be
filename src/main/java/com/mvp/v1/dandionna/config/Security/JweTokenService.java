package com.mvp.v1.dandionna.config.Security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.AeadAlgorithm;

/**
 * JSON Web Encryption(JWE) 토큰 발급·검증을 담당하는 상태 없는 서비스.
 * 암호화 설정은 생성자 주입으로 고정되므로, 이 클래스는 토큰 구조와 만료 시간 계산에만 집중한다.
 * 요청 처리 흐름은 doc/security-flow.md 를 참고한다.
 *
 * @author rua
 */
@Service
public class JweTokenService {
	private final SecretKey key;
	private final AeadAlgorithm enc;
	private final String issuer;
	private final int accessMinutes;
	private final int refreshDays;

	public JweTokenService(SecretKey key, AeadAlgorithm enc,
		JwtProps props) {
		this.key = key;
		this.enc = enc;
		this.issuer = props.jwt().issuer();
		this.accessMinutes = props.jwt().accessMinutes();
		this.refreshDays = props.jwt().refreshDays();
	}

	/**
	 * 컨트롤러 인가에 사용하는 단기 Access 토큰을 발급한다.
	 *
	 * @param userId 이후 Authentication#principal 로 저장될 사용자 ID
	 * @param role   필터에서 "ROLE_{role}" 형태로 래핑할 단순 역할 문자열
	 */
	public String issueAccessToken(String userId, String role) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(accessMinutes * 60L);
		return Jwts.builder()
			.issuer(issuer)
			.subject(userId)
			.claim("role", role)
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp))
			.encryptWith(key, enc)     // JWE(alg=dir, enc=A256GCM) 생성
			.compact();
	}

	/**
	 * 재로그인 없이 세션을 연장할 수 있도록 장기 Refresh 토큰을 발급한다.
	 */
	public String issueRefreshToken(String userId) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(refreshDays * 86400L);
		return Jwts.builder()
			.issuer(issuer)
			.subject(userId)
			.claim("typ", "refresh")
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp))
			.encryptWith(key, enc)
			.compact();
	}

	/**
	 * 들어온 토큰을 복호화·검증한다. 서명/issuer/만료 검증은 jjwt 가 처리한다.
	 *
	 * @param compactJwe Authorization 헤더에서 추출한 암호화 토큰
	 * @return 후속 필터가 사용할 {@link Claims} 페이로드
	 * @throws JwtException 만료·위조 등으로 토큰이 유효하지 않을 때
	 */
	public Claims parseClaims(String compactJwe) throws JwtException {
		// 복호화 + 표준 클레임 파싱 (exp/iat 등 유효성은 JwtException으로 터짐)
		return Jwts.parser()
			.decryptWith(key)
			.requireIssuer(issuer)
			.build()
			.parseEncryptedClaims(compactJwe)
			.getPayload();
	}
}
