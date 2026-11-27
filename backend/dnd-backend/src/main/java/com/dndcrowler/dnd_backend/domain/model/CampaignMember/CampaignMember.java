package com.dndcrowler.dnd_backend.domain.model.CampaignMember;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberCampaignIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberCharacterIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberJoinedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberUserIdException;

public record CampaignMember(
		Long id,
		Long campaignId,
		Long userId,
		Long characterId,
		Instant joinedAt) {
	public CampaignMember {
		if (campaignId == null || campaignId <= 0) {
			throw new InvalidCampaignMemberCampaignIdException();
		}

		if (userId == null || userId <= 0) {
			throw new InvalidCampaignMemberUserIdException();
		}

		if (characterId == null || characterId <= 0) {
			throw new InvalidCampaignMemberCharacterIdException();
		}

		if (joinedAt == null) {
			throw new InvalidCampaignMemberJoinedAtException();
		}
	}

	public static CampaignMember create(
			Long campaignId,
			Long userId,
			Long characterId) {
		return new CampaignMember(
				null,
				campaignId,
				userId,
				characterId,
				Instant.now());
	}
}
