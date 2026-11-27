package com.dndcrowler.dnd_backend.adapter.in.web.campaignMember.DTO.Response;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

public record CampaignMemberResponse(
	Long id,
	Long campaignId,
	Long userId,
	Long characterId,
	Instant joinedAt
) {
	public static CampaignMemberResponse from(CampaignMember member) {
		return new CampaignMemberResponse(
			member.id(), 
			member.campaignId(), 
			member.userId(), 
			member.characterId(), 
			member.joinedAt()
		);
	}
}
