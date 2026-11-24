package com.dndcrowler.dnd_backend.application.port.in.query;

import java.time.DayOfWeek;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;

public record CampaignSearchCriteria(
		String name,
		CampaignFormat format,
		Integer characterLevel,
		DayOfWeek dayOfWeek) {
	public CampaignSearchCriteria {
		name = name == null || name.isBlank()
				? null
				: name.trim();
	}
}
