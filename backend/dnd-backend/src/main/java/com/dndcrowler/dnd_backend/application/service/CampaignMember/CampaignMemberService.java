package com.dndcrowler.dnd_backend.application.service.CampaignMember;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dndcrowler.dnd_backend.application.port.in.CampaignMemberUseCase;
import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.CampaignMember.CampaignMemberRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignMemberNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignRosterChangeNotAllowedException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

@Service
public class CampaignMemberService implements CampaignMemberUseCase {
	private final CampaignMemberRepositoryPort memberRepository;
	private final CampaignRepositoryPort campaignRepository;

	public CampaignMemberService(CampaignMemberRepositoryPort memberRepository,
			CampaignRepositoryPort campaignRepository) {
		this.memberRepository = memberRepository;
		this.campaignRepository = campaignRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignMember> getCampaignMembers(Long campaignId, Long currentUserId) {
		Campaign campaign = campaignRepository.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
		if (!campaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}

		return memberRepository.findByCampaignId(campaign.id());
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignMember> getMyMemberships(Long currentUserId) {
		return memberRepository.findByUserId(currentUserId);
	}

	@Override
	@Transactional
	public void removeMember(Long campaignId, Long memberId, Long currentUserId) {
		Campaign removablePlayerCampaign = campaignRepository
				.findByIdForUpdate(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));

		if (!removablePlayerCampaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}

		if (removablePlayerCampaign.status() != CampaignStatus.RECRUITING &&
				removablePlayerCampaign.status() != CampaignStatus.FULL) {
			throw new CampaignRosterChangeNotAllowedException(
					campaignId,
					removablePlayerCampaign.status());
		}

		CampaignMember member = memberRepository
				.findById(memberId)
				.orElseThrow(() -> new CampaignMemberNotFoundException(memberId));

		if (!member.campaignId().equals(campaignId)) {
			throw new CampaignMemberNotFoundException(memberId);
		}

		memberRepository.deleteById(member.id());

		if (removablePlayerCampaign.status() == CampaignStatus.FULL) {
			campaignRepository.save(removablePlayerCampaign.reopenRecruiting());
		}
	}

	@Override
	@Transactional
	public void leaveCampaign(Long campaignId, Long currentUserId) {
		Campaign campaign = campaignRepository
				.findByIdForUpdate(campaignId)
				.orElseThrow(
						() -> new CampaignNotFoundException(campaignId));

		if (campaign.status() != CampaignStatus.RECRUITING &&
				campaign.status() != CampaignStatus.FULL) {
			throw new CampaignRosterChangeNotAllowedException(
					campaignId,
					campaign.status());
		}

		CampaignMember member = memberRepository
				.findByCampaignIdAndUserId(campaignId, currentUserId)
				.orElseThrow(() -> new CampaignMemberNotFoundException(
						campaignId,
						currentUserId));

		memberRepository.deleteById(member.id());

		if (campaign.status() == CampaignStatus.FULL) {
			campaignRepository.save(campaign.reopenRecruiting());
		}
	}

}
