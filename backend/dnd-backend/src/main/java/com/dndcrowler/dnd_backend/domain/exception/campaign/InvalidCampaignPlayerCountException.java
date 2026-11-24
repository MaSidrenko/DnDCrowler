package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignPlayerCountException extends IllegalArgumentException {
	public InvalidCampaignPlayerCountException() {
		super("Campaign maximum player count must be at least 1");
	}
}
