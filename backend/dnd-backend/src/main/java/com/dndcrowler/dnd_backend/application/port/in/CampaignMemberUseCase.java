package com.dndcrowler.dnd_backend.application.port.in;

import java.util.List;

import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

public interface CampaignMemberUseCase {

	List<CampaignMember> getCampaignMembers(
			Long campaignId,
			Long currentUserId);

	List<CampaignMember> getMyMemberships(
			Long currentUserId);

	void removeMember(
			Long campaignId,
			Long memberId,
			Long currentUserId);

	void leaveCampaign(
		Long campaignId,
		Long currentUserId
	);
}
