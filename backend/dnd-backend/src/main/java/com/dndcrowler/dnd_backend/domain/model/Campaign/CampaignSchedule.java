package com.dndcrowler.dnd_backend.domain.model.Campaign;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;

import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignScheduleException;

public record CampaignSchedule(
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		ZoneId timeZone) {

	public CampaignSchedule {
		if (dayOfWeek == null || startTime == null || timeZone == null) {
			throw new InvalidCampaignScheduleException();
		}
	}
}
