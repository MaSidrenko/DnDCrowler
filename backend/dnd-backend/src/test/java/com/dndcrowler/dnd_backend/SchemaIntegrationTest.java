package com.dndcrowler.dnd_backend;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
public class SchemaIntegrationTest {
	private static final OffsetDateTime TEST_TIME = OffsetDateTime.parse("2024-01-01T00:00:00Z");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void containerShouldBeRunning() {
		assertTrue(postgreSQLContainer.isRunning());
	}

	@Test
	void characterTableShouldExist() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'characters'",
				Integer.class);
		assertTrue(count != null && count > 0, "Character table should exist");
	}

	@Test
	void shouldInsertUser() {
		OffsetDateTime now = OffsetDateTime.parse("2024-01-01T00:00:00Z");

		int rows = jdbcTemplate.update(
				"INSERT INTO users (username, email, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
				"test_user", "insert@example.com", "USER", now, now);

		assertEquals(1, rows);
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE email = ?", Integer.class,
				"insert@example.com"));
	}

	@Test
	void shouldRejectDuplicateEmail() {
		OffsetDateTime now = OffsetDateTime.parse("2024-01-01T00:00:00Z");
		String sql = "INSERT INTO users (username, email, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

		jdbcTemplate.update(sql, "first", "duplicate@example.com", "USER", now, now);

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbcTemplate.update(sql, "second", "duplicate@example.com",
						"USER", now, now));
	}

	@Test
	void shouldRejectCharacterWithoutOwner() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertCharacter(-1L, "OrphanCharacter"));
	}

	@Test
	void shouldCascadeDeleteUser() {
		Long userId = insertUser("cascade@example.com");
		Long characterId = insertCharacter(userId, "User Character");
		Long skillId = insertSkill(characterId);
		Long effectId = insertEffect(skillId);
		jdbcTemplate.update(
				"INSERT INTO password_credentials (user_id, password_hash, password_changed_at) VALUES (?, ?, ?)",
				userId, "test-hash", TEST_TIME);
		Long tokenId = jdbcTemplate.queryForObject(
				"INSERT INTO email_verification (user_id, email, token_hash, expires_at) VALUES (?, ?, ?, ?) RETURNING id",
				Long.class, userId, "cascade@example.com", "cascade-token-hash", TEST_TIME.plusDays(1));

		assertEquals(1, jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM characters WHERE id = ?", Integer.class, characterId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skills WHERE id = ?", Integer.class, skillId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skill_effects WHERE id = ?", Integer.class, effectId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_credentials WHERE user_id = ?", Integer.class, userId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_verification WHERE id = ?", Integer.class, tokenId));
	}

	@Test
	void shouldCascadeDeleteCharacter() {
		Long userId = insertUser("cascade-character@example.com");
		Long characterId = insertCharacter(userId, "Test Character");
		Long skillId = insertSkill(characterId);
		Long effectId = insertEffect(skillId);

		assertEquals(1, jdbcTemplate.update("DELETE FROM characters WHERE id = ?", characterId));
		assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM characters WHERE id = ?", Integer.class, characterId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skills WHERE id = ?", Integer.class, skillId));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skill_effects WHERE id = ?", Integer.class, effectId));
	}

	@Test
	void shouldRejectDuplicateVerificationTokenHash() {
		Long userId = insertUser("token@example.com");
		String emailVerificationSql = "INSERT INTO email_verification (user_id, email, token_hash, expires_at) VALUES (?, ?, ?, ?)";

		jdbcTemplate.update(emailVerificationSql,
				userId, "token@example.com", "hash123", TEST_TIME.plusDays(1));

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbcTemplate.update(emailVerificationSql,
						userId, "token@example.com", "hash123", TEST_TIME.plusDays(1)));
	}

	@Test
	void shouldRejectCredentialsWithoutUser() {
		String sql = "INSERT INTO password_credentials (user_id, password_hash, password_changed_at) VALUES (?, ?, ?)";

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbcTemplate.update(sql, -1L, "test-hash", TEST_TIME));
	}

	private Long insertUser(String email) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO users (username, email, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?) RETURNING id",
				Long.class, "test_user", email, "USER", TEST_TIME, TEST_TIME);
	}

	private Long insertCharacter(Long ownerId, String name) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO characters
				  (name, description, owner_id, level, race,
				   strength, dexterity, constitution, intelligence, wisdom, charisma,
				   character_class, visibility)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class,
				name, "A test character", ownerId, 1, "HUMAN",
				10, 10, 10, 10, 10, 10, "FIGHTER", "PUBLIC");
	}

	private Long insertSkill(Long characterId) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO skills (character_id, name, description, activation_type, usage_policy) VALUES (?, ?, ?, ?, ?) RETURNING id",
				Long.class, characterId, "Test skill", "Test description", "PASSIVE", "ONE");
	}

	private Long insertEffect(Long skillId) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO skill_effects (skill_id, effect_type, stat, amount) VALUES (?, ?, ?, ?) RETURNING id",
				Long.class, skillId, "STAT_BONUS", "STR", 2);
	}
}
