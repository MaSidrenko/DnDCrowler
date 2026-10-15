package com.dndcrowler.dnd_backend.application.service.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;

@Testcontainers
@SpringBootTest
@MockitoBean(types = PasswordCredentialsRepositoryPort.class)
public class RegisterUserRollbackIntegrationTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final RegisterUserUseCase useCase;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordCredentialsRepositoryPort credentialsPort;

	@Autowired
	RegisterUserRollbackIntegrationTest(RegisterUserUseCase useCase, JdbcTemplate jdbcTemplate,
			PasswordCredentialsRepositoryPort credentialsPort) {
		this.useCase = useCase;
		this.jdbcTemplate = jdbcTemplate;
		this.credentialsPort = credentialsPort;
	}

	@Test
	void rollsBackUserWhenSavingCredentialsFails() {
		String email = "rollback@example.com";
		when(credentialsPort.save(any(PasswordCredentials.class)))
				.thenThrow(new IllegalStateException("simulated failure"));

		assertThrows(IllegalStateException.class,
				() -> useCase.registerUser(
						new RegisterUserCommand("alice", email, "secret123")));

		Integer users = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE email = ?",
				Integer.class, email);
		assertEquals(0, users);
	}
}
