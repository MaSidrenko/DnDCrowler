package com.dndcrowler.dnd_backend.application.port.in.command.Campaign;


import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;

public record UpdateCampaignCommand(
		Long currentUserId,
		String name,
		String description,
		CampaignFormat format,
		int maxPlayerCount,
		int minLevel,
		int maxLevel,
		CampaignSchedule schedule,
		String requirements) {
}
