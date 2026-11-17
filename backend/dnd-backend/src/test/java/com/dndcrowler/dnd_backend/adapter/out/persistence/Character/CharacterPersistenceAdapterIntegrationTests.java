package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;
import com.dndcrowler.dnd_backend.domain.model.Character.Stat;
import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.DamageBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.Effect;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;
import com.dndcrowler.dnd_backend.domain.model.Skills.StatBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CharacterPersistenceAdapterIntegrationTests {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final CharacterPersistenceAdapter adapter;
	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;

	@Autowired
	CharacterPersistenceAdapterIntegrationTests(
			CharacterPersistenceAdapter adapter,
			JdbcTemplate jdbcTemplate,
			EntityManager entityManager) {
		this.adapter = adapter;
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	@Test
	void successCreateCharacter() {
		String email = "FirstEmail@test.com";
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		Long userId = jdbcTemplate.queryForObject(
				"""
							INSERT INTO users (username, email, role, created_at, updated_at, email_verified_at)
							VALUES(?,?,?,?,?,?)
							RETURNING id
						""", Long.class,
				"firstTestUser",
				email,
				UserRole.USER.name(),
				now,
				now,
				now);
		AbilityScores abilityScores = AbilityScores.create(
				10, 10, 10, 20, 12, 16);

		Effect effect = StatBonus.create(Stat.CHA, 4);
		List<Effect> effects = new ArrayList<Effect>();

		effects.add(effect);

		Skills skill = Skills.create(
				"TestSkill", "Skill for test",
				ActivationType.PASSIVE, effects, UsagePolicy.NONE);
		List<Skills> skills = new ArrayList<Skills>();

		skills.add(skill);

		Character newChar = Character.create("Test Wizard", "Character created by persistence test", userId,
				Race.ELF, abilityScores, skills,
				CharacterClass.WIZARD,
				CharacterVisibility.PUBLIC);

		Character saved = adapter.save(newChar);

		assertNotNull(saved.id());
		assertEquals(1, saved.skills().size());
		assertNotNull(saved.skills().getFirst().id());

		Long skillCharacterId = jdbcTemplate.queryForObject(
				"""
							SELECT character_id FROM skills WHERE character_id = ?
						""", Long.class, saved.id());
		assertEquals(saved.id(), skillCharacterId);
		Long skillId = jdbcTemplate.queryForObject(
				"""
							SELECT id FROM skills WHERE character_id = ?
						""", Long.class, saved.id());
		Long effectSkillId = jdbcTemplate.queryForObject(
				"""
							SELECT skill_id FROM skill_effects WHERE skill_id = ?
						""", Long.class, skillId);
		assertEquals(skillId, effectSkillId);

		Map<String, Object> effectData = jdbcTemplate.queryForMap(
				"""
							SELECT effect_type, stat, amount
							FROM skill_effects
							WHERE skill_id = ?
						""", skillId);
		assertEquals("STAT_BONUS", effectData.get("effect_type"));
		assertEquals("CHA", effectData.get("stat"));
		assertEquals(4, ((Number) effectData.get("amount")).intValue());

		int skillCount = jdbcTemplate.queryForObject(
				"""
							SELECT COUNT(*) FROM skills WHERE character_id = ?
						""", int.class, saved.id());
		int effectCount = jdbcTemplate.queryForObject(
				"""
							SELECT COUNT(*) FROM skill_effects WHERE skill_id = ?
						""", int.class, skillId);

		assertEquals(1, skillCount);
		assertEquals(1, effectCount);

	}

	@Test
	void loadingByCharacterId() {
		Long userId = insertUser("load-character@example.com");

		AbilityScores scores = AbilityScores.create(
				8, 14, 12, 18, 16, 10);

		Skills skills = Skills.create(
				"Arcane Knowledge",
				"Increases Intelligence",
				ActivationType.PASSIVE,
				List.of(new StatBonus(Stat.INT, 3)),
				UsagePolicy.NONE);

		Character saved = adapter.save(Character.create("Test Mage",
				"Character used for loading test",
				userId,
				Race.ELF, scores,
				List.of(skills),
				CharacterClass.WIZARD,
				CharacterVisibility.PRIVATE));

		entityManager.clear();

		Character loaded = adapter.findById(saved.id())
				.orElseThrow();

		assertEquals(saved.id(), loaded.id());
		assertEquals(userId, loaded.ownerID());
		assertEquals("Test Mage", loaded.name());
		assertEquals("Character used for loading test", loaded.description());
		assertEquals(1, loaded.level());
		assertEquals(Race.ELF, loaded.race());
		assertEquals(CharacterClass.WIZARD, loaded.characterClass());
		assertEquals(CharacterVisibility.PRIVATE, loaded.characterVisibility());
		assertEquals(scores, loaded.abilityScores());

		assertEquals(1, loaded.skills().size());

		Skills loadedSkill = loaded.skills().getFirst();

		assertNotNull(loadedSkill.id());
		assertEquals("Arcane Knowledge", loadedSkill.name());
		assertEquals("Increases Intelligence", loadedSkill.description());
		assertEquals(ActivationType.PASSIVE, loadedSkill.activationType());
		assertEquals(UsagePolicy.NONE, loadedSkill.usagePolicy());

		assertEquals(1, loadedSkill.effects().size());

		StatBonus loadedEffect = assertInstanceOf(
				StatBonus.class, loadedSkill.effects().getFirst());

		assertEquals(Stat.INT, loadedEffect.stat());
		assertEquals(3, loadedEffect.amount());
	}

	@Test
	void findByIdReturnsEmptyWhenCharacterDoesNotExist() {
		assertTrue(adapter.findById(Long.MAX_VALUE).isEmpty());
	}

	@Test
	void updateCharacter() {
		Long userId = insertUser("update-character@example.com");

		AbilityScores initialScores = AbilityScores.create(
				20, 16, 8, 8, 10, 12);
		Skills initialSkill = Skills.create(
				"Massive Attack",
				"Warrior makes a massive attack",
				ActivationType.ACTIVE,
				List.of(new DamageBonus(3)),
				UsagePolicy.ONE_FIGHT);

		Character saved = adapter.save(Character.create(
				"Test Warrior",
				"Character used for update test",
				userId,
				Race.GNOME,
				initialScores,
				List.of(initialSkill),
				CharacterClass.FIGHTER,
				CharacterVisibility.PRIVATE));

		Long oldSkillId = saved.skills().getFirst().id();
		Long oldEffectId = jdbcTemplate.queryForObject(
				"SELECT id FROM skill_effects WHERE skill_id = ?",
				Long.class,
				oldSkillId);

		entityManager.clear();

		AbilityScores updatedScores = AbilityScores.create(
				14, 18, 12, 10, 16, 8);
		Skills updatedSkill = Skills.create(
				"Shield Bash",
				"Deals additional shield damage",
				ActivationType.ACTIVE,
				List.of(new DamageBonus(7)),
				UsagePolicy.ONE_DAY);

		Character updated = adapter.save(new Character(
				saved.id(),
				"Warrior",
				"Updated Warrior",
				userId,
				2,
				Race.HUMAN,
				updatedScores,
				List.of(updatedSkill),
				CharacterClass.MONK,
				CharacterVisibility.PUBLIC));

		assertEquals(saved.id(), updated.id());
		assertEquals(userId, updated.ownerID());
		assertEquals("Warrior", updated.name());
		assertEquals("Updated Warrior", updated.description());
		assertEquals(2, updated.level());
		assertEquals(Race.HUMAN, updated.race());
		assertEquals(updatedScores, updated.abilityScores());
		assertEquals(CharacterClass.MONK, updated.characterClass());
		assertEquals(CharacterVisibility.PUBLIC, updated.characterVisibility());
		assertEquals(1, updated.skills().size());

		Skills savedUpdatedSkill = updated.skills().getFirst();
		assertNotNull(savedUpdatedSkill.id());
		assertNotEquals(oldSkillId, savedUpdatedSkill.id());
		assertEquals("Shield Bash", savedUpdatedSkill.name());
		assertEquals("Deals additional shield damage", savedUpdatedSkill.description());
		assertEquals(UsagePolicy.ONE_DAY, savedUpdatedSkill.usagePolicy());
		DamageBonus updatedEffect = assertInstanceOf(
				DamageBonus.class,
				savedUpdatedSkill.effects().getFirst());
		assertEquals(7, updatedEffect.amount());

		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM skills WHERE id = ?",
				Integer.class,
				oldSkillId));
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM skill_effects WHERE id = ?",
				Integer.class,
				oldEffectId));
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM skills WHERE character_id = ?",
				Integer.class,
				updated.id()));
		assertEquals(1, jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM skill_effects effects
				JOIN skills ON skills.id = effects.skill_id
				WHERE skills.character_id = ?
				""",
				Integer.class,
				updated.id()));
	}

	@Test
	void findByOwnerIdReturnsOnlyOwnerCharacters() {
		Long firstOwnerId = insertUser("first-owner@example.com");
		Long secondOwnerId = insertUser("second-owner@example.com");

		Character first = saveCharacter(
				firstOwnerId, "First hero", Race.ELF,
				CharacterClass.WIZARD, CharacterVisibility.PUBLIC);
		Character second = saveCharacter(
				firstOwnerId, "Second hero", Race.HUMAN,
				CharacterClass.FIGHTER, CharacterVisibility.PRIVATE);
		saveCharacter(
				secondOwnerId, "Foreign hero", Race.GNOME,
				CharacterClass.ROGUE, CharacterVisibility.PUBLIC);

		entityManager.clear();

		assertCharacterIds(
				adapter.findByOwnerId(firstOwnerId),
				first.id(), second.id());
	}

	@Test
	void findAllReturnsAllCharacters() {
		Long ownerId = insertUser("find-all@example.com");

		Character first = saveCharacter(
				ownerId, "First character", Race.ELF,
				CharacterClass.WIZARD, CharacterVisibility.PUBLIC);
		Character second = saveCharacter(
				ownerId, "Second character", Race.DWARF,
				CharacterClass.FIGHTER, CharacterVisibility.PRIVATE);

		entityManager.clear();

		assertCharacterIds(adapter.findAll(), first.id(), second.id());
	}

	@Test
	void filtersByClassRaceAndVisibility() {
		Long ownerId = insertUser("filters@example.com");

		Character publicWizard = saveCharacter(
				ownerId, "Public wizard", Race.ELF,
				CharacterClass.WIZARD, CharacterVisibility.PUBLIC);
		Character privateWizard = saveCharacter(
				ownerId, "Private wizard", Race.ELF,
				CharacterClass.WIZARD, CharacterVisibility.PRIVATE);
		Character publicFighter = saveCharacter(
				ownerId, "Public fighter", Race.GNOME,
				CharacterClass.FIGHTER, CharacterVisibility.PUBLIC);

		entityManager.clear();

		assertCharacterIds(
				adapter.findCharacterByClassAndVisibility(
						CharacterClass.WIZARD, CharacterVisibility.PUBLIC),
				publicWizard.id());
		assertCharacterIds(
				adapter.findCharacterByRaceAndVisibility(
						Race.ELF, CharacterVisibility.PUBLIC),
				publicWizard.id());
		assertCharacterIds(
				adapter.findCharacterByVisibility(CharacterVisibility.PUBLIC),
				publicWizard.id(), publicFighter.id());
		assertCharacterIds(
				adapter.findCharacterByVisibility(CharacterVisibility.PRIVATE),
				privateWizard.id());
	}

	@Test
	void findsPublicCharacterByPartialNameIgnoringCase() {
		Long ownerId = insertUser("name-search@example.com");

		Character publicCharacter = saveCharacter(
				ownerId, "Silver Flame", Race.ELF,
				CharacterClass.WIZARD, CharacterVisibility.PUBLIC);
		saveCharacter(
				ownerId, "Silver Shadow", Race.DROW,
				CharacterClass.ROGUE, CharacterVisibility.PRIVATE);
		saveCharacter(
				ownerId, "Golden Flame", Race.HUMAN,
				CharacterClass.MONK, CharacterVisibility.PUBLIC);

		entityManager.clear();

		assertCharacterIds(
				adapter.findCharacterByNameAndVisibility(
						"sIlVeR", CharacterVisibility.PUBLIC),
				publicCharacter.id());
	}

	private Long insertUser(String email) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		return jdbcTemplate.queryForObject(
				"""
						INSERT INTO users (
						    username,
						    email,
						    role,
						    created_at,
						    updated_at,
						    email_verified_at
						)
						VALUES (?, ?, ?, ?, ?, ?)
						RETURNING id
						""",
				Long.class,
				"character-test-user",
				email,
				UserRole.USER.name(),
				now,
				now,
				now);
	}

	private Character saveCharacter(
			Long ownerId,
			String name,
			Race race,
			CharacterClass characterClass,
			CharacterVisibility visibility) {
		Skills skill = Skills.create(
				"Skill for " + name,
				"Test skill",
				ActivationType.PASSIVE,
				List.of(new DamageBonus(1)),
				UsagePolicy.NONE);

		return adapter.save(Character.create(
				name,
				"Description for " + name,
				ownerId,
				race,
				AbilityScores.create(10, 10, 10, 10, 10, 10),
				List.of(skill),
				characterClass,
				visibility));
	}

	private void assertCharacterIds(List<Character> actual, Long... expectedIds) {
		Set<Long> actualIds = actual.stream()
				.map(Character::id)
				.collect(Collectors.toSet());

		assertEquals(Set.of(expectedIds), actualIds);
	}
}
