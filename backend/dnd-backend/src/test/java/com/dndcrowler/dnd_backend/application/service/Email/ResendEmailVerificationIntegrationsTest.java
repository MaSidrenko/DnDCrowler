package com.dndcrowler.dnd_backend.application.service.Email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.ResendEmailVerificationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ResendEmailVerificationCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationCodeHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationEmailSenderPort;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = VerificationEmailSenderPort.class)
public class ResendEmailVerificationIntegrationsTest {
	private static final OffsetDateTime TEST_TIME = OffsetDateTime.parse("2026-01-01T00:00:00Z");
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final ResendEmailVerificationUseCase resendEmailUseCase;
	private final VerificationCodeHasherPort hasher;
	private final JdbcTemplate jdbcTemplate;
	private final VerificationEmailSenderPort emailSender;
	private final DataSource dataSource;

	@Autowired
	ResendEmailVerificationIntegrationsTest(
			ResendEmailVerificationUseCase resendEmailUseCase, JdbcTemplate jdbcTemplate,
			VerificationCodeHasherPort hasher, VerificationEmailSenderPort emailSender,
			DataSource dataSource) {
		this.resendEmailUseCase = resendEmailUseCase;
		this.jdbcTemplate = jdbcTemplate;
		this.hasher = hasher;
		this.emailSender = emailSender;
		this.dataSource = dataSource;
	}

	@Test
	void cooldownActive() {
		String email = "unverifed@example.com";
		Long userId = jdbcTemplate.queryForObject(
				"""
							INSERT INTO users(username, email, role, created_at, updated_at)
							VALUES (?, ?, ?, ?, ?)
							RETURNING id
						""",
				Long.class,
				"Maxim",
				email,
				UserRole.USER.name(),
				TEST_TIME,
				TEST_TIME);

		String hashCode = hasher.hash("123456");
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		int inserted = jdbcTemplate.update(
				"""
						INSERT INTO email_verification(user_id, email, token_hash, expires_at, created_at)
						VALUES (?, ?, ?, ?, ?)
						""",
				userId,
				email,
				hashCode,
				now.plusMinutes(5),
				now);
		assertEquals(1, inserted);

		resendEmailUseCase.resend(new ResendEmailVerificationCommand(email));

		Integer tokens = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM email_verification WHERE email = ?",
				Integer.class, email);

		assertEquals(1, tokens);
		verifyNoInteractions(emailSender);

	}

	@Test
	void cooldownInactive() {
		String email = "unverifed2@example.com";
		Long userId = jdbcTemplate.queryForObject(
				"""
							INSERT INTO users(username, email, role, created_at, updated_at)
							VALUES (?, ?, ?, ?, ?)
							RETURNING id
						""",
				Long.class,
				"Maxim",
				email,
				UserRole.USER.name(),
				TEST_TIME,
				TEST_TIME);

		String hashCode = hasher.hash("123456");
		OffsetDateTime oldCreatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(61);
		int inserted = jdbcTemplate.update(
				"""
						INSERT INTO email_verification(
						    user_id,
						    email,
						    token_hash,
						    expires_at,
						    created_at
						)
						VALUES (?, ?, ?, ?, ?)
						""",
				userId,
				email,
				hashCode,
				oldCreatedAt.plusMinutes(5),
				oldCreatedAt);
		assertEquals(1, inserted);

		resendEmailUseCase.resend(new ResendEmailVerificationCommand(email));

		Integer tokens = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM email_verification WHERE email = ?",
				Integer.class, email);

		assertEquals(2, tokens);

		Long latestTokenUserId = jdbcTemplate.queryForObject(
				"""
						SELECT user_id
						FROM email_verification
						WHERE email = ?
						ORDER BY id DESC
						LIMIT 1
						""",
				Long.class,
				email);

		assertEquals(userId, latestTokenUserId);

		Instant latestCreatedAt = jdbcTemplate.queryForObject(
				"""
						SELECT created_at
						FROM email_verification
						WHERE email = ?
						ORDER BY id DESC
						LIMIT 1
						""",
				(rs, rowNum) -> rs.getTimestamp("created_at").toInstant(),
				email);

		assertTrue(
				latestCreatedAt.isAfter(oldCreatedAt.toInstant()));

		ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

		verify(emailSender).sender(
				eq(email),
				codeCaptor.capture());

		String sentCode = codeCaptor.getValue();

		assertTrue(sentCode.matches("\\d{6}"));

		String latestTokenHash = jdbcTemplate.queryForObject(
				"""
						SELECT token_hash
						FROM email_verification
						WHERE email = ?
						ORDER BY id DESC
						LIMIT 1
						""",
				String.class,
				email);

		assertTrue(
				hasher.matches(sentCode, latestTokenHash));
	}

	@Test
	void concurrentResendsCreateOnlyOneNewToken() throws Exception {
		String email = "concurrent-resend@example.com";

		Long userId = jdbcTemplate.queryForObject(
				"""
						INSERT INTO users(username, email, role, created_at, updated_at)
						VALUES (?, ?, ?, ?, ?)
						RETURNING id
						""",
				Long.class,
				"concurrent_user",
				email,
				UserRole.USER.name(),
				TEST_TIME,
				TEST_TIME);

		OffsetDateTime oldCreatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(61);

		int inserted = jdbcTemplate.update(
				"""
						INSERT INTO email_verification(
						    user_id,
						    email,
						    token_hash,
						    expires_at,
						    created_at
						)
						VALUES (?, ?, ?, ?, ?)
						""",
				userId,
				email,
				hasher.hash("123456"),
				oldCreatedAt.plusMinutes(5),
				oldCreatedAt);
		assertEquals(1, inserted);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<Void> first = null;
		Future<Void> second = null;

		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand(email);

		try (Connection lockConnection = dataSource.getConnection()) {
			lockConnection.setAutoCommit(false);

			try (PreparedStatement statement = lockConnection.prepareStatement(
					"""
							SELECT id
							FROM users
							WHERE id = ?
							FOR UPDATE
							""")) {

				statement.setLong(1, userId);

				try (ResultSet rows = statement.executeQuery()) {
					assertTrue(rows.next());
				}
			}

			Callable<Void> attempt = () -> {
				ready.countDown();

				if (!start.await(5, TimeUnit.SECONDS)) {
					throw new TimeoutException("Start signal not received");
				}

				resendEmailUseCase.resend(command);
				return null;
			};

			first = executor.submit(attempt);
			second = executor.submit(attempt);

			try {
				assertTrue(ready.await(5, TimeUnit.SECONDS));

				start.countDown();

				assertTrue(
						awaitBlockedResendReads(),
						"Both resend requests should wait for the token lock");
			} finally {
				lockConnection.rollback();
			}

			first.get(10, TimeUnit.SECONDS);
			second.get(10, TimeUnit.SECONDS);
		} finally {
			if (first != null) {
				first.cancel(true);
			}

			if (second != null) {
				second.cancel(true);
			}

			executor.shutdownNow();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		}

		Integer tokenCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM email_verification WHERE email = ?",
				Integer.class,
				email);

		assertEquals(2, tokenCount);

		verify(emailSender, times(1))
				.sender(eq(email), anyString());
	}

	private boolean awaitBlockedResendReads() throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

		while (System.nanoTime() < deadline) {
			Integer waiting = jdbcTemplate.queryForObject(
					"""
							SELECT COUNT(*)
							FROM pg_stat_activity
							WHERE wait_event_type = 'Lock'
							  AND query ILIKE 'select%from users%'
							""",
					Integer.class);

			if (waiting != null && waiting >= 2) {
				return true;
			}

			Thread.sleep(25);
		}

		return false;
	}
}
