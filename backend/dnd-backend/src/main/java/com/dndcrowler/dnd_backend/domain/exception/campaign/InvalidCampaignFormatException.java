package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignFormatException extends IllegalArgumentException {
	public InvalidCampaignFormatException() {
		super("Campaign format is required");
	}
}
