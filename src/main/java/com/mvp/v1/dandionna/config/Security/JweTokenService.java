package com.mvp.v1.dandionna.config.Security;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.AeadAlgorithm;

/**
 * JSON Web Encryption(JWE) 토큰 발급·검증을 담당하는 상태 없는 서비스.
 * 키 로테이션을 지원하여 현재 키로 발급하고, 이전 키로도 검증할 수 있다.
 * 요청 처리 흐름은 doc/security-flow.md 를 참고한다.
 *
 * @author rua
 */
@Service
public class JweTokenService {
	private final SecretKey currentKey;
	private final Map<String, SecretKey> keyMap;
	private final AeadAlgorithm enc;
	private final String issuer;
	private final String kid;
	private final int accessMinutes;
	private final int refreshDays;

	public JweTokenService(SecretKey jweSecretKey, Map<String, SecretKey> jweKeyMap,
		AeadAlgorithm enc, JwtProps props) {
		this.currentKey = jweSecretKey;
		this.keyMap = jweKeyMap;
		this.enc = enc;
		this.issuer = props.jwt().issuer();
		this.kid = props.jwe().kid();
		this.accessMinutes = props.jwt().accessMinutes();
		this.refreshDays = props.jwt().refreshDays();
	}

	/**
	 * 컨트롤러 인가에 사용하는 단기 Access 토큰을 발급한다.
	 * kid 헤더를 포함하여 키 로테이션 시 어떤 키로 암호화했는지 식별한다.
	 */
	public String issueAccessToken(String userId, String role) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(accessMinutes * 60L);
		return Jwts.builder()
			.header().add("kid", kid).and()
			.issuer(issuer)
			.subject(userId)
			.claim("role", role)
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp))
			.encryptWith(currentKey, enc)
			.compact();
	}

	/**
	 * 재로그인 없이 세션을 연장할 수 있도록 장기 Refresh 토큰을 발급한다.
	 */
	public String issueRefreshToken(String userId) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(refreshDays * 86400L);
		return Jwts.builder()
			.header().add("kid", kid).and()
			.issuer(issuer)
			.subject(userId)
			.claim("typ", "refresh")
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp))
			.encryptWith(currentKey, enc)
			.compact();
	}

	/**
	 * 들어온 토큰을 복호화·검증한다.
	 * 현재 키로 먼저 시도하고, 실패 시 이전 키들로 시도한다 (키 로테이션 지원).
	 *
	 * @param compactJwe Authorization 헤더에서 추출한 암호화 토큰
	 * @return 후속 필터가 사용할 {@link Claims} 페이로드
	 * @throws JwtException 만료·위조 등으로 토큰이 유효하지 않을 때
	 */
	public Claims parseClaims(String compactJwe) throws JwtException {
		// 현재 키로 먼저 시도
		try {
			return parseWithKey(compactJwe, currentKey);
		} catch (JwtException ex) {
			// 현재 키 실패 시, 다른 키들로 시도
			for (Map.Entry<String, SecretKey> entry : keyMap.entrySet()) {
				if (entry.getValue().equals(currentKey)) continue;
				try {
					return parseWithKey(compactJwe, entry.getValue());
				} catch (JwtException ignored) {
					// 다음 키 시도
				}
			}
			throw ex; // 모든 키 실패
		}
	}

	private Claims parseWithKey(String compactJwe, SecretKey key) throws JwtException {
		return Jwts.parser()
			.decryptWith(key)
			.requireIssuer(issuer)
			.build()
			.parseEncryptedClaims(compactJwe)
			.getPayload();
	}
}
