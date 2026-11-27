package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignApplicationNotFoundException extends RuntimeException {
	public CampaignApplicationNotFoundException(Long applicationId) {
		super("Campaign application not found: id=" + applicationId);
	}
}
