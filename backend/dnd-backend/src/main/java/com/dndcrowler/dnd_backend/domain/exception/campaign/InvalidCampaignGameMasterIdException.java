package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignGameMasterIdException extends IllegalArgumentException {
	public InvalidCampaignGameMasterIdException() {
		super("Campaign game master ID must be positive");
	}
}
