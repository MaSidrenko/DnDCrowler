package com.dndcrowler.dnd_backend.application.service.User;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.dndcrowler.dnd_backend.application.port.in.LoginUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.LoginUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.application.service.exception.InvalidCredentialsException;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class LoginUserIntegrationTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final LoginUserUseCase loginUserUseCase;
	private final PasswordHasherPort passwordHasherPort;
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	LoginUserIntegrationTest(LoginUserUseCase loginUserUseCase,
			PasswordHasherPort passwordHasherPort, 
			JdbcTemplate jdbcTemplate) {
		this.loginUserUseCase = loginUserUseCase;
		this.passwordHasherPort = passwordHasherPort;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	void logsInVerifiedUserWithValidCredentials() {
		String email = "firstTest@email.com";
		String password = "testpass";
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		Long userId = jdbcTemplate.queryForObject(
				"""
							INSERT INTO users (username, email, role, created_at, updated_at, email_verified_at)
							VALUES (?, ?, ?, ?, ?, ?)
							RETURNING id
						""", Long.class,
				"firstTestUser",
				email,
				UserRole.USER.name(),
				now,
				now,
				now);

		String hashedPassword = passwordHasherPort.hashPassword(password);
		int insertedCredentials = jdbcTemplate.update(
				"""
						INSERT INTO password_credentials (
						    user_id,
						    password_hash,
						    password_changed_at
						)
						VALUES (?, ?, ?)
						""",
				userId,
				hashedPassword,
				now);
		assertEquals(1, insertedCredentials);
		LoginUserCommand command = new LoginUserCommand(email, password);

		User user = loginUserUseCase.loginUser(command);

		assertEquals(userId, user.id());
		assertEquals(email, user.email());
	}

	@Test 
	void rejectsInvalidPasswordWithRealBCryptHash() {
		String email = "wrong@email.com";
		String correctPassword = "correctpass";
		String wrongPassword = "wrongpass";
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		Long userId = jdbcTemplate.queryForObject(
			"""
				INSERT INTO users(
					username,
					email,
					role,
					created_at,
					updated_at,
					email_verified_at
				)
				VALUES (?, ?, ?, ?, ?, ?)
				RETURNING id
			""", Long.class,
			"wrongPasswordUser",
			email,
			UserRole.USER.name(),
			now,
			now,
			now
		);

		String passwordHash = passwordHasherPort.hashPassword(correctPassword);

		int insertedCredentials = jdbcTemplate.update(
			"""
				INSERT INTO password_credentials (
					user_id,
					password_hash,
					password_changed_at
				)
				VALUES (?, ?, ?)
			""",
			userId,
			passwordHash,
			now
		);

		assertEquals(1, insertedCredentials);

		LoginUserCommand command = new LoginUserCommand(email, wrongPassword);

		assertThrows(InvalidCredentialsException.class, () ->
			 loginUserUseCase.loginUser(command));
	}
}
