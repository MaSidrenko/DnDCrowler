package com.dndcrowler.dnd_backend.adapter.in.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import jakarta.servlet.DispatcherType;

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
		http
				.authenticationManager(authenticationManager())
				.authorizeHttpRequests(authorize -> authorize
						.dispatcherTypeMatchers(
								DispatcherType.ERROR)
						.permitAll()
						.requestMatchers(
								HttpMethod.POST,
								"/api/auth/register",
								"/api/auth/email/confirm",
								"/api/auth/email/resend",
								"/api/auth/login")
						.permitAll()
						.requestMatchers(
								HttpMethod.GET,
								"/api/auth/csrf")
						.permitAll()
						.requestMatchers(
								"/v3/api-docs",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html")
						.permitAll()
						.anyRequest().authenticated())
				.sessionManagement(session -> session
						.sessionCreationPolicy(
								SessionCreationPolicy.IF_REQUIRED))
				.csrf(csrf -> csrf
						.ignoringRequestMatchers(
								PathPatternRequestMatcher.pathPattern(
										HttpMethod.POST,
										"/api/auth/register"),
								PathPatternRequestMatcher.pathPattern(
										HttpMethod.POST,
										"/api/auth/email/confirm"),
								PathPatternRequestMatcher.pathPattern(
										HttpMethod.POST,
										"/api/auth/email/resend")))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.securityContext(security -> security
						.securityContextRepository(
								securityContextRepository())
						.requireExplicitSave(true))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler(
								new HttpStatusReturningLogoutSuccessHandler(
										HttpStatus.NO_CONTENT))
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID"))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(
								new HttpStatusEntryPoint(
										HttpStatus.UNAUTHORIZED)));

		return http.build();
	}
}
