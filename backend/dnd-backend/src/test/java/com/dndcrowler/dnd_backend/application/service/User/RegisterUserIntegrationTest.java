package com.dndcrowler.dnd_backend.application.service.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.model.User.User;

@Testcontainers
@SpringBootTest
public class RegisterUserIntegrationTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final RegisterUserUseCase useCase;
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	RegisterUserIntegrationTest(RegisterUserUseCase useCase, JdbcTemplate jdbcTemplate) {
		this.useCase = useCase;
		this.jdbcTemplate = jdbcTemplate;
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
		assertEquals(1, users);
	}

}
