package com.dndcrowler.dnd_backend.application.service.CampaignMember;

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

import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.CampaignMember.CampaignMemberRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignMemberNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignRosterChangeNotAllowedException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

@ExtendWith(MockitoExtension.class)
class CampaignMemberServiceTest {
	private static final Long CAMPAIGN_ID = 100L;
	private static final Long OTHER_CAMPAIGN_ID = 101L;
	private static final Long MEMBER_ID = 200L;
	private static final Long GAME_MASTER_ID = 10L;
	private static final Long USER_ID = 20L;
	private static final Long CHARACTER_ID = 30L;
	private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	CampaignMemberRepositoryPort memberRepository;

	@Mock
	CampaignRepositoryPort campaignRepository;

	@InjectMocks
	CampaignMemberService service;

	@Test
	void returnsCampaignMembersToGameMaster() {
		List<CampaignMember> members = List.of(member(CAMPAIGN_ID));
		when(campaignRepository.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING)));
		when(memberRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(members);

		List<CampaignMember> result = service.getCampaignMembers(
				CAMPAIGN_ID, GAME_MASTER_ID);

		assertSame(members, result);
		verify(memberRepository).findByCampaignId(CAMPAIGN_ID);
	}

	@Test
	void rejectsCampaignMemberListingWhenCampaignDoesNotExist() {
		when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class,
				() -> service.getCampaignMembers(CAMPAIGN_ID, GAME_MASTER_ID));

		verifyNoInteractions(memberRepository);
	}

	@Test
	void rejectsCampaignMemberListingForUserWhoIsNotGameMaster() {
		when(campaignRepository.findById(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(CampaignStatus.RECRUITING)));

		assertThrows(CampaignAccessDeniedException.class,
				() -> service.getCampaignMembers(CAMPAIGN_ID, USER_ID));

		verifyNoInteractions(memberRepository);
	}

	@Test
	void returnsMembershipsForCurrentUser() {
		List<CampaignMember> memberships = List.of(member(CAMPAIGN_ID));
		when(memberRepository.findByUserId(USER_ID)).thenReturn(memberships);

		List<CampaignMember> result = service.getMyMemberships(USER_ID);

		assertSame(memberships, result);
		verify(memberRepository).findByUserId(USER_ID);
	}

	@Test
	void gameMasterRemovesMemberFromRecruitingCampaign() {
		stubCampaignForUpdate(CampaignStatus.RECRUITING);
		when(memberRepository.findById(MEMBER_ID))
				.thenReturn(Optional.of(member(CAMPAIGN_ID)));

		service.removeMember(CAMPAIGN_ID, MEMBER_ID, GAME_MASTER_ID);

		verify(memberRepository).deleteById(MEMBER_ID);
		verify(campaignRepository, never()).save(any());
	}

	@Test
	void removingMemberReopensFullCampaign() {
		stubCampaignForUpdate(CampaignStatus.FULL);
		when(memberRepository.findById(MEMBER_ID))
				.thenReturn(Optional.of(member(CAMPAIGN_ID)));

		service.removeMember(CAMPAIGN_ID, MEMBER_ID, GAME_MASTER_ID);

		ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
		verify(memberRepository).deleteById(MEMBER_ID);
		verify(campaignRepository).save(campaignCaptor.capture());
		assertEquals(CampaignStatus.RECRUITING, campaignCaptor.getValue().status());
	}

	@Test
	void rejectsMemberRemovalByUserWhoIsNotGameMaster() {
		stubCampaignForUpdate(CampaignStatus.RECRUITING);

		assertThrows(CampaignAccessDeniedException.class,
				() -> service.removeMember(CAMPAIGN_ID, MEMBER_ID, USER_ID));

		verifyNoInteractions(memberRepository);
	}

	@Test
	void rejectsMemberRemovalWhenRosterCannotBeChanged() {
		stubCampaignForUpdate(CampaignStatus.IN_PROGRESS);

		assertThrows(CampaignRosterChangeNotAllowedException.class,
				() -> service.removeMember(
						CAMPAIGN_ID, MEMBER_ID, GAME_MASTER_ID));

		verifyNoInteractions(memberRepository);
	}

	@Test
	void rejectsRemovalWhenMemberDoesNotExist() {
		stubCampaignForUpdate(CampaignStatus.RECRUITING);
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

		assertThrows(CampaignMemberNotFoundException.class,
				() -> service.removeMember(
						CAMPAIGN_ID, MEMBER_ID, GAME_MASTER_ID));

		verify(memberRepository, never()).deleteById(any());
	}

	@Test
	void rejectsRemovalOfMemberFromAnotherCampaign() {
		stubCampaignForUpdate(CampaignStatus.RECRUITING);
		when(memberRepository.findById(MEMBER_ID))
				.thenReturn(Optional.of(member(OTHER_CAMPAIGN_ID)));

		assertThrows(CampaignMemberNotFoundException.class,
				() -> service.removeMember(
						CAMPAIGN_ID, MEMBER_ID, GAME_MASTER_ID));

		verify(memberRepository, never()).deleteById(any());
	}

	@Test
	void rejectsMemberRemovalWhenCampaignDoesNotExist() {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignNotFoundException.class,
				() -> service.removeMember(
						CAMPAIGN_ID, MEMBER_ID, GAME_MASTER_ID));

		verifyNoInteractions(memberRepository);
	}

	@Test
	void memberLeavesRecruitingCampaign() {
		stubCampaignForUpdate(CampaignStatus.RECRUITING);
		when(memberRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID))
				.thenReturn(Optional.of(member(CAMPAIGN_ID)));

		service.leaveCampaign(CAMPAIGN_ID, USER_ID);

		verify(memberRepository).deleteById(MEMBER_ID);
		verify(campaignRepository, never()).save(any());
	}

	@Test
	void leavingMemberReopensFullCampaign() {
		stubCampaignForUpdate(CampaignStatus.FULL);
		when(memberRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID))
				.thenReturn(Optional.of(member(CAMPAIGN_ID)));

		service.leaveCampaign(CAMPAIGN_ID, USER_ID);

		ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
		verify(memberRepository).deleteById(MEMBER_ID);
		verify(campaignRepository).save(campaignCaptor.capture());
		assertEquals(CampaignStatus.RECRUITING, campaignCaptor.getValue().status());
	}

	@Test
	void rejectsLeavingWhenRosterCannotBeChanged() {
		stubCampaignForUpdate(CampaignStatus.COMPLETED);

		assertThrows(CampaignRosterChangeNotAllowedException.class,
				() -> service.leaveCampaign(CAMPAIGN_ID, USER_ID));

		verifyNoInteractions(memberRepository);
	}

	@Test
	void rejectsLeavingWhenUserIsNotCampaignMember() {
		stubCampaignForUpdate(CampaignStatus.RECRUITING);
		when(memberRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID))
				.thenReturn(Optional.empty());

		assertThrows(CampaignMemberNotFoundException.class,
				() -> service.leaveCampaign(CAMPAIGN_ID, USER_ID));

		verify(memberRepository, never()).deleteById(any());
	}

	private void stubCampaignForUpdate(CampaignStatus status) {
		when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID))
				.thenReturn(Optional.of(campaign(status)));
	}

	private Campaign campaign(CampaignStatus status) {
		return new Campaign(
				CAMPAIGN_ID,
				GAME_MASTER_ID,
				"Test campaign",
				"Test campaign description",
				status,
				CampaignFormat.ONLINE,
				4,
				1,
				10,
				new CampaignSchedule(
						DayOfWeek.SATURDAY,
						LocalTime.of(19, 0),
						ZoneId.of("Europe/Moscow")),
				"Be respectful",
				TIME,
				TIME);
	}

	private CampaignMember member(Long campaignId) {
		return new CampaignMember(
				MEMBER_ID,
				campaignId,
				USER_ID,
				CHARACTER_ID,
				TIME);
	}
}
