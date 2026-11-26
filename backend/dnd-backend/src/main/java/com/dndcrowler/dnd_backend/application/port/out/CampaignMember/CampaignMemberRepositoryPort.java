package com.dndcrowler.dnd_backend.application.port.out.CampaignMember;

import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

public interface CampaignMemberRepositoryPort {
	CampaignMember save(CampaignMember member);

	boolean existsByCampaignIdAndUserId(
			Long campaignId,
			Long userId);

	Long countByCampaignId(Long campaignId);
}
