package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationUpdatedAtBeforeCreatedAtException extends IllegalArgumentException {
	public InvalidCampaignApplicationUpdatedAtBeforeCreatedAtException() {
		super("Campaign application update time must not be before creation time");
	}
}
