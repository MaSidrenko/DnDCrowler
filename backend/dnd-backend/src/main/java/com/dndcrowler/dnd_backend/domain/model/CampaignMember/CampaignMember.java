package com.dndcrowler.dnd_backend.domain.model.CampaignMember;

import java.time.Instant;

public record CampaignMember(
	Long id,
	Long campaignId,
	Long userId,
	Long characterId,
	Instant joinedAt
) {
	public CampaignMember {
		if(campaignId == null) {
			throw new IllegalArgumentException();
		}

		if(userId == null) {
			throw new IllegalArgumentException();
		}

		if(characterId == null) {
			throw new IllegalArgumentException();
		}

		if(joinedAt == null) {
			throw new IllegalArgumentException();
		}
	}
}
