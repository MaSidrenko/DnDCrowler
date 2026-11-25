package com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Response;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

public record CampaignResponse(
		Long id,
		Long gameMasterId,
		String name,
		String description,
		CampaignStatus status,
		CampaignFormat format,
		int maxPlayerCount,
		int minLevel,
		int maxLevel,
		CampaignScheduleResponse schedule,
		String requirements,
		Instant createdAt,
		Instant updatedAt) {
	public static CampaignResponse from(Campaign campaign) {
		return new CampaignResponse(
				campaign.id(), campaign.gameMasterId(),
				campaign.name(), campaign.description(),
				campaign.status(), campaign.format(),
				campaign.maxPlayerCount(),
				campaign.minLevel(), campaign.maxLevel(),
				CampaignScheduleResponse.from(campaign.schedule()),
				campaign.requirements(),
				campaign.createdAt(), campaign.updatedAt());
	}
}
