package com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication;

public record SubmitCampaignApplicationCommand(
		Long currentUserId,
		Long campaignId,
		Long characterId,
		String message) {

}
