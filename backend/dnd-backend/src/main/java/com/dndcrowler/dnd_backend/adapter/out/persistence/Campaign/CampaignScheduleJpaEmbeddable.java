package com.dndcrowler.dnd_backend.adapter.out.persistence.Campaign;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class CampaignScheduleJpaEmbeddable {
	@Enumerated(EnumType.STRING)
	@Column(name = "schedule_day_of_week", nullable = false, length = 20)
	private DayOfWeek dayOfWeek;

	@Column(name = "schedule_start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "schedule_time_zone", nullable = false, length = 100)
	private String timeZone;

	protected CampaignScheduleJpaEmbeddable() {
	}

	public CampaignScheduleJpaEmbeddable(CampaignSchedule schedule) {
		this.dayOfWeek = schedule.dayOfWeek();
		this.startTime = schedule.startTime();
		this.timeZone = schedule.timeZone().getId();
	}

	public CampaignSchedule toDomain() {
		return new CampaignSchedule(
				dayOfWeek,
				startTime,
				ZoneId.of(timeZone));
	}
}
