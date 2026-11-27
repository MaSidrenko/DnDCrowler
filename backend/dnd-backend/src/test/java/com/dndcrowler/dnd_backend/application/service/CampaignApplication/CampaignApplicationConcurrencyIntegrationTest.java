package com.dndcrowler.dnd_backend.application.service.CampaignApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.dndcrowler.dnd_backend.application.service.exception.CampaignFullException;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.support.CampaignApplicationPostgresTestSupport;

@SpringBootTest
@ActiveProfiles("test")
class CampaignApplicationConcurrencyIntegrationTest
		extends CampaignApplicationPostgresTestSupport {
	private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");

	private final CampaignApplicationService service;
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	CampaignApplicationConcurrencyIntegrationTest(
			CampaignApplicationService service,
			JdbcTemplate jdbcTemplate) {
		this.service = service;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	void onlyOneConcurrentAcceptanceCanTakeLastPlace() throws Exception {
		Long gameMasterId = insertUser("concurrency-gm@example.com");
		Long firstApplicantId = insertUser("concurrency-first@example.com");
		Long secondApplicantId = insertUser("concurrency-second@example.com");
		Long firstCharacterId = insertCharacter(firstApplicantId, "First", 5);
		Long secondCharacterId = insertCharacter(secondApplicantId, "Second", 5);
		Long campaignId = insertCampaign(gameMasterId, 1);
		Long firstApplicationId = insertApplication(
				campaignId, firstApplicantId, firstCharacterId);
		Long secondApplicationId = insertApplication(
				campaignId, secondApplicantId, secondCharacterId);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<Attempt> first = executor.submit(
				() -> acceptAfterSignal(firstApplicationId, gameMasterId, ready, start));
		Future<Attempt> second = executor.submit(
				() -> acceptAfterSignal(secondApplicationId, gameMasterId, ready, start));

		try {
			if (!ready.await(5, TimeUnit.SECONDS)) {
				throw new AssertionError("Concurrent tasks did not become ready in time");
			}
			start.countDown();
			List<Attempt> attempts = List.of(
					first.get(10, TimeUnit.SECONDS),
					second.get(10, TimeUnit.SECONDS));

			assertEquals(1, attempts.stream().filter(Attempt::successful).count());
			RuntimeException failure = attempts.stream()
					.map(Attempt::failure)
					.filter(java.util.Objects::nonNull)
					.findFirst()
					.orElseThrow();
			assertInstanceOf(CampaignFullException.class, failure);
		} finally {
			first.cancel(true);
			second.cancel(true);
			executor.shutdownNow();
		}

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM campaign_members WHERE campaign_id = ?",
				Integer.class,
				campaignId));
		assertEquals("FULL", jdbcTemplate.queryForObject(
				"SELECT status FROM campaigns WHERE id = ?",
				String.class,
				campaignId));
		assertEquals(1, jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*) FROM campaign_applications
				WHERE campaign_id = ? AND status = 'ACCEPTED'
				""",
				Integer.class,
				campaignId));
		assertEquals(1, jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*) FROM campaign_applications
				WHERE campaign_id = ? AND status = 'PENDING'
				""",
				Integer.class,
				campaignId));
	}

	private Attempt acceptAfterSignal(
			Long applicationId,
			Long gameMasterId,
			CountDownLatch ready,
			CountDownLatch start) throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			CampaignApplication accepted = service.accept(applicationId, gameMasterId);
			return new Attempt(accepted, null);
		} catch (RuntimeException exception) {
			return new Attempt(null, exception);
		}
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

	private Long insertCampaign(Long gameMasterId, int maxPlayers) {
		OffsetDateTime time = OffsetDateTime.ofInstant(TEST_TIME, ZoneOffset.UTC);
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO campaigns
				  (game_master_id, name, description, status, format,
				   max_player_count, min_level, max_level,
				   schedule_day_of_week, schedule_start_time, schedule_time_zone,
				   requirements, created_at, updated_at)
				VALUES (?, 'Concurrent campaign', 'Test campaign', 'RECRUITING', 'ONLINE',
				        ?, 1, 10, 'SATURDAY', '19:00:00', 'Europe/Moscow',
				        'Be respectful', ?, ?)
				RETURNING id
				""",
				Long.class,
				gameMasterId,
				maxPlayers,
				time,
				time);
	}

	private Long insertApplication(
			Long campaignId,
			Long applicantId,
			Long characterId) {
		OffsetDateTime time = OffsetDateTime.ofInstant(TEST_TIME, ZoneOffset.UTC);
		return jdbcTemplate.queryForObject(
				"""
				INSERT INTO campaign_applications
				  (campaign_id, applicant_id, character_id, status, message, created_at, updated_at)
				VALUES (?, ?, ?, 'PENDING', 'Join request', ?, ?)
				RETURNING id
				""",
				Long.class,
				campaignId,
				applicantId,
				characterId,
				time,
				time);
	}

	private record Attempt(
			CampaignApplication application,
			RuntimeException failure) {
		boolean successful() {
			return application != null && failure == null;
		}
	}
}
