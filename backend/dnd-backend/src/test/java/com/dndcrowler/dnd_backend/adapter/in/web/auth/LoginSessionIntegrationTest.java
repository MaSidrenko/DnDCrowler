package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoginSessionIntegrationTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final MockMvc mockMvc;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordHasherPort passwordHasherPort;

	@Autowired
	LoginSessionIntegrationTest(
			MockMvc mockMvc,
			JdbcTemplate jdbcTemplate,
			PasswordHasherPort passwordHasherPort) {
		this.mockMvc = mockMvc;
		this.jdbcTemplate = jdbcTemplate;
		this.passwordHasherPort = passwordHasherPort;
	}

	@Test
	void createsAuthenticatedSessionForVerifiedUser() throws Exception {
		String email = "session-success@example.com";
		String password = "secret123";

		Long userId = insertUser("session-user", email, password, true);

		CsrfData csrf = obtainCsrf();

		MvcResult result = login(csrf, email, password)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(userId))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("USER"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		assertNotNull(session);

		SecurityContext securityContext = (SecurityContext) session.getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

		assertNotNull(securityContext);
		assertTrue(securityContext.getAuthentication().isAuthenticated());

		AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) securityContext.getAuthentication()
				.getPrincipal();

		assertEquals(userId, principal.userId());
		assertEquals(email, principal.email());
	}

	@Test
	void doesNotAuthenticateUserWithWrongPassword() throws Exception {
		String email = "session-wrong-password@example.com";

		insertUser("wrong-password-user", email, "correct-password", true);

		CsrfData csrf = obtainCsrf();

		login(csrf, email, "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		assertNull(csrf.session().getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
	}

	@Test
	void doesNotAuthenticateUserWithUnverifiedEmail() throws Exception {
		String email = "session-unverified@example.com";
		String password = "secret123";

		insertUser("unverifed-user", email, password, false);

		CsrfData csrf = obtainCsrf();

		login(csrf, email, password)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));

		assertNull(csrf.session().getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
	}

	@Test
	void returnsCurrentUserFromAuthenticatedSession() throws Exception {
		String username = "current-user";
		String email = "current-user@example.com";
		String password = "secret123";

		Long userId = insertUser(
				username,
				email,
				password,
				true);

		CsrfData csrf = obtainCsrf();

		MvcResult loginResult = login(csrf, email, password)
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest()
				.getSession(false);

		assertNotNull(authenticatedSession);

		mockMvc.perform(get("/api/auth/me")
				.session(authenticatedSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(userId))
				.andExpect(jsonPath("$.userName").value(username))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void rejectsCurrentUserRequestWithoutAuthenticatedSession() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions login(CsrfData csrf, String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.session(csrf.session())
				.header(csrf.headerName(), csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
									{
										"email": "%s",
										"password": "%s"
									}
								""".formatted(email, password)));
	}

	private CsrfData obtainCsrf() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();

		CsrfToken token = (CsrfToken) result.getRequest()
				.getAttribute(CsrfToken.class.getName());

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		assertNotNull(token);
		assertNotNull(session);

		return new CsrfData(
				session,
				token.getHeaderName(),
				token.getToken());
	}

	private Long insertUser(String username, String email, String password, boolean verified) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		Long userId = jdbcTemplate.queryForObject(
				"""
							INSERT INTO users (
								username,
								email,
								role,
								created_at,
								updated_at,
								email_verified_at
							)
							VALUES(?,?,?,?,?,?)
							RETURNING id
						""", Long.class,
				username,
				email,
				UserRole.USER.name(),
				now,
				now,
				verified ? now : null);
		jdbcTemplate.update(
				"""
						INSERT INTO password_credentials (
							user_id, password_hash, password_changed_at
						)
						VALUES(?,?,?)
						""",
				userId,
				passwordHasherPort.hashPassword(password),
				now);
		return userId;
	}
}
