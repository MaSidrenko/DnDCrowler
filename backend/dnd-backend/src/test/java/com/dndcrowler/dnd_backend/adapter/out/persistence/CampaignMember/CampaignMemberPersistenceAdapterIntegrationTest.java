package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;
import com.dndcrowler.dnd_backend.support.CampaignApplicationPostgresTestSupport;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignMemberPersistenceAdapterIntegrationTest
		extends CampaignApplicationPostgresTestSupport {
	private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");

	private final CampaignMemberPersistenceAdapter adapter;
	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;

	@Autowired
	CampaignMemberPersistenceAdapterIntegrationTest(
			CampaignMemberPersistenceAdapter adapter,
			JdbcTemplate jdbcTemplate,
			EntityManager entityManager) {
		this.adapter = adapter;
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	@Test
	void savesAndFindsMemberThroughEveryLookup() {
		Fixture fixture = fixture("mapping");
		CampaignMember member = new CampaignMember(
				null,
				fixture.campaignId(),
				fixture.userId(),
				fixture.characterId(),
				TEST_TIME);

		CampaignMember saved = adapter.save(member);
		entityManager.clear();

		assertNotNull(saved.id());
		assertEquals(saved, adapter.findById(saved.id()).orElseThrow());
		assertEquals(Set.of(saved.id()), ids(adapter.findByCampaignId(fixture.campaignId())));
		assertEquals(Set.of(saved.id()), ids(adapter.findByUserId(fixture.userId())));
		assertEquals(saved, adapter.findByCampaignIdAndUserId(
				fixture.campaignId(), fixture.userId()).orElseThrow());
		assertTrue(adapter.existsByCampaignIdAndUserId(
				fixture.campaignId(), fixture.userId()));
		assertEquals(1L, adapter.countByCampaignId(fixture.campaignId()));
	}

	@Test
	void filtersMembersByCampaignAndUser() {
		Fixture first = fixture("filter-first");
		Fixture second = fixture("filter-second");
		CampaignMember firstMember = adapter.save(member(first));
		CampaignMember secondMember = adapter.save(member(second));
		entityManager.clear();

		assertEquals(Set.of(firstMember.id()), ids(
				adapter.findByCampaignId(first.campaignId())));
		assertEquals(Set.of(secondMember.id()), ids(
				adapter.findByUserId(second.userId())));
		assertFalse(adapter.findByCampaignIdAndUserId(
				first.campaignId(), second.userId()).isPresent());
	}

	@Test
	void deletesMemberById() {
		Fixture fixture = fixture("delete");
		CampaignMember saved = adapter.save(member(fixture));

		adapter.deleteById(saved.id());
		entityManager.flush();
		entityManager.clear();

		assertFalse(adapter.findById(saved.id()).isPresent());
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM campaign_members WHERE id = ?",
				Integer.class,
				saved.id()));
	}

	private CampaignMember member(Fixture fixture) {
		return new CampaignMember(
				null,
				fixture.campaignId(),
				fixture.userId(),
				fixture.characterId(),
				TEST_TIME);
	}

	private Set<Long> ids(java.util.List<CampaignMember> members) {
		return members.stream()
				.map(CampaignMember::id)
				.collect(Collectors.toSet());
	}

	private Fixture fixture(String suffix) {
		Long gameMasterId = insertUser(suffix + "-gm@example.com");
		Long userId = insertUser(suffix + "-member@example.com");
		Long characterId = insertCharacter(userId, suffix + " character");
		Long campaignId = insertCampaign(gameMasterId, suffix + " campaign");
		return new Fixture(userId, characterId, campaignId);
	}

	private Long insertUser(String email) {
		OffsetDateTime time = OffsetDateTime.ofInstant(TEST_TIME, ZoneOffset.UTC);
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO users (username, email, role, created_at, updated_at, email_verified_at)
				VALUES (?, ?, 'USER', ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				email.substring(0, email.indexOf('@')),
				email,
				time,
				time,
				time);
	}

	private Long insertCharacter(Long ownerId, String name) {
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO characters
				  (name, description, owner_id, level, race,
				   strength, dexterity, constitution, intelligence, wisdom, charisma,
				   character_class, visibility)
				VALUES (?, 'Test character', ?, 5, 'HUMAN',
				        10, 10, 10, 10, 10, 10, 'FIGHTER', 'PRIVATE')
				RETURNING id
				""",
				Long.class,
				name,
				ownerId);
	}

	private Long insertCampaign(Long gameMasterId, String name) {
		OffsetDateTime time = OffsetDateTime.ofInstant(TEST_TIME, ZoneOffset.UTC);
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO campaigns
				  (game_master_id, name, description, status, format,
				   max_player_count, min_level, max_level,
				   schedule_day_of_week, schedule_start_time, schedule_time_zone,
				   requirements, created_at, updated_at)
				VALUES (?, ?, 'Test campaign', 'RECRUITING', 'ONLINE',
				        4, 1, 10, 'SATURDAY', '19:00:00', 'Europe/Moscow',
				        'Be respectful', ?, ?)
				RETURNING id
				""",
				Long.class,
				gameMasterId,
				name,
				time,
				time);
	}

	private record Fixture(
			Long userId,
			Long characterId,
			Long campaignId) {
	}
}
