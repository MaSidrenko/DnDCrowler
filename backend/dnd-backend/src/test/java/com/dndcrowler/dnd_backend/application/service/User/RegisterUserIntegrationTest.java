package com.dndcrowler.dnd_backend.application.service.User;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationEmailSenderPort;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.model.User.User;

@Testcontainers
@SpringBootTest
@MockitoBean(types = VerificationEmailSenderPort.class)
public class RegisterUserIntegrationTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final RegisterUserUseCase useCase;
	private final JdbcTemplate jdbcTemplate;
	private final VerificationEmailSenderPort emailPort;


	@Autowired
	RegisterUserIntegrationTest(
		RegisterUserUseCase useCase, 
		VerificationEmailSenderPort emailPort,
		JdbcTemplate jdbcTemplate
	) {
		this.useCase = useCase;
		this.jdbcTemplate = jdbcTemplate;
		this.emailPort = emailPort;
	}

	@Test
	void registerUser() {
		String email = "test@example.com";
		String password = "secret123";

		User user = useCase.registerUser(new RegisterUserCommand("alice", email, password));

		assertNotNull(user.id());
		assertEquals(email, user.email());

		Integer users = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE id = ? AND email = ?",
				Integer.class, user.id(), email);
		assertEquals(1, users);

		String hash = jdbcTemplate.queryForObject(
				"SELECT password_hash FROM password_credentials WHERE user_id = ?",
				String.class, user.id());

		assertNotEquals(password, hash);
		assertTrue(new BCryptPasswordEncoder().matches(password, hash));

		Long userIdFromEmailVerification = jdbcTemplate.queryForObject(
				"SELECT user_id FROM email_verification WHERE user_id = ?",
				Long.class, user.id());
		assertEquals(user.id(), userIdFromEmailVerification);

		String emailFromEmailVerification = jdbcTemplate.queryForObject(
				"SELECT email FROM email_verification WHERE user_id = ?",
				String.class, user.id());
		assertEquals(user.email(), emailFromEmailVerification);

		Timestamp usedAtFromEmailVerification = jdbcTemplate.queryForObject(
				"SELECT used_at FROM email_verification WHERE user_id = ?",
				Timestamp.class, user.id());
		assertNull(usedAtFromEmailVerification);

		Timestamp expiresAtFromEmailVerification = jdbcTemplate.queryForObject(
				"SELECT expires_at FROM email_verification WHERE user_id = ?",
				Timestamp.class, user.id());
		assertNotNull(expiresAtFromEmailVerification);
		assertTrue(expiresAtFromEmailVerification.toInstant().isAfter(Instant.now()));

		String tokenHashFromEmailVerification = jdbcTemplate.queryForObject(
				"SELECT token_hash FROM email_verification WHERE user_id =?",
				String.class, user.id());
		assertNotNull(tokenHashFromEmailVerification);

		ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailPort).sender(eq(email), codeCaptor.capture());

		String sentCode = codeCaptor.getValue();
		assertTrue(sentCode.matches("\\d{6}"));
		assertNotEquals(sentCode, tokenHashFromEmailVerification);
		assertTrue(new BCryptPasswordEncoder().matches(sentCode, tokenHashFromEmailVerification));
	}

	@Test
	void testRegisterUserWithRepeatedEmail() {
		String email = "test2@example.com";
		String password = "secret123";

		User user = useCase.registerUser(new RegisterUserCommand("alice", email, password));

		assertNotNull(user.id());
		assertEquals(email, user.email());

		assertThrows(EmailAlreadyRegisteredException.class,
				() -> useCase.registerUser(new RegisterUserCommand("alice", email, password)));

		Integer users = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE email = ?",
				Integer.class, email);

		Integer countTokenHashInEmailVerification = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM email_verification WHERE email = ?",
				Integer.class, email);

		assertEquals(1, countTokenHashInEmailVerification);
		assertEquals(1, users);

		verify(emailPort, times(1)).sender(eq(email), anyString());
	}

}
