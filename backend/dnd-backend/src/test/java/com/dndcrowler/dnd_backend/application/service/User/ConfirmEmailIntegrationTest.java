package com.dndcrowler.dnd_backend.application.service.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.ConfirmEmailUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationCodeHasherPort;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class ConfirmEmailIntegrationTest {
	private static final OffsetDateTime TEST_TIME = OffsetDateTime.parse("2026-01-01T00:00:00Z");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final ConfirmEmailUseCase confirmEmailUseCase;
	private final VerificationCodeHasherPort hasher;
	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	@Autowired
	ConfirmEmailIntegrationTest(ConfirmEmailUseCase confirmEmailUseCase, VerificationCodeHasherPort hasher,
			JdbcTemplate jdbcTemplate, DataSource dataSource) {
		this.confirmEmailUseCase = confirmEmailUseCase;
		this.hasher = hasher;
		this.jdbcTemplate = jdbcTemplate;
		this.dataSource = dataSource;
	}

	@Test
	void confirmEmailReturnTrue() {
		String email = "example@exmpl.com";
		String code = "123456";

		Long userId = jdbcTemplate.queryForObject("""
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

		String hashCode = hasher.hash(code);

		Long verifyId = jdbcTemplate.queryForObject("""
				INSERT INTO email_verification(user_id, email, token_hash, expires_at, used_at)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				userId,
				email,
				hashCode,
				OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5),
				null);

		boolean isVerified = confirmEmailUseCase.confirmEmail(new ConfirmEmailCommand(email, code));

		assertTrue(isVerified);

		User userFromDbByEmail = jdbcTemplate.queryForObject("SELECT * FROM users WHERE email = ?",
				(rs, rowNum) -> new User(
						rs.getLong("id"),
						rs.getString("username"),
						rs.getString("email"),
						UserRole.valueOf(rs.getString("role")),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant(),
						rs.getTimestamp("email_verified_at") == null
								? null
								: rs.getTimestamp("email_verified_at").toInstant()),
				email);

		EmailVerificationToken tokenFromDbByEmail = jdbcTemplate
				.queryForObject("SELECT * FROM email_verification WHERE email = ?",
						(rs, rowNum) -> new EmailVerificationToken(
								rs.getLong("id"),
								rs.getLong("user_id"),
								rs.getString("email"),
								rs.getString("token_hash"),
								rs.getTimestamp("expires_at") == null
										? null
										: rs.getTimestamp("expires_at").toInstant(),
								rs.getTimestamp("used_at") == null
										? null
										: rs.getTimestamp("used_at").toInstant()),
						email);

		assertEquals(userId, userFromDbByEmail.id());
		assertEquals(verifyId, tokenFromDbByEmail.id());
		assertNotNull(userFromDbByEmail.emailVerifiedAt());
		assertNotNull(tokenFromDbByEmail.usedAt());
		assertFalse(confirmEmailUseCase.confirmEmail(new ConfirmEmailCommand(email, code)));

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM email_verification WHERE email = ?", Integer.class, email));
	}

	@Test
	void confirmEmailUsedLastGivenCode() {
		String email = "example2@exmpl.com";
		String firstCode = "123456";
		String secondCode = "123356";

		Long userId = jdbcTemplate.queryForObject("""
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

		String firstHashCode = hasher.hash(firstCode);
		String secondHashCode = hasher.hash(secondCode);

		Long verifyId = jdbcTemplate.queryForObject("""
				INSERT INTO email_verification(user_id, email, token_hash, expires_at, used_at)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				userId,
				email,
				firstHashCode,
				OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5),
				null);
		Long verifyId2 = jdbcTemplate.queryForObject("""
				INSERT INTO email_verification(user_id, email, token_hash, expires_at, used_at)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				userId,
				email,
				secondHashCode,
				OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5),
				null);

		boolean notIsVerified = confirmEmailUseCase.confirmEmail(new ConfirmEmailCommand(email, firstCode));
		boolean isVerified = confirmEmailUseCase.confirmEmail(new ConfirmEmailCommand(email, secondCode));

		assertFalse(notIsVerified);
		assertTrue(isVerified);

		EmailVerificationToken firstTokenFromDbById = jdbcTemplate
				.queryForObject("SELECT * FROM email_verification WHERE id = ?",
						(rs, rowNum) -> new EmailVerificationToken(
								rs.getLong("id"),
								rs.getLong("user_id"),
								rs.getString("email"),
								rs.getString("token_hash"),
								rs.getTimestamp("expires_at") == null
										? null
										: rs.getTimestamp("expires_at").toInstant(),
								rs.getTimestamp("used_at") == null
										? null
										: rs.getTimestamp("used_at").toInstant()),
						verifyId);
		EmailVerificationToken secondTokenFromDbById = jdbcTemplate
				.queryForObject("SELECT * FROM email_verification WHERE id = ?",
						(rs, rowNum) -> new EmailVerificationToken(
								rs.getLong("id"),
								rs.getLong("user_id"),
								rs.getString("email"),
								rs.getString("token_hash"),
								rs.getTimestamp("expires_at") == null
										? null
										: rs.getTimestamp("expires_at").toInstant(),
								rs.getTimestamp("used_at") == null
										? null
										: rs.getTimestamp("used_at").toInstant()),
						verifyId2);

		assertNull(firstTokenFromDbById.usedAt());
		assertNotNull(secondTokenFromDbById.usedAt());
	}

	@Test
	void confirmEmailCallingTwoAccepts() throws Exception {
		String email = "example3@exmpl.com";
		String code = "123456";

		Long userId = jdbcTemplate.queryForObject("""
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

		String hashCode = hasher.hash(code);

		Long verifyId = jdbcTemplate.queryForObject("""
				INSERT INTO email_verification(user_id, email, token_hash, expires_at, used_at)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				userId,
				email,
				hashCode,
				OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5),
				null);


		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ConfirmEmailCommand command = new ConfirmEmailCommand(email, code);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<Boolean> first = null;
		Future<Boolean> second = null;

		try (Connection lockConnection = dataSource.getConnection()) {
			lockConnection.setAutoCommit(false);
			try (PreparedStatement statement = lockConnection.prepareStatement(
					"SELECT id FROM email_verification WHERE id = ? FOR UPDATE")) {
				statement.setLong(1, verifyId);
				try (ResultSet rows = statement.executeQuery()) {
					assertTrue(rows.next());
				}
			}

			Callable<Boolean> attempt = () -> {
				ready.countDown();
				if (!start.await(5, TimeUnit.SECONDS)) {
					throw new TimeoutException("Start signal not received");
				}
				return confirmEmailUseCase.confirmEmail(command);
			};
			first = executor.submit(attempt);
			second = executor.submit(attempt);

			try {
				assertTrue(ready.await(5, TimeUnit.SECONDS));
				start.countDown();
				assertTrue(awaitBlockedVerificationReads(), "Both confirmations should wait for the token lock");
			} finally {
				lockConnection.rollback();
			}

			int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
					+ (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
			assertEquals(1, successes);
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

		assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
				"SELECT used_at IS NOT NULL FROM email_verification WHERE id = ?", Boolean.class, verifyId));
		assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
				"SELECT email_verified_at IS NOT NULL FROM users WHERE id = ?", Boolean.class, userId));
	}

	private boolean awaitBlockedVerificationReads() throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			Integer waiting = jdbcTemplate.queryForObject("""
					SELECT COUNT(*) FROM pg_stat_activity
					WHERE wait_event_type = 'Lock'
					  AND query ILIKE 'select%from email_verification%'
					""", Integer.class);
			if (waiting != null && waiting >= 2) {
				return true;
			}
			Thread.sleep(25);
		}
		return false;
	}

}
