package com.mvp.v1.dandionna.config.Security;

import java.io.IOException;
import java.util.List;

import com.mvp.v1.dandionna.auth.service.TokenBlacklistService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Spring Security 체인에 JWE 기반 인증을 연결하는 상태 없는 필터.
 * 주요 역할:
 * <ul>
 *     <li>{@code Authorization} 헤더에서 Bearer 토큰을 추출한다.</li>
 *     <li>{@link JweTokenService} 에게 검증/복호화를 위임한다.</li>
 *     <li>{@link SecurityContextHolder} 에 인증 객체를 채워 컨트롤러가 주체 정보를 읽게 한다.</li>
 * </ul>
 * 필터 배치는 doc/security-flow.md 에 시각화되어 있다.
 *
 * @author rua
 */
public class JweAuthFilter extends OncePerRequestFilter {
	private final JweTokenService tokens;
	private final TokenBlacklistService blacklistService;

	public JweAuthFilter(JweTokenService tokens,
		TokenBlacklistService blacklistService) {
		this.tokens = tokens;
		this.blacklistService = blacklistService;
	}

	/**
	 * {@link OncePerRequestFilter} 덕분에 요청당 한 번만 실행된다.
	 * 파싱에 실패하면 컨텍스트를 건드리지 않고 체인을 진행해, 이후 계층이 익명 사용자로 처리하도록 둔다.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
		throws ServletException, IOException {
		String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth != null && auth.startsWith("Bearer ")) {
			String token = auth.substring(7);
			try {
				if (blacklistService.isAccessTokenBlacklisted(token)) {
					chain.doFilter(req, res);
					return;
				}

				Claims claims = tokens.parseClaims(token);
				String userId = claims.getSubject();
				String role = String.valueOf(claims.get("role"));

				var authToken = new UsernamePasswordAuthenticationToken(
					userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			} catch (JwtException ex) {
				// Invalid/expired token -> leave context empty, downstream layer returns 401/403 as needed.
			}
		}
		chain.doFilter(req, res);
	}
}
