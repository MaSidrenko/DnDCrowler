package com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;

public record CampaignScheduleResponse(
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		ZoneId timeZone) {
	public static CampaignScheduleResponse from(CampaignSchedule schedule) {
		return new CampaignScheduleResponse(
				schedule.dayOfWeek(),
				schedule.startTime(),
				schedule.timeZone());
	}
}
