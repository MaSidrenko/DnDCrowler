package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignFullException extends RuntimeException {
	public CampaignFullException(Long campaignId) {
		super("Campaign has no available player slots: id=" + campaignId);
	}
}
