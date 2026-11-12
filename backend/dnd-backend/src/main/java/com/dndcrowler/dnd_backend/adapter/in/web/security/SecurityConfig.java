package com.dndcrowler.dnd_backend.adapter.in.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final LoginAuthenticationProvider loginAuthenticationProvider;

	public SecurityConfig(
			LoginAuthenticationProvider loginAuthenticationProvider) {
		this.loginAuthenticationProvider = loginAuthenticationProvider;
	}

	@Bean
	AuthenticationManager authenticationManager() {
		return new ProviderManager(loginAuthenticationProvider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authenticationManager(authenticationManager())
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								HttpMethod.POST,
								"/api/auth/register",
								"/api/auth/email/confirm",
								"/api/auth/email/resend",
								"/api/auth/login")
						.permitAll()
						.requestMatchers(
								"/api/auth/csrf",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html")
						.permitAll()
						.anyRequest().authenticated())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.csrf(Customizer.withDefaults())
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.securityContext(security -> security
						.securityContextRepository(securityContextRepository())
						.requireExplicitSave(true))
				.exceptionHandling(exceptions -> exceptions
					.authenticationEntryPoint(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
					)		
				);

		return http.build();
	}
}
