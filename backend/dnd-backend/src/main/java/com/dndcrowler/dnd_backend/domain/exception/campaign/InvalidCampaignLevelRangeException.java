package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignLevelRangeException extends IllegalArgumentException {
	public InvalidCampaignLevelRangeException() {
		super("Campaign level range must be between 1 and 20, with minimum not greater than maximum");
	}
}
