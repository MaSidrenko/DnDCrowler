package com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication.DTO.Request;

import com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication.SubmitCampaignApplicationCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitCampaignApplicationRequest(
	@NotNull 
	@Positive 
	Long characterId,
	@Size(max = 1000)
	String message
) {
	public SubmitCampaignApplicationCommand toCommand(
		Long currentUserId,
		Long campaignId
	) {
		return new SubmitCampaignApplicationCommand(
			currentUserId,
			campaignId,
			characterId,
			message
		);
	}
}
