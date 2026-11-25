package com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Request;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;

import jakarta.validation.constraints.NotNull;

public record CampaignScheduleRequest(
	@NotNull DayOfWeek dayOfWeek,
	@NotNull LocalTime startTime,
	@NotNull ZoneId timeZone
) {
	public CampaignSchedule toDomain() {
		return new CampaignSchedule(dayOfWeek, startTime, timeZone);
	}
}
