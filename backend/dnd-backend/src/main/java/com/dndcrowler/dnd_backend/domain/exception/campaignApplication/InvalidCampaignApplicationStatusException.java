package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationStatusException extends IllegalArgumentException {
	public InvalidCampaignApplicationStatusException() {
		super("Campaign application status is required");
	}
}
