package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignCreatedAtException extends IllegalArgumentException {
	public InvalidCampaignCreatedAtException() {
		super("Campaign creation time is required");
	}
}
