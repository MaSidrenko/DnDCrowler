package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignUpdatedAtBeforeCreatedAtException extends IllegalArgumentException {
	public InvalidCampaignUpdatedAtBeforeCreatedAtException() {
		super("Campaign update time must not be before creation time");
	}
}
