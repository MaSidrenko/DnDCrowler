package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationUpdatedAtException extends IllegalArgumentException {
	public InvalidCampaignApplicationUpdatedAtException() {
		super("Campaign application update time is required");
	}
}
