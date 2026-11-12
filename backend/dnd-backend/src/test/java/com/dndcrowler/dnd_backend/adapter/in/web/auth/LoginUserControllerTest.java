package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
public class LoginUserControllerTest {
	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private SecurityContextRepository securityContextRepository;

	@Mock
	private SessionAuthenticationStrategy sessionAuthenticationStrategy;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(
				new LoginUserController(
						authenticationManager,
						securityContextRepository,
						sessionAuthenticationStrategy))
				.setControllerAdvice(new LoginUserExceptionHandler())
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void logsInUserAndSavesSecurityContext() throws Exception {
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				42L,
				"alice",
				"alice@example.com",
				UserRole.USER);

		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));

		when(authenticationManager.authenticate(
				any(Authentication.class))).thenReturn(authentication);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "alice@example.com",
						  "password": "secret123"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.userName").value("alice"))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());

		verify(sessionAuthenticationStrategy).onAuthentication(
				eq(authentication), any(HttpServletRequest.class), any(HttpServletResponse.class));

		ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);

		verify(securityContextRepository).saveContext(
				contextCaptor.capture(), any(HttpServletRequest.class), any(HttpServletResponse.class));

		assertSame(
				authentication, contextCaptor.getValue().getAuthentication());

		ArgumentCaptor<Authentication> authenticationCaptor  = 
			ArgumentCaptor.forClass(Authentication.class);

		verify(authenticationManager).authenticate(
			authenticationCaptor.capture());

		Authentication request = authenticationCaptor.getValue();

		assertEquals("alice@example.com", request.getName());
		assertEquals("secret123", request.getCredentials());
	}

	@Test
	void returnsUnauthorizedForInvalidCredentials() throws Exception {
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenThrow(new BadCredentialsException(
						"Internal authentication message"));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(
						"""
							{
								"email": "alice@example.com",
								"password": "wrong-password"
							}	
						"""
					))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401))
					.andExpect(jsonPath("$.detail")
						.value("Invalid email or password"));
	}

	@Test 
	void returnsForbiddenForUnverifiedEmail() throws Exception {
		when(authenticationManager.authenticate(any(Authentication.class)))
			.thenThrow(new DisabledException(
				"Internal authentication message"
			));

		mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(
				"""
					{
						"email": "alice@example.com",
						"password": "secret123"
					}
				"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.detail")
					.value("Email address is not verified"));
	}

	@Test 
	void rejectsInvalidEmailBeforeAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(
				"""
					{
						"email": "invalid-email",
						"password": "secret123"
					}
				"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(
			authenticationManager,
			securityContextRepository,
			sessionAuthenticationStrategy
		);

	}

	@Test
	void rejectsBlankPasswordBeforeAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(
				"""
					{
						"email": "alice@example.com",
						"password": ""
					}
				"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(
			authenticationManager,
			securityContextRepository,
			sessionAuthenticationStrategy
		);
	}
}
