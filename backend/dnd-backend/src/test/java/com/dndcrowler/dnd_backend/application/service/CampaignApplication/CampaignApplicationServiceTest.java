package com.dndcrowler.dnd_backend.application.service.CampaignApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication.SubmitCampaignApplicationCommand;
import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.CampaignApplication.CampaignApplicationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.CampaignMember.CampaignMemberRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Character.CharacterRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationAlreadyExistsException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignFullException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignMemberAlreadyExistsException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotRecruitingException;
import com.dndcrowler.dnd_backend.application.service.exception.CannotApplyToOwnCampaignException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterLevelNotAllowedException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;
import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

@ExtendWith(MockitoExtension.class)
class CampaignApplicationServiceTest {
	private static final Long CAMPAIGN_ID = 100L;
	private static final Long APPLICATION_ID = 200L;
	private static final Long GAME_MASTER_ID = 10L;
	private static final Long APPLICANT_ID = 20L;
	private static final Long CHARACTER_ID = 30L;
	private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	CampaignApplicationRepositoryPort applicationRepository;

	@Mock
	CampaignMemberRepositoryPort memberRepository;

	@Mock
	CampaignRepositoryPort campaignRepository;

	@Mock
	CharacterRepositoryPort characterRepository;

	@InjectMocks
	CampaignApplicationService service;

	@Test
	void submitsApplicationForOwnedCharacter() {
		SubmitCampaignApplicationCommand command = command();
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));
		when(characterRepository.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character(APPLICANT_ID, 5)));
		when(applicationRepository.existsActiveByCampaignIdAndApplicantId(
				CAMPAIGN_ID, APPLICANT_ID)).thenReturn(false);
		when(memberRepository.existsByCampaignIdAndUserId(CAMPAIGN_ID, APPLICANT_ID))
				.thenReturn(false);
		when(applicationRepository.save(any(CampaignApplication.class)))
				.thenAnswer(invocation -> withApplicationId(invocation.getArgument(0)));

		CampaignApplication result = service.submit(command);

		assertEquals(APPLICATION_ID, result.id());
		assertEquals(CampaignApplicationStatus.PENDING, result.status());
		ArgumentCaptor<CampaignApplication> captor =
				ArgumentCaptor.forClass(CampaignApplication.class);
		verify(applicationRepository).save(captor.capture());
		assertEquals(CAMPAIGN_ID, captor.getValue().campaignId());
		assertEquals(APPLICANT_ID, captor.getValue().applicantId());
		assertEquals(CHARACTER_ID, captor.getValue().characterId());
		assertEquals("Let me join", captor.getValue().message());
	}

	@Test
	void rejectsSubmissionWhenCampaignDoesNotExist() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class, () -> service.submit(command()));

		verifyNoInteractions(characterRepository, applicationRepository, memberRepository);
	}

	@Test
	void rejectsSubmissionToCampaignThatIsNotRecruiting() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.DRAFT, 4)));

		assertThrows(CampaignNotRecruitingException.class, () -> service.submit(command()));

		verifyNoInteractions(characterRepository, applicationRepository, memberRepository);
	}

	@Test
	void rejectsSubmissionToFullCampaign() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.FULL, 4)));

		assertThrows(CampaignFullException.class, () -> service.submit(command()));
	}

	@Test
	void gameMasterCannotApplyToOwnCampaign() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));

		SubmitCampaignApplicationCommand command = new SubmitCampaignApplicationCommand(
				GAME_MASTER_ID, CAMPAIGN_ID, CHARACTER_ID, null);

		assertThrows(CannotApplyToOwnCampaignException.class,
				() -> service.submit(command));
		verifyNoInteractions(characterRepository, applicationRepository, memberRepository);
	}

	@Test
	void rejectsCharacterOwnedByAnotherUser() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));
		when(characterRepository.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character(999L, 5)));

		assertThrows(CharacterAccessDeniedException.class, () -> service.submit(command()));
		verify(applicationRepository, never()).save(any());
	}

	@Test
	void rejectsCharacterOutsideCampaignLevelRange() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));
		when(characterRepository.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character(APPLICANT_ID, 11)));

		assertThrows(CharacterLevelNotAllowedException.class, () -> service.submit(command()));
		verify(applicationRepository, never()).save(any());
	}

	@Test
	void rejectsDuplicatePendingApplication() {
		stubValidSubmissionDependencies();
		when(applicationRepository.existsActiveByCampaignIdAndApplicantId(
				CAMPAIGN_ID, APPLICANT_ID)).thenReturn(true);

		assertThrows(CampaignApplicationAlreadyExistsException.class,
				() -> service.submit(command()));

		verify(applicationRepository, never()).save(any());
		verifyNoInteractions(memberRepository);
	}

	@Test
	void rejectsApplicationFromExistingMember() {
		stubValidSubmissionDependencies();
		when(applicationRepository.existsActiveByCampaignIdAndApplicantId(
				CAMPAIGN_ID, APPLICANT_ID)).thenReturn(false);
		when(memberRepository.existsByCampaignIdAndUserId(CAMPAIGN_ID, APPLICANT_ID))
				.thenReturn(true);

		assertThrows(CampaignMemberAlreadyExistsException.class,
				() -> service.submit(command()));

		verify(applicationRepository, never()).save(any());
	}

	@Test
	void acceptsApplicationAndMarksCampaignFullWhenLastPlaceIsTaken() {
		Campaign campaign = campaign(CampaignStatus.RECRUITING, 2);
		CampaignApplication pending = application(CampaignApplicationStatus.PENDING);
		stubAcceptance(pending, campaign, 1L);
		when(applicationRepository.save(any(CampaignApplication.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(memberRepository.save(any(CampaignMember.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CampaignApplication result = service.accept(APPLICATION_ID, GAME_MASTER_ID);

		assertEquals(CampaignApplicationStatus.ACCEPTED, result.status());
		ArgumentCaptor<CampaignMember> memberCaptor =
				ArgumentCaptor.forClass(CampaignMember.class);
		verify(memberRepository).save(memberCaptor.capture());
		assertEquals(CAMPAIGN_ID, memberCaptor.getValue().campaignId());
		assertEquals(APPLICANT_ID, memberCaptor.getValue().userId());
		assertEquals(CHARACTER_ID, memberCaptor.getValue().characterId());
		ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
		verify(campaignRepository).save(campaignCaptor.capture());
		assertEquals(CampaignStatus.FULL, campaignCaptor.getValue().status());
	}

	@Test
	void acceptsApplicationWithoutMarkingCampaignFullWhenPlacesRemain() {
		Campaign campaign = campaign(CampaignStatus.RECRUITING, 4);
		stubAcceptance(application(CampaignApplicationStatus.PENDING), campaign, 1L);
		when(applicationRepository.save(any(CampaignApplication.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CampaignApplication result = service.accept(APPLICATION_ID, GAME_MASTER_ID);

		assertEquals(CampaignApplicationStatus.ACCEPTED, result.status());
		verify(campaignRepository, never()).save(any());
	}

	@Test
	void rejectsAcceptanceByUserWhoIsNotGameMaster() {
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.of(application(CampaignApplicationStatus.PENDING)));
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));

		assertThrows(CampaignAccessDeniedException.class,
				() -> service.accept(APPLICATION_ID, APPLICANT_ID));

		verifyNoInteractions(memberRepository, characterRepository);
	}

	@Test
	void rejectsAcceptanceWhenCampaignHasNoFreePlaces() {
		Campaign campaign = campaign(CampaignStatus.RECRUITING, 2);
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.of(application(CampaignApplicationStatus.PENDING)));
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign));
		when(memberRepository.existsByCampaignIdAndUserId(CAMPAIGN_ID, APPLICANT_ID))
				.thenReturn(false);
		when(memberRepository.countByCampaignId(CAMPAIGN_ID)).thenReturn(2L);

		assertThrows(CampaignFullException.class,
				() -> service.accept(APPLICATION_ID, GAME_MASTER_ID));

		verifyNoInteractions(characterRepository);
		verify(applicationRepository, never()).save(any());
	}

	@Test
	void rejectsMissingApplicationForStateChangingOperations() {
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignApplicationNotFoundException.class,
				() -> service.accept(APPLICATION_ID, GAME_MASTER_ID));
		assertThrows(CampaignApplicationNotFoundException.class,
				() -> service.reject(APPLICATION_ID, GAME_MASTER_ID));
		assertThrows(CampaignApplicationNotFoundException.class,
				() -> service.withdraw(APPLICATION_ID, APPLICANT_ID));
	}

	@Test
	void gameMasterRejectsPendingApplication() {
		CampaignApplication pending = application(CampaignApplicationStatus.PENDING);
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.of(pending));
		when(campaignRepository.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));
		when(applicationRepository.save(any(CampaignApplication.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CampaignApplication result = service.reject(APPLICATION_ID, GAME_MASTER_ID);

		assertEquals(CampaignApplicationStatus.REJECTED, result.status());
		verify(applicationRepository).save(result);
	}

	@Test
	void applicantWithdrawsPendingApplication() {
		CampaignApplication pending = application(CampaignApplicationStatus.PENDING);
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.of(pending));
		when(applicationRepository.save(any(CampaignApplication.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CampaignApplication result = service.withdraw(APPLICATION_ID, APPLICANT_ID);

		assertEquals(CampaignApplicationStatus.WITHDRAWN, result.status());
		verify(applicationRepository).save(result);
	}

	@Test
	void anotherUserCannotWithdrawApplication() {
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.of(application(CampaignApplicationStatus.PENDING)));

		assertThrows(CampaignApplicationAccessDeniedException.class,
				() -> service.withdraw(APPLICATION_ID, 999L));

		verify(applicationRepository, never()).save(any());
	}

	@Test
	void returnsApplicationsForCurrentApplicant() {
		List<CampaignApplication> expected = List.of(application(CampaignApplicationStatus.PENDING));
		when(applicationRepository.findByApplicantId(APPLICANT_ID)).thenReturn(expected);

		List<CampaignApplication> result = service.getMyApplications(APPLICANT_ID);

		assertSame(expected, result);
		verify(applicationRepository).findByApplicantId(APPLICANT_ID);
	}

	@Test
	void returnsCampaignApplicationsOnlyToGameMaster() {
		List<CampaignApplication> expected = List.of(application(CampaignApplicationStatus.PENDING));
		when(campaignRepository.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));
		when(applicationRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(expected);

		assertSame(expected,
				service.getCampaignApplications(CAMPAIGN_ID, GAME_MASTER_ID));
		assertThrows(CampaignAccessDeniedException.class,
				() -> service.getCampaignApplications(CAMPAIGN_ID, APPLICANT_ID));
	}

	private void stubValidSubmissionDependencies() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING, 4)));
		when(characterRepository.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character(APPLICANT_ID, 5)));
	}

	private void stubAcceptance(
			CampaignApplication application,
			Campaign campaign,
			long memberCount) {
		when(applicationRepository.findByIdForUpdate(APPLICATION_ID))
				.thenReturn(Optional.of(application));
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign));
		when(memberRepository.existsByCampaignIdAndUserId(CAMPAIGN_ID, APPLICANT_ID))
				.thenReturn(false);
		when(memberRepository.countByCampaignId(CAMPAIGN_ID)).thenReturn(memberCount);
		when(characterRepository.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character(APPLICANT_ID, 5)));
	}

	private SubmitCampaignApplicationCommand command() {
		return new SubmitCampaignApplicationCommand(
				APPLICANT_ID, CAMPAIGN_ID, CHARACTER_ID, "Let me join");
	}

	private Campaign campaign(CampaignStatus status, int maxPlayers) {
		return new Campaign(
				CAMPAIGN_ID,
				GAME_MASTER_ID,
				"Curse of Strahd",
				"Gothic horror campaign",
				status,
				CampaignFormat.ONLINE,
				maxPlayers,
				3,
				10,
				new CampaignSchedule(
						DayOfWeek.SATURDAY,
						LocalTime.of(19, 0),
						ZoneId.of("Europe/Moscow")),
				"Be respectful",
				TIME,
				TIME);
	}

	private Character character(Long ownerId, int level) {
		return new Character(
				CHARACTER_ID,
				"Aria",
				"Elven wizard",
				ownerId,
				level,
				Race.ELF,
				new AbilityScores(8, 14, 12, 17, 13, 10),
				List.of(),
				CharacterClass.WIZARD,
				CharacterVisibility.PRIVATE);
	}

	private CampaignApplication application(CampaignApplicationStatus status) {
		return new CampaignApplication(
				APPLICATION_ID,
				CAMPAIGN_ID,
				APPLICANT_ID,
				CHARACTER_ID,
				status,
				"Let me join",
				TIME,
				TIME);
	}

	private CampaignApplication withApplicationId(CampaignApplication application) {
		return new CampaignApplication(
				APPLICATION_ID,
				application.campaignId(),
				application.applicantId(),
				application.characterId(),
				application.status(),
				application.message(),
				application.createdAt(),
				application.updatedAt());
	}
}
