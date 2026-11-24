package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignRequirementsException extends IllegalArgumentException {
	public InvalidCampaignRequirementsException() {
		super("Campaign requirements must not be null");
	}
}
