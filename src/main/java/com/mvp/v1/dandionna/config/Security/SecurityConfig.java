package com.mvp.v1.dandionna.config.Security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.mvp.v1.dandionna.auth.service.TokenBlacklistService;

/**
 * Spring Security 구성을 담당하는 클래스.
 * 필터 체인을 얇게 유지하고, 토큰 파싱·키 생성 같은 무거운 작업은 별도 빈에게 맡긴다.
 * 전체 요청 라이프사이클은 doc/security-flow.md 를 참고한다.
 *
 * @author rua
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class})
public class SecurityConfig {

	private final CorsProperties corsProperties;
	private final RateLimitProperties rateLimitProperties;

	public SecurityConfig(CorsProperties corsProperties, RateLimitProperties rateLimitProperties) {
		this.corsProperties = corsProperties;
		this.rateLimitProperties = rateLimitProperties;
	}

	/**
	 * 무상태(Stateless) 보안 체인을 등록한다:
	 * <ol>
	 *     <li>쿠키 대신 토큰을 쓰므로 CSRF 를 비활성화한다.</li>
	 *     <li>{@code SessionCreationPolicy.STATELESS} 로 서버 세션 생성을 막는다.</li>
	 *     <li>헬스체크/인증 엔드포인트는 화이트리스트로 열고 나머지는 보호한다.</li>
	 *     <li>{@link JweAuthFilter} 를 UsernamePasswordAuthenticationFilter 앞에 추가해 Bearer 토큰을 먼저 처리한다.</li>
	 *     <li>보안 헤더(X-Content-Type-Options, X-Frame-Options, HSTS 등)를 설정한다.</li>
	 * </ol>
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JweTokenService tokens,
		TokenBlacklistService blacklistService, StringRedisTemplate redisTemplate) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers
				.contentTypeOptions(Customizer.withDefaults())
				.frameOptions(frame -> frame.deny())
				.httpStrictTransportSecurity(hsts -> hsts
					.includeSubDomains(true)
					.maxAgeInSeconds(31536000))
				.referrerPolicy(referrer -> referrer
					.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
				.permissionPolicy(permissions -> permissions
					.policy("camera=(), microphone=(), geolocation=(self)"))
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/auth/**").permitAll()
				.requestMatchers("/actuator/health", "/actuator/prometheus",
					"/swagger-ui.html", "/swagger-ui/**",
					"/v3/api-docs/**", "/api-docs/**").permitAll()
				.requestMatchers(HttpMethod.GET,
					"/public/**",
					"/store-admin.html",
					"/static/**",
					"/favicon.ico").permitAll()
				// 소비자 전용
				.requestMatchers("/api/v1/home/**").hasRole("CONSUMER")
				.requestMatchers(HttpMethod.GET, "/api/v1/stores/*/no-show-posts").hasRole("CONSUMER")
				.requestMatchers("/api/v1/orders/**").hasRole("CONSUMER")
				.requestMatchers("/api/v1/favorites/**").hasRole("CONSUMER")
				// 사장님 전용
				.requestMatchers("/api/v1/owner/**").hasRole("OWNER")
				.requestMatchers("/api/v1/stores/**").hasRole("OWNER")
				// 관리자 전용
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				// 공통 인증 필요
				.requestMatchers("/api/v1/push/**").authenticated()
				.anyRequest().authenticated()
			)
			.addFilterBefore(new JweAuthFilter(tokens, blacklistService), UsernamePasswordAuthenticationFilter.class)
			.addFilterAfter(new RateLimitFilter(redisTemplate, rateLimitProperties), JweAuthFilter.class)
			.httpBasic(httpBasic -> httpBasic.disable());

		return http.build();
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
					.allowedMethods(corsProperties.allowedMethods().toArray(String[]::new))
					.allowedHeaders(corsProperties.allowedHeaders().toArray(String[]::new))
					.allowCredentials(true)
					.maxAge(3600);
			}
		};
	}
}
