package com.mvp.v1.dandionna.config.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mvp.v1.dandionna.auth.service.TokenBlacklistService;

/**
 * Spring Security 구성을 담당하는 클래스.
 * 필터 체인을 얇게 유지하고, 토큰 파싱·키 생성 같은 무거운 작업은 별도 빈에게 맡긴다.
 * 전체 요청 라이프사이클은 doc/security-flow.md 를 참고한다.
 *
 * @author rua
 */
@Configuration
public class SecurityConfig {
	/**
	 * 무상태(Stateless) 보안 체인을 등록한다:
	 * <ol>
	 *     <li>쿠키 대신 토큰을 쓰므로 CSRF 를 비활성화한다.</li>
	 *     <li>{@code SessionCreationPolicy.STATELESS} 로 서버 세션 생성을 막는다.</li>
	 *     <li>헬스체크/인증 엔드포인트는 화이트리스트로 열고 나머지는 보호한다.</li>
	 *     <li>{@link JweAuthFilter} 를 UsernamePasswordAuthenticationFilter 앞에 추가해 Bearer 토큰을 먼저 처리한다.</li>
	 * </ol>
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JweTokenService tokens,
		TokenBlacklistService blacklistService) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/v1/api/auth/**").permitAll()
				.requestMatchers("/actuator/health").permitAll()
				.requestMatchers(HttpMethod.GET, "/public/**").permitAll()
				.anyRequest().authenticated()
			)
				.addFilterBefore(new JweAuthFilter(tokens, blacklistService), UsernamePasswordAuthenticationFilter.class)
			.httpBasic(Customizer.withDefaults());

		return http.build();
	}
}
