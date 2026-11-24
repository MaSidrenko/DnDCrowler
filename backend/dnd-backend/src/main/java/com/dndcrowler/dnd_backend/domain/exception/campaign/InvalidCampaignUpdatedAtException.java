package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignUpdatedAtException extends IllegalArgumentException {
	public InvalidCampaignUpdatedAtException() {
		super("Campaign update time is required");
	}
}
