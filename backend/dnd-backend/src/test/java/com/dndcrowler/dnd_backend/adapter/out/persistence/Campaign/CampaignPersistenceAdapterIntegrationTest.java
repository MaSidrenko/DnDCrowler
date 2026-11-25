package com.dndcrowler.dnd_backend.adapter.out.persistence.Campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignPersistenceAdapterIntegrationTest {
	private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final CampaignPersistenceAdapter adapter;
	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;

	@Autowired
	CampaignPersistenceAdapterIntegrationTest(
			CampaignPersistenceAdapter adapter,
			JdbcTemplate jdbcTemplate,
			EntityManager entityManager) {
		this.adapter = adapter;
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	@Test
	void savesAndLoadsAllCampaignFields() {
		Long gameMasterId = insertUser("campaign-mapping@example.com");
		Campaign campaign = campaign(
				gameMasterId,
				"Curse of Strahd",
				CampaignStatus.DRAFT,
				CampaignFormat.HYBRID,
				3,
				12,
				DayOfWeek.FRIDAY);

		Campaign saved = adapter.save(campaign);
		entityManager.clear();
		Campaign loaded = adapter.findById(saved.id()).orElseThrow();

		assertNotNull(saved.id());
		assertEquals(gameMasterId, loaded.gameMasterId());
		assertEquals(campaign.name(), loaded.name());
		assertEquals(campaign.description(), loaded.description());
		assertEquals(campaign.status(), loaded.status());
		assertEquals(campaign.format(), loaded.format());
		assertEquals(campaign.maxPlayerCount(), loaded.maxPlayerCount());
		assertEquals(campaign.minLevel(), loaded.minLevel());
		assertEquals(campaign.maxLevel(), loaded.maxLevel());
		assertEquals(campaign.schedule(), loaded.schedule());
		assertEquals(campaign.requirements(), loaded.requirements());
		assertEquals(TEST_TIME, loaded.createdAt());
		assertEquals(TEST_TIME, loaded.updatedAt());

		Map<String, Object> storedSchedule = jdbcTemplate.queryForMap(
				"""
				SELECT schedule_day_of_week, schedule_time_zone
				FROM campaigns
				WHERE id = ?
				""",
				saved.id());
		assertEquals("FRIDAY", storedSchedule.get("schedule_day_of_week"));
		assertEquals("Europe/Moscow", storedSchedule.get("schedule_time_zone"));
	}

	@Test
	void updatesExistingCampaignWithoutCreatingAnotherRow() {
		Long gameMasterId = insertUser("campaign-update@example.com");
		Campaign saved = adapter.save(campaign(
				gameMasterId,
				"Original campaign",
				CampaignStatus.DRAFT,
				CampaignFormat.ONLINE,
				1,
				5,
				DayOfWeek.FRIDAY));

		Campaign changed = saved.update(
				"Updated campaign",
				"Updated description",
				CampaignFormat.OFFLINE,
				7,
				4,
				15,
				new CampaignSchedule(
						DayOfWeek.SATURDAY,
						LocalTime.of(18, 30),
						ZoneId.of("UTC")),
				"Updated requirements");

		Campaign updated = adapter.save(changed);
		entityManager.clear();
		Campaign loaded = adapter.findById(saved.id()).orElseThrow();

		assertEquals(saved.id(), updated.id());
		assertEquals("Updated campaign", loaded.name());
		assertEquals(CampaignFormat.OFFLINE, loaded.format());
		assertEquals(DayOfWeek.SATURDAY, loaded.schedule().dayOfWeek());
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM campaigns WHERE id = ?",
				Integer.class,
				saved.id()));
	}

	@Test
	void findsCampaignsOnlyForRequestedGameMaster() {
		Long firstOwner = insertUser("campaign-owner-one@example.com");
		Long secondOwner = insertUser("campaign-owner-two@example.com");
		adapter.save(campaign(firstOwner, "First owned", CampaignStatus.DRAFT,
				CampaignFormat.ONLINE, 1, 5, DayOfWeek.FRIDAY));
		adapter.save(campaign(firstOwner, "Second owned", CampaignStatus.RECRUITING,
				CampaignFormat.HYBRID, 3, 8, DayOfWeek.SATURDAY));
		adapter.save(campaign(secondOwner, "Foreign", CampaignStatus.DRAFT,
				CampaignFormat.OFFLINE, 1, 20, DayOfWeek.SUNDAY));

		List<Campaign> result = adapter.findByGameMasterId(firstOwner);

		assertEquals(Set.of("First owned", "Second owned"), names(result));
		assertTrue(result.stream().allMatch(c -> c.gameMasterId().equals(firstOwner)));
	}

	@ParameterizedTest
	@EnumSource(
			value = CampaignStatus.class,
			names = {"RECRUITING", "FULL", "IN_PROGRESS", "COMPLETED"})
	void returnsPubliclyVisibleCampaign(CampaignStatus status) {
		Long gameMasterId = insertUser("public-" + status.name().toLowerCase() + "@example.com");
		Campaign saved = adapter.save(campaign(
				gameMasterId,
				"Public " + status,
				status,
				CampaignFormat.ONLINE,
				1,
				10,
				DayOfWeek.FRIDAY));

		assertTrue(adapter.findPubliclyVisibleById(saved.id()).isPresent());
	}

	@ParameterizedTest
	@EnumSource(value = CampaignStatus.class, names = {"DRAFT", "CANCELLED"})
	void hidesNonPublicCampaign(CampaignStatus status) {
		Long gameMasterId = insertUser("hidden-" + status.name().toLowerCase() + "@example.com");
		Campaign saved = adapter.save(campaign(
				gameMasterId,
				"Hidden " + status,
				status,
				CampaignFormat.ONLINE,
				1,
				10,
				DayOfWeek.FRIDAY));

		assertTrue(adapter.findPubliclyVisibleById(saved.id()).isEmpty());
		assertTrue(adapter.findById(saved.id()).isPresent());
	}

	@Test
	void searchAlwaysReturnsOnlyRecruitingCampaigns() {
		Long gameMasterId = insertUser("campaign-status-search@example.com");
		adapter.save(campaign(gameMasterId, "Recruiting", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Draft", CampaignStatus.DRAFT,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Started", CampaignStatus.IN_PROGRESS,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));

		List<Campaign> result = adapter.searchRecruiting(
				new CampaignSearchCriteria(null, null, null, null));

		assertEquals(List.of("Recruiting"), result.stream().map(Campaign::name).toList());
	}

	@Test
	void searchesByPartialNameIgnoringCase() {
		Long gameMasterId = insertUser("campaign-name-search@example.com");
		adapter.save(campaign(gameMasterId, "Curse of Strahd", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Lost Mine", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));

		List<Campaign> result = adapter.searchRecruiting(
				new CampaignSearchCriteria("  STRAHD  ", null, null, null));

		assertEquals(List.of("Curse of Strahd"), result.stream().map(Campaign::name).toList());
	}

	@Test
	void searchesByFormat() {
		Long gameMasterId = insertUser("campaign-format-search@example.com");
		adapter.save(campaign(gameMasterId, "Online", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Offline", CampaignStatus.RECRUITING,
				CampaignFormat.OFFLINE, 1, 10, DayOfWeek.FRIDAY));

		List<Campaign> result = adapter.searchRecruiting(
				new CampaignSearchCriteria(null, CampaignFormat.OFFLINE, null, null));

		assertEquals(List.of("Offline"), result.stream().map(Campaign::name).toList());
	}

	@Test
	void searchesByInclusiveCharacterLevelRange() {
		Long gameMasterId = insertUser("campaign-level-search@example.com");
		adapter.save(campaign(gameMasterId, "Low levels", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 5, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "High levels", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 6, 12, DayOfWeek.FRIDAY));

		List<Campaign> atLowerBoundary = adapter.searchRecruiting(
				new CampaignSearchCriteria(null, null, 1, null));
		List<Campaign> atUpperBoundary = adapter.searchRecruiting(
				new CampaignSearchCriteria(null, null, 5, null));

		assertEquals(List.of("Low levels"),
				atLowerBoundary.stream().map(Campaign::name).toList());
		assertEquals(List.of("Low levels"),
				atUpperBoundary.stream().map(Campaign::name).toList());
	}

	@Test
	void searchesByScheduleDay() {
		Long gameMasterId = insertUser("campaign-day-search@example.com");
		adapter.save(campaign(gameMasterId, "Friday game", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Sunday game", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 1, 10, DayOfWeek.SUNDAY));

		List<Campaign> result = adapter.searchRecruiting(
				new CampaignSearchCriteria(null, null, null, DayOfWeek.SUNDAY));

		assertEquals(List.of("Sunday game"), result.stream().map(Campaign::name).toList());
	}

	@Test
	void combinesAllRecruitingSearchFilters() {
		Long gameMasterId = insertUser("campaign-combined-search@example.com");
		adapter.save(campaign(gameMasterId, "Strahd online", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 3, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Strahd offline", CampaignStatus.RECRUITING,
				CampaignFormat.OFFLINE, 3, 10, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Strahd high level", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 11, 20, DayOfWeek.FRIDAY));
		adapter.save(campaign(gameMasterId, "Strahd sunday", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 3, 10, DayOfWeek.SUNDAY));
		adapter.save(campaign(gameMasterId, "Other online", CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE, 3, 10, DayOfWeek.FRIDAY));

		List<Campaign> result = adapter.searchRecruiting(
				new CampaignSearchCriteria(
						"strahd",
						CampaignFormat.ONLINE,
						5,
						DayOfWeek.FRIDAY));

		assertEquals(List.of("Strahd online"), result.stream().map(Campaign::name).toList());
	}

	@Test
	void findByIdReturnsEmptyForUnknownCampaign() {
		assertFalse(adapter.findById(Long.MAX_VALUE).isPresent());
	}

	@Test
	void rejectsCampaignWithoutExistingGameMaster() {
		Campaign orphan = campaign(
				Long.MAX_VALUE,
				"Orphan campaign",
				CampaignStatus.DRAFT,
				CampaignFormat.ONLINE,
				1,
				10,
				DayOfWeek.FRIDAY);

		org.junit.jupiter.api.Assertions.assertThrows(
				DataIntegrityViolationException.class,
				() -> adapter.save(orphan));
	}

	@Test
	void deletesCampaignWhenGameMasterIsDeleted() {
		Long gameMasterId = insertUser("campaign-cascade@example.com");
		Campaign saved = adapter.save(campaign(
				gameMasterId,
				"Cascade campaign",
				CampaignStatus.DRAFT,
				CampaignFormat.ONLINE,
				1,
				10,
				DayOfWeek.FRIDAY));
		entityManager.clear();

		assertEquals(1, jdbcTemplate.update(
				"DELETE FROM users WHERE id = ?",
				gameMasterId));
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM campaigns WHERE id = ?",
				Integer.class,
				saved.id()));
	}

	private Campaign campaign(
			Long gameMasterId,
			String name,
			CampaignStatus status,
			CampaignFormat format,
			int minLevel,
			int maxLevel,
			DayOfWeek dayOfWeek) {
		return new Campaign(
				null,
				gameMasterId,
				name,
				"Campaign description",
				status,
				format,
				6,
				minLevel,
				maxLevel,
				new CampaignSchedule(
						dayOfWeek,
						LocalTime.of(19, 30),
						ZoneId.of("Europe/Moscow")),
				"Campaign requirements",
				TEST_TIME,
				TEST_TIME);
	}

	private Set<String> names(List<Campaign> campaigns) {
		return campaigns.stream()
				.map(Campaign::name)
				.collect(Collectors.toSet());
	}

	private Long insertUser(String email) {
		OffsetDateTime now = OffsetDateTime.ofInstant(TEST_TIME, ZoneOffset.UTC);
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO users (username, email, role, created_at, updated_at, email_verified_at)
				VALUES (?, ?, ?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				"game-master",
				email,
				"USER",
				now,
				now,
				now);
	}
}
