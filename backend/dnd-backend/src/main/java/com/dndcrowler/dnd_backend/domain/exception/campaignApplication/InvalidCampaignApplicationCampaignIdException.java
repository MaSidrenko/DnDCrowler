package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationCampaignIdException extends IllegalArgumentException {
	public InvalidCampaignApplicationCampaignIdException() {
		super("Campaign application campaign id must be positive");
	}
}
