package com.dndcrowler.dnd_backend.domain.exception.campaign;

public class InvalidCampaignScheduleException extends IllegalArgumentException {
	public InvalidCampaignScheduleException() {
		super("Campaign schedule requires a day, start time and time zone");
	}
}
