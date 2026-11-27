package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember.CampaignMemberPersistenceAdapter;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;
import com.dndcrowler.dnd_backend.support.CampaignApplicationPostgresTestSupport;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignApplicationPersistenceAdapterIntegrationTest
		extends CampaignApplicationPostgresTestSupport {
	private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");

	private final CampaignApplicationPersistenceAdapter applicationAdapter;
	private final CampaignMemberPersistenceAdapter memberAdapter;
	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;

	@Autowired
	CampaignApplicationPersistenceAdapterIntegrationTest(
			CampaignApplicationPersistenceAdapter applicationAdapter,
			CampaignMemberPersistenceAdapter memberAdapter,
			JdbcTemplate jdbcTemplate,
			EntityManager entityManager) {
		this.applicationAdapter = applicationAdapter;
		this.memberAdapter = memberAdapter;
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	@Test
	void savesAndLoadsAllApplicationFields() {
		Fixture fixture = fixture("mapping", 4);
		CampaignApplication application = new CampaignApplication(
				null,
				fixture.campaignId(),
				fixture.applicantId(),
				fixture.characterId(),
				CampaignApplicationStatus.PENDING,
				"I would like to join",
				TEST_TIME,
				TEST_TIME);

		CampaignApplication saved = applicationAdapter.save(application);
		entityManager.clear();
		CampaignApplication loaded = applicationAdapter.findById(saved.id()).orElseThrow();

		assertNotNull(saved.id());
		assertEquals(saved, loaded);
		assertTrue(applicationAdapter.findByIdForUpdate(saved.id()).isPresent());
	}

	@Test
	void updatesExistingApplicationWithoutCreatingAnotherRow() {
		Fixture fixture = fixture("update", 4);
		CampaignApplication saved = applicationAdapter.save(application(fixture, TEST_TIME));
		CampaignApplication accepted = saved.accept();

		CampaignApplication updated = applicationAdapter.save(accepted);
		entityManager.clear();
		CampaignApplication loaded = applicationAdapter.findById(saved.id()).orElseThrow();

		assertEquals(saved.id(), updated.id());
		assertEquals(CampaignApplicationStatus.ACCEPTED, loaded.status());
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM campaign_applications WHERE id = ?",
				Integer.class,
				saved.id()));
	}

	@Test
	void reportsOnlyPendingApplicationAsActive() {
		Fixture fixture = fixture("active", 4);
		CampaignApplication saved = applicationAdapter.save(application(fixture, TEST_TIME));

		assertTrue(applicationAdapter.existsActiveByCampaignIdAndApplicantId(
				fixture.campaignId(), fixture.applicantId()));

		applicationAdapter.save(saved.reject());

		assertFalse(applicationAdapter.existsActiveByCampaignIdAndApplicantId(
				fixture.campaignId(), fixture.applicantId()));
	}

	@Test
	void listsApplicationsNewestFirstAndFiltersByOwner() {
		Fixture fixture = fixture("listing", 4);
		Long secondApplicant = insertUser("listing-second@example.com");
		Long secondCharacter = insertCharacter(secondApplicant, "Second character", 5);

		CampaignApplication older = applicationAdapter.save(application(fixture, TEST_TIME));
		CampaignApplication newer = applicationAdapter.save(new CampaignApplication(
				null,
				fixture.campaignId(),
				secondApplicant,
				secondCharacter,
				CampaignApplicationStatus.PENDING,
				"newer",
				TEST_TIME.plusSeconds(60),
				TEST_TIME.plusSeconds(60)));

		List<CampaignApplication> campaignApplications =
				applicationAdapter.findByCampaignId(fixture.campaignId());

		assertEquals(List.of(newer.id(), older.id()),
				campaignApplications.stream().map(CampaignApplication::id).toList());
		assertEquals(List.of(older.id()),
				applicationAdapter.findByApplicantId(fixture.applicantId())
						.stream().map(CampaignApplication::id).toList());
	}

	@Test
	void databaseRejectsSecondPendingApplicationForSameUserAndCampaign() {
		Fixture fixture = fixture("duplicate", 4);
		applicationAdapter.save(application(fixture, TEST_TIME));

		CampaignApplication duplicate = application(fixture, TEST_TIME.plusSeconds(1));

		assertThrows(DataIntegrityViolationException.class,
				() -> applicationAdapter.save(duplicate));
	}

	@Test
	void savesMemberAndReportsCampaignCapacity() {
		Fixture fixture = fixture("member", 4);

		CampaignMember saved = memberAdapter.save(CampaignMember.create(
				fixture.campaignId(), fixture.applicantId(), fixture.characterId()));

		assertNotNull(saved.id());
		assertTrue(memberAdapter.existsByCampaignIdAndUserId(
				fixture.campaignId(), fixture.applicantId()));
		assertEquals(1L, memberAdapter.countByCampaignId(fixture.campaignId()));
	}

	@Test
	void databaseRejectsDuplicateCampaignMember() {
		Fixture fixture = fixture("duplicate-member", 4);
		memberAdapter.save(CampaignMember.create(
				fixture.campaignId(), fixture.applicantId(), fixture.characterId()));

		CampaignMember duplicate = CampaignMember.create(
				fixture.campaignId(), fixture.applicantId(), fixture.characterId());

		assertThrows(DataIntegrityViolationException.class,
				() -> memberAdapter.save(duplicate));
	}

	@Test
	void deletingCampaignCascadesApplicationsAndMembers() {
		Fixture fixture = fixture("cascade", 4);
		CampaignApplication application = applicationAdapter.save(application(fixture, TEST_TIME));
		CampaignMember member = memberAdapter.save(CampaignMember.create(
				fixture.campaignId(), fixture.applicantId(), fixture.characterId()));
		entityManager.clear();

		jdbcTemplate.update("DELETE FROM campaigns WHERE id = ?", fixture.campaignId());

		assertEquals(0, count("campaign_applications", application.id()));
		assertEquals(0, count("campaign_members", member.id()));
	}

	private CampaignApplication application(Fixture fixture, Instant createdAt) {
		return new CampaignApplication(
				null,
				fixture.campaignId(),
				fixture.applicantId(),
				fixture.characterId(),
				CampaignApplicationStatus.PENDING,
				"Application message",
				createdAt,
				createdAt);
	}

	private Fixture fixture(String suffix, int maxPlayers) {
		Long gameMasterId = insertUser(suffix + "-gm@example.com");
		Long applicantId = insertUser(suffix + "-applicant@example.com");
		Long characterId = insertCharacter(applicantId, suffix + " character", 5);
		Long campaignId = insertCampaign(gameMasterId, suffix + " campaign", maxPlayers);
		return new Fixture(gameMasterId, applicantId, characterId, campaignId);
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

	private Long insertCharacter(Long ownerId, String name, int level) {
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO characters
				  (name, description, owner_id, level, race,
				   strength, dexterity, constitution, intelligence, wisdom, charisma,
				   character_class, visibility)
				VALUES (?, 'Test character', ?, ?, 'HUMAN',
				        10, 10, 10, 10, 10, 10, 'FIGHTER', 'PRIVATE')
				RETURNING id
				""",
				Long.class,
				name,
				ownerId,
				level);
	}

	private Long insertCampaign(Long gameMasterId, String name, int maxPlayers) {
		OffsetDateTime time = OffsetDateTime.ofInstant(TEST_TIME, ZoneOffset.UTC);
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO campaigns
				  (game_master_id, name, description, status, format,
				   max_player_count, min_level, max_level,
				   schedule_day_of_week, schedule_start_time, schedule_time_zone,
				   requirements, created_at, updated_at)
				VALUES (?, ?, 'Test campaign', 'RECRUITING', 'ONLINE',
				        ?, 1, 10, 'SATURDAY', '19:00:00', 'Europe/Moscow',
				        'Be respectful', ?, ?)
				RETURNING id
				""",
				Long.class,
				gameMasterId,
				name,
				maxPlayers,
				time,
				time);
	}

	private int count(String table, Long id) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE id = ?",
				Integer.class,
				id);
	}

	private record Fixture(
			Long gameMasterId,
			Long applicantId,
			Long characterId,
			Long campaignId) {
	}
}
