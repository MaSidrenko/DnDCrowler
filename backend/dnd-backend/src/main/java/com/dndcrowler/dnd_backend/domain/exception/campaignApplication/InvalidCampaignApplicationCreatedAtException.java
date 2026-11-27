package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationCreatedAtException extends IllegalArgumentException {
	public InvalidCampaignApplicationCreatedAtException() {
		super("Campaign application creation time is required");
	}
}
