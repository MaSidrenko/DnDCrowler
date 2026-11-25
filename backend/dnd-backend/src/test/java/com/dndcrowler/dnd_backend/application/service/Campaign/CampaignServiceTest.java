package com.dndcrowler.dnd_backend.application.service.Campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.CreateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.UpdateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusTransitionException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
	private static final Long OWNER_ID = 10L;
	private static final Long OTHER_USER_ID = 20L;
	private static final Long CAMPAIGN_ID = 100L;
	private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	CampaignRepositoryPort campaignRepositoryPort;

	@Mock
	UserRepositoryPort userRepositoryPort;

	@InjectMocks
	CampaignService service;

	@Test
	void createsDraftCampaignForExistingUser() {
		CreateCampaignCommand command = createCommand(OWNER_ID);
		when(userRepositoryPort.findById(OWNER_ID))
				.thenReturn(Optional.of(user(OWNER_ID)));
		when(campaignRepositoryPort.save(any(Campaign.class)))
				.thenAnswer(invocation -> withId(invocation.getArgument(0), CAMPAIGN_ID));

		Campaign result = service.createCampaign(command);

		assertEquals(CAMPAIGN_ID, result.id());
		ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
		verify(campaignRepositoryPort).save(captor.capture());
		Campaign saved = captor.getValue();
		assertNull(saved.id());
		assertEquals(OWNER_ID, saved.gameMasterId());
		assertEquals(CampaignStatus.DRAFT, saved.status());
		assertEquals(command.name(), saved.name());
		assertEquals(command.schedule(), saved.schedule());
	}

	@Test
	void rejectsCreationWhenUserDoesNotExist() {
		when(userRepositoryPort.findById(OWNER_ID)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> service.createCampaign(createCommand(OWNER_ID)));

		verifyNoInteractions(campaignRepositoryPort);
	}

	@Test
	void ownerUpdatesCampaignWithoutChangingIdentityOrStatus() {
		Campaign existing = campaign(CampaignStatus.RECRUITING);
		UpdateCampaignCommand command = updateCommand(OWNER_ID);
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(existing));
		when(campaignRepositoryPort.save(any(Campaign.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Campaign result = service.updateCampaign(CAMPAIGN_ID, command);

		assertEquals(CAMPAIGN_ID, result.id());
		assertEquals(OWNER_ID, result.gameMasterId());
		assertEquals(CampaignStatus.RECRUITING, result.status());
		assertEquals(CREATED_AT, result.createdAt());
		assertEquals(command.name(), result.name());
		assertEquals(command.format(), result.format());
		verify(campaignRepositoryPort).save(result);
	}

	@Test
	void rejectsUpdateByAnotherUser() {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.DRAFT)));

		assertThrows(CampaignAccessDeniedException.class,
				() -> service.updateCampaign(CAMPAIGN_ID, updateCommand(OTHER_USER_ID)));

		verify(campaignRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsUpdateWhenCampaignDoesNotExist() {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class,
				() -> service.updateCampaign(CAMPAIGN_ID, updateCommand(OWNER_ID)));

		verify(campaignRepositoryPort, never()).save(any());
	}

	@ParameterizedTest
	@EnumSource(StatusAction.class)
	void ownerChangesCampaignStatus(StatusAction action) {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(action.initialStatus)));
		when(campaignRepositoryPort.save(any(Campaign.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Campaign result = invoke(action, OWNER_ID);

		assertEquals(action.targetStatus, result.status());
		verify(campaignRepositoryPort).save(result);
	}

	@ParameterizedTest
	@EnumSource(StatusAction.class)
	void rejectsStatusChangeByAnotherUser(StatusAction action) {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(action.initialStatus)));

		assertThrows(CampaignAccessDeniedException.class,
				() -> invoke(action, OTHER_USER_ID));

		verify(campaignRepositoryPort, never()).save(any());
	}

	@ParameterizedTest
	@EnumSource(StatusAction.class)
	void rejectsStatusChangeWhenCampaignDoesNotExist(StatusAction action) {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class,
				() -> invoke(action, OWNER_ID));

		verify(campaignRepositoryPort, never()).save(any());
	}

	@Test
	void doesNotSaveCampaignAfterInvalidStatusTransition() {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING)));

		assertThrows(InvalidCampaignStatusTransitionException.class,
				() -> service.publishCampaign(CAMPAIGN_ID, OWNER_ID));

		verify(campaignRepositoryPort, never()).save(any());
	}

	@Test
	void returnsPubliclyVisibleCampaign() {
		Campaign campaign = campaign(CampaignStatus.RECRUITING);
		when(campaignRepositoryPort.findPubliclyVisibleById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign));

		Campaign result = service.getPublicCampaignById(CAMPAIGN_ID);

		assertSame(campaign, result);
	}

	@Test
	void hidesCampaignThatIsNotPubliclyVisible() {
		when(campaignRepositoryPort.findPubliclyVisibleById(CAMPAIGN_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class,
				() -> service.getPublicCampaignById(CAMPAIGN_ID));
	}

	@Test
	void ownerReadsOwnCampaign() {
		Campaign campaign = campaign(CampaignStatus.DRAFT);
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign));

		Campaign result = service.getMyCampaignById(CAMPAIGN_ID, OWNER_ID);

		assertSame(campaign, result);
	}

	@Test
	void rejectsReadingOwnedCampaignByAnotherUser() {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.DRAFT)));

		assertThrows(CampaignAccessDeniedException.class,
				() -> service.getMyCampaignById(CAMPAIGN_ID, OTHER_USER_ID));
	}

	@Test
	void rejectsReadingOwnedCampaignWhenItDoesNotExist() {
		when(campaignRepositoryPort.findById(CAMPAIGN_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class,
				() -> service.getMyCampaignById(CAMPAIGN_ID, OWNER_ID));
	}

	@Test
	void returnsCampaignsForRequestedGameMaster() {
		List<Campaign> expected = List.of(campaign(CampaignStatus.DRAFT));
		when(campaignRepositoryPort.findByGameMasterId(OWNER_ID)).thenReturn(expected);

		List<Campaign> result = service.getMyCampaigns(OWNER_ID);

		assertSame(expected, result);
		verify(campaignRepositoryPort).findByGameMasterId(OWNER_ID);
	}

	@Test
	void delegatesRecruitingSearchCriteria() {
		CampaignSearchCriteria criteria = new CampaignSearchCriteria(
				"curse", CampaignFormat.ONLINE, 5, DayOfWeek.FRIDAY);
		List<Campaign> expected = List.of(campaign(CampaignStatus.RECRUITING));
		when(campaignRepositoryPort.searchRecruiting(criteria)).thenReturn(expected);

		List<Campaign> result = service.searchCampaigns(criteria);

		assertSame(expected, result);
		verify(campaignRepositoryPort).searchRecruiting(criteria);
	}

	private Campaign invoke(StatusAction action, Long currentUserId) {
		return switch (action) {
			case PUBLISH -> service.publishCampaign(CAMPAIGN_ID, currentUserId);
			case START -> service.startCampaign(CAMPAIGN_ID, currentUserId);
			case COMPLETE -> service.completeCampaign(CAMPAIGN_ID, currentUserId);
			case CANCEL -> service.cancelCampaign(CAMPAIGN_ID, currentUserId);
		};
	}

	private CreateCampaignCommand createCommand(Long currentUserId) {
		return new CreateCampaignCommand(
				currentUserId,
				"Curse of Strahd",
				"Gothic horror adventure",
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(),
				"Weekly attendance");
	}

	private UpdateCampaignCommand updateCommand(Long currentUserId) {
		return new UpdateCampaignCommand(
				currentUserId,
				"Updated campaign",
				"Updated description",
				CampaignFormat.HYBRID,
				6,
				3,
				12,
				new CampaignSchedule(
						DayOfWeek.SATURDAY,
						LocalTime.of(18, 30),
						ZoneId.of("UTC")),
				"Updated requirements");
	}

	private Campaign campaign(CampaignStatus status) {
		return new Campaign(
				CAMPAIGN_ID,
				OWNER_ID,
				"Curse of Strahd",
				"Gothic horror adventure",
				status,
				CampaignFormat.ONLINE,
				5,
				1,
				10,
				schedule(),
				"Weekly attendance",
				CREATED_AT,
				CREATED_AT);
	}

	private Campaign withId(Campaign campaign, Long id) {
		return new Campaign(
				id,
				campaign.gameMasterId(),
				campaign.name(),
				campaign.description(),
				campaign.status(),
				campaign.format(),
				campaign.maxPlayerCount(),
				campaign.minLevel(),
				campaign.maxLevel(),
				campaign.schedule(),
				campaign.requirements(),
				campaign.createdAt(),
				campaign.updatedAt());
	}

	private User user(Long id) {
		return new User(
				id,
				"game-master",
				"gm@example.com",
				UserRole.USER,
				CREATED_AT,
				CREATED_AT,
				CREATED_AT);
	}

	private CampaignSchedule schedule() {
		return new CampaignSchedule(
				DayOfWeek.FRIDAY,
				LocalTime.of(19, 0),
				ZoneId.of("Europe/Moscow"));
	}

	private enum StatusAction {
		PUBLISH(CampaignStatus.DRAFT, CampaignStatus.RECRUITING),
		START(CampaignStatus.RECRUITING, CampaignStatus.IN_PROGRESS),
		COMPLETE(CampaignStatus.IN_PROGRESS, CampaignStatus.COMPLETED),
		CANCEL(CampaignStatus.RECRUITING, CampaignStatus.CANCELLED);

		private final CampaignStatus initialStatus;
		private final CampaignStatus targetStatus;

		StatusAction(CampaignStatus initialStatus, CampaignStatus targetStatus) {
			this.initialStatus = initialStatus;
			this.targetStatus = targetStatus;
		}
	}
}
