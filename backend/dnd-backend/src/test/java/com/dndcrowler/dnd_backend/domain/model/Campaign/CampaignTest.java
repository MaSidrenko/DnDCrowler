package com.dndcrowler.dnd_backend.domain.model.Campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignDescriptionException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignFormatException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignGameMasterIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignLevelRangeException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignNameException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignPlayerCountException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignRequirementsException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignScheduleException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusTransitionException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignUpdatedAtBeforeCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignUpdatedAtException;

public class CampaignTest {
	@Test
	void createsCampaignAsDraft() {
		Campaign campaign = Campaign.create(
				10L,
				"Curse of Strahd",
				"Gothic adventure",
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(), "");

		assertNull(campaign.id());
		assertEquals(CampaignStatus.DRAFT, campaign.status());
		assertEquals(campaign.createdAt(), campaign.updatedAt());
	}

	@Test
	void updateCampaignWithSaveIdAndOwnerAndStatusAndCreatedAt() {
		Instant now = Instant.now();
		Campaign campaign = new Campaign(
				1L,
				10L,
				"Curse of Strahd",
				"Gothic adventure",
				CampaignStatus.RECRUITING,
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(), "",
				now, now);
		Campaign newData = campaign.update("New name",
				"new desc",
				CampaignFormat.HYBRID,
				6, 2, 12,
				schedule(), "");

		assertNotEquals(campaign.name(), newData.name());
		assertNotEquals(campaign.description(), newData.description());
		assertNotEquals(campaign.format(), newData.format());
		assertEquals(campaign.status(), newData.status());
		assertNotEquals(campaign.maxPlayerCount(), newData.maxPlayerCount());
		assertNotEquals(campaign.minLevel(), newData.minLevel());
		assertNotEquals(campaign.maxLevel(), newData.maxLevel());
		assertEquals(campaign.id(), newData.id());
		assertEquals(campaign.gameMasterId(), newData.gameMasterId());
		assertEquals(campaign.createdAt(), newData.createdAt());
	}

	@Test
	void toPublish() {
		Campaign campaign = Campaign.create(
				10L,
				"Curse of Strahd",
				"Gothic adventure",
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(), "");
		Campaign publish = campaign.publish();

		assertEquals(CampaignStatus.RECRUITING, publish.status());
		assertNotEquals(campaign.status(), publish.status());
	}

	@Test
	void toInProgress() {
		Campaign campaign = Campaign.create(
				10L,
				"Curse of Strahd",
				"Gothic adventure",
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(), "");
		Campaign publish = campaign.publish();

		assertEquals(CampaignStatus.RECRUITING, publish.status());
		assertNotEquals(campaign.status(), publish.status());

		Campaign inProgress = publish.start();

		assertEquals(CampaignStatus.IN_PROGRESS, inProgress.status());
		assertNotEquals(inProgress.status(), publish.status());
	}

	@Test
	void startsFullCampaign() {
		Campaign inProgress = campaignWithStatus(CampaignStatus.FULL).start();

		assertEquals(CampaignStatus.IN_PROGRESS, inProgress.status());
	}

	@Test
	void toComplete() {
		Campaign campaign = Campaign.create(
				10L,
				"Curse of Strahd",
				"Gothic adventure",
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(), "");
		Campaign publish = campaign.publish();

		assertEquals(CampaignStatus.RECRUITING, publish.status());
		assertNotEquals(campaign.status(), publish.status());

		Campaign inProgress = publish.start();

		assertEquals(CampaignStatus.IN_PROGRESS, inProgress.status());
		assertNotEquals(inProgress.status(), publish.status());

		Campaign completed = inProgress.complete();

		assertEquals(CampaignStatus.COMPLETED, completed.status());
		assertNotEquals(inProgress.status(), completed.status());
	}

	@ParameterizedTest
	@EnumSource(
			value = CampaignStatus.class,
			names = {"DRAFT", "RECRUITING", "FULL", "IN_PROGRESS"})
	void cancelsUnfinishedCampaign(CampaignStatus status) {
		Campaign cancelled = campaignWithStatus(status).cancel();

		assertEquals(CampaignStatus.CANCELLED, cancelled.status());
	}

	@Test
	void rejectsInvalidStatusTransitions() {
		assertAll(
				() -> assertThrows(
						InvalidCampaignStatusTransitionException.class,
						() -> campaignWithStatus(CampaignStatus.RECRUITING).publish()),
				() -> assertThrows(
						InvalidCampaignStatusTransitionException.class,
						() -> campaignWithStatus(CampaignStatus.DRAFT).start()),
				() -> assertThrows(
						InvalidCampaignStatusTransitionException.class,
						() -> campaignWithStatus(CampaignStatus.RECRUITING).complete()),
				() -> assertThrows(
						InvalidCampaignStatusTransitionException.class,
						() -> campaignWithStatus(CampaignStatus.COMPLETED).cancel()),
				() -> assertThrows(
						InvalidCampaignStatusTransitionException.class,
						() -> campaignWithStatus(CampaignStatus.CANCELLED).cancel()));
	}

	@Test
	void rejectsInvalidCampaignData() {
		assertAll(
				() -> assertThrows(
						InvalidCampaignNameException.class,
						() -> createCampaign(" ", "Description", 5, 1, 10, schedule())),
				() -> assertThrows(
						InvalidCampaignDescriptionException.class,
						() -> createCampaign("Campaign", " ", 5, 1, 10, schedule())),
				() -> assertThrows(
						InvalidCampaignPlayerCountException.class,
						() -> createCampaign("Campaign", "Description", 0, 1, 10, schedule())),
				() -> assertThrows(
						InvalidCampaignLevelRangeException.class,
						() -> createCampaign("Campaign", "Description", 5, 0, 10, schedule())),
				() -> assertThrows(
						InvalidCampaignLevelRangeException.class,
						() -> createCampaign("Campaign", "Description", 5, 1, 21, schedule())),
				() -> assertThrows(
						InvalidCampaignLevelRangeException.class,
						() -> createCampaign("Campaign", "Description", 5, 10, 5, schedule())),
				() -> assertThrows(
						InvalidCampaignScheduleException.class,
						() -> createCampaign("Campaign", "Description", 5, 1, 10, null)),
				() -> assertThrows(
						InvalidCampaignScheduleException.class,
						() -> new CampaignSchedule(
								null,
								LocalTime.NOON,
								ZoneId.of("Europe/Moscow"))),
				() -> assertThrows(
						InvalidCampaignScheduleException.class,
						() -> new CampaignSchedule(
								DayOfWeek.FRIDAY,
								null,
								ZoneId.of("Europe/Moscow"))),
				() -> assertThrows(
						InvalidCampaignScheduleException.class,
						() -> new CampaignSchedule(
								DayOfWeek.FRIDAY,
								LocalTime.NOON,
								null)));
	}

	@Test
	void rejectsMissingMetadataAndInvalidTimestamps() {
		Instant createdAt = Instant.parse("2026-01-02T00:00:00Z");
		Instant earlier = createdAt.minusSeconds(1);

		assertAll(
				() -> assertThrows(
						InvalidCampaignGameMasterIdException.class,
						() -> Campaign.create(
								0L, "Campaign", "Description", CampaignFormat.ONLINE,
								5, 1, 10, schedule(), "")),
				() -> assertThrows(
						InvalidCampaignFormatException.class,
						() -> Campaign.create(
								10L, "Campaign", "Description", null,
								5, 1, 10, schedule(), "")),
				() -> assertThrows(
						InvalidCampaignRequirementsException.class,
						() -> Campaign.create(
								10L, "Campaign", "Description", CampaignFormat.ONLINE,
								5, 1, 10, schedule(), null)),
				() -> assertThrows(
						InvalidCampaignStatusException.class,
						() -> campaignWithMetadata(null, createdAt, createdAt)),
				() -> assertThrows(
						InvalidCampaignCreatedAtException.class,
						() -> campaignWithMetadata(CampaignStatus.DRAFT, null, createdAt)),
				() -> assertThrows(
						InvalidCampaignUpdatedAtException.class,
						() -> campaignWithMetadata(CampaignStatus.DRAFT, createdAt, null)),
				() -> assertThrows(
						InvalidCampaignUpdatedAtBeforeCreatedAtException.class,
						() -> campaignWithMetadata(CampaignStatus.DRAFT, createdAt, earlier)));
	}

	private Campaign campaignWithMetadata(
			CampaignStatus status,
			Instant createdAt,
			Instant updatedAt) {
		return new Campaign(
				1L,
				10L,
				"Campaign",
				"Description",
				status,
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(),
				"",
				createdAt,
				updatedAt);
	}

	private Campaign campaignWithStatus(CampaignStatus status) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new Campaign(
				1L,
				10L,
				"Curse of Strahd",
				"Gothic adventure",
				status,
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(),
				"",
				now,
				now);
	}

	private Campaign createCampaign(
			String name,
			String description,
			int maxPlayerCount,
			int minLevel,
			int maxLevel,
			CampaignSchedule schedule) {
		return Campaign.create(
				10L,
				name,
				description,
				CampaignFormat.ONLINE,
				maxPlayerCount,
				minLevel,
				maxLevel,
				schedule,
				"");
	}

	private CampaignSchedule schedule() {
		return new CampaignSchedule(
				DayOfWeek.FRIDAY,
				LocalTime.NOON,
				ZoneId.of("Europe/Moscow"));
	}
}
