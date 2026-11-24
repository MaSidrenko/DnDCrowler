package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignDescriptionException extends IllegalArgumentException {
	public InvalidCampaignDescriptionException() {
		super("Campaign description must not be blank");
	}
}
