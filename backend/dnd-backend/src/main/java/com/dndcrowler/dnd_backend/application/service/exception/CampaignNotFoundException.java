package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignNotFoundException extends RuntimeException {
	public CampaignNotFoundException(Long campaignId) {
		super("Campaign not found: id=" + campaignId);
	}
}
