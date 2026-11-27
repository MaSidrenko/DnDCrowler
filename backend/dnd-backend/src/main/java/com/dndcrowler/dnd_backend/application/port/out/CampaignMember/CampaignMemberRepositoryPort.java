package com.dndcrowler.dnd_backend.application.port.out.CampaignMember;

import java.util.List;
import java.util.Optional;

import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

public interface CampaignMemberRepositoryPort {
	CampaignMember save(CampaignMember member);

	boolean existsByCampaignIdAndUserId(
			Long campaignId,
			Long userId);

	long countByCampaignId(Long campaignId);

	Optional<CampaignMember> findById(Long memberId);

	List<CampaignMember> findByCampaignId(Long campaignId);

	List<CampaignMember> findByUserId(Long userId);

	Optional<CampaignMember> findByCampaignIdAndUserId(
			Long campaignId,
			Long userId);
	
	void  deleteById(Long memberId);
}
