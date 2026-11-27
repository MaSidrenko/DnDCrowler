package com.dndcrowler.dnd_backend.domain.model.CampaignApplication;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationApplicantIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationCampaignIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationCharacterIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationStatusException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationStatusTransitionException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationUpdatedAtBeforeCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationUpdatedAtException;

class CampaignApplicationTest {
	private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void createsPendingApplication() {
		CampaignApplication application = CampaignApplication.create(
				10L, 20L, 30L, "I would like to join");

		assertAll(
				() -> assertNull(application.id()),
				() -> assertEquals(10L, application.campaignId()),
				() -> assertEquals(20L, application.applicantId()),
				() -> assertEquals(30L, application.characterId()),
				() -> assertEquals(CampaignApplicationStatus.PENDING, application.status()),
				() -> assertEquals("I would like to join", application.message()),
				() -> assertEquals(application.createdAt(), application.updatedAt()));
	}

	@ParameterizedTest
	@EnumSource(ApplicationAction.class)
	void changesPendingApplicationStatus(ApplicationAction action) {
		CampaignApplication pending = application(CampaignApplicationStatus.PENDING);

		CampaignApplication result = action.apply(pending);

		assertAll(
				() -> assertEquals(action.targetStatus, result.status()),
				() -> assertEquals(pending.id(), result.id()),
				() -> assertEquals(pending.campaignId(), result.campaignId()),
				() -> assertEquals(pending.applicantId(), result.applicantId()),
				() -> assertEquals(pending.characterId(), result.characterId()),
				() -> assertEquals(pending.createdAt(), result.createdAt()),
				() -> assertTrue(!result.updatedAt().isBefore(pending.updatedAt())));
	}

	@ParameterizedTest
	@EnumSource(value = CampaignApplicationStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
	void rejectsTransitionFromFinishedApplication(CampaignApplicationStatus status) {
		CampaignApplication application = application(status);

		assertAll(
				() -> assertThrows(InvalidCampaignApplicationStatusTransitionException.class,
						application::accept),
				() -> assertThrows(InvalidCampaignApplicationStatusTransitionException.class,
						application::reject),
				() -> assertThrows(InvalidCampaignApplicationStatusTransitionException.class,
						application::withdraw));
	}

	@Test
	void validatesRequiredFieldsAndTimestampOrder() {
		assertAll(
				() -> assertThrows(InvalidCampaignApplicationCampaignIdException.class,
						() -> new CampaignApplication(1L, null, 2L, 3L,
								CampaignApplicationStatus.PENDING, null, CREATED_AT, CREATED_AT)),
				() -> assertThrows(InvalidCampaignApplicationApplicantIdException.class,
						() -> new CampaignApplication(1L, 1L, 0L, 3L,
								CampaignApplicationStatus.PENDING, null, CREATED_AT, CREATED_AT)),
				() -> assertThrows(InvalidCampaignApplicationCharacterIdException.class,
						() -> new CampaignApplication(1L, 1L, 2L, -1L,
								CampaignApplicationStatus.PENDING, null, CREATED_AT, CREATED_AT)),
				() -> assertThrows(InvalidCampaignApplicationStatusException.class,
						() -> new CampaignApplication(1L, 1L, 2L, 3L,
								null, null, CREATED_AT, CREATED_AT)),
				() -> assertThrows(InvalidCampaignApplicationCreatedAtException.class,
						() -> new CampaignApplication(1L, 1L, 2L, 3L,
								CampaignApplicationStatus.PENDING, null, null, CREATED_AT)),
				() -> assertThrows(InvalidCampaignApplicationUpdatedAtException.class,
						() -> new CampaignApplication(1L, 1L, 2L, 3L,
								CampaignApplicationStatus.PENDING, null, CREATED_AT, null)),
				() -> assertThrows(InvalidCampaignApplicationUpdatedAtBeforeCreatedAtException.class,
						() -> new CampaignApplication(1L, 1L, 2L, 3L,
								CampaignApplicationStatus.PENDING, null, CREATED_AT,
								CREATED_AT.minusSeconds(1))));
	}

	private CampaignApplication application(CampaignApplicationStatus status) {
		return new CampaignApplication(
				100L, 10L, 20L, 30L, status, "message", CREATED_AT, CREATED_AT);
	}

	private enum ApplicationAction {
		ACCEPT(CampaignApplicationStatus.ACCEPTED) {
			@Override
			CampaignApplication apply(CampaignApplication application) {
				return application.accept();
			}
		},
		REJECT(CampaignApplicationStatus.REJECTED) {
			@Override
			CampaignApplication apply(CampaignApplication application) {
				return application.reject();
			}
		},
		WITHDRAW(CampaignApplicationStatus.WITHDRAWN) {
			@Override
			CampaignApplication apply(CampaignApplication application) {
				return application.withdraw();
			}
		};

		private final CampaignApplicationStatus targetStatus;

		ApplicationAction(CampaignApplicationStatus targetStatus) {
			this.targetStatus = targetStatus;
		}

		abstract CampaignApplication apply(CampaignApplication application);
	}
}
