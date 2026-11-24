package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignNameException extends IllegalArgumentException {
	public InvalidCampaignNameException() {
		super("Campaign name must not be blank");
	}
}
