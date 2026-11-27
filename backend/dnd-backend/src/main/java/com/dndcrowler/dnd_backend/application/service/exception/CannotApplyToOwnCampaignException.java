package com.dndcrowler.dnd_backend.application.service.exception;

public class CannotApplyToOwnCampaignException extends RuntimeException {
	public CannotApplyToOwnCampaignException(Long campaignId) {
		super("The game master cannot apply to their own campaign: id=" + campaignId);
	}
}
