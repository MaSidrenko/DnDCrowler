package com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Request;

import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.CreateCampaignCommand;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCampaignRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank String description,
		@NotNull CampaignFormat format,
		@Min(1) int maxPlayerCount,
		@Min(1) @Max(20) int minLevel,
		@Min(1) @Max(20) int maxLevel,
		@Valid @NotNull CampaignScheduleRequest schedule,
		@NotNull String requirements) {
	public CreateCampaignCommand toCommand(Long currentUserId) {
		return new CreateCampaignCommand(
				currentUserId, name, description, format,
				maxPlayerCount, minLevel, maxLevel,
				schedule.toDomain(), requirements);
	}
}
