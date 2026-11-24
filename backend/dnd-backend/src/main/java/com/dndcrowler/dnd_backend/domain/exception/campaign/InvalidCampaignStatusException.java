package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignStatusException extends IllegalArgumentException {
	public InvalidCampaignStatusException() {
		super("Campaign status is required");
	}
}
