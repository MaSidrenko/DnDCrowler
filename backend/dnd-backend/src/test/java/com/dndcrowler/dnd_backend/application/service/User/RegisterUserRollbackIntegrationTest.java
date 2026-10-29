package com.dndcrowler.dnd_backend.application.service.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationEmailSenderPort;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.Email.VerificationCodeIssuedEvent;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;

@Testcontainers
@SpringBootTest
@MockitoBean(types = {
		PasswordCredentialsRepositoryPort.class,
		VerificationEmailSenderPort.class
})
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
	private final VerificationEmailSenderPort emailPort;
	private final ApplicationEventPublisher events;
	private final TransactionTemplate transactionTemplate;

	@Autowired
	RegisterUserRollbackIntegrationTest(
			RegisterUserUseCase useCase,
			JdbcTemplate jdbcTemplate,
			PasswordCredentialsRepositoryPort credentialsPort,
			VerificationEmailSenderPort emailPort,
			ApplicationEventPublisher events,
			PlatformTransactionManager transactionManager) {
		this.useCase = useCase;
		this.jdbcTemplate = jdbcTemplate;
		this.credentialsPort = credentialsPort;
		this.emailPort = emailPort;
		this.events = events;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
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

		Integer tokens = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM email_verification WHERE email = ?",
				Integer.class, email);
		assertEquals(0, tokens);
		verifyNoInteractions(emailPort);
	}

	@Test 
	void doesNotSendEmailWhenTransactionRollsBackAfterEventPublication() {
		String email = "rollback-event@example.com";
		String code = "123456";

		assertThrows(IllegalStateException.class, () -> {
			transactionTemplate.executeWithoutResult(status -> {
				events.publishEvent(
					new VerificationCodeIssuedEvent(email, code)
				);

				throw new IllegalStateException(
					"simulated failure after event publication"
				);
			});
		});

		verifyNoInteractions(emailPort);
	}
}
