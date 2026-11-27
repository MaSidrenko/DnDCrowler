package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationCharacterIdException extends IllegalArgumentException {
	public InvalidCampaignApplicationCharacterIdException() {
		super("Campaign application character id must be positive");
	}
}
