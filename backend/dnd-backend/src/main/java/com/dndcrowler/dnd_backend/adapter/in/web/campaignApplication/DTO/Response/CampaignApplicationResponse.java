package com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication.DTO.Response;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;

public record CampaignApplicationResponse(
		Long id,
		Long campaignId,
		Long applicantId,
		Long characterId,
		CampaignApplicationStatus status,
		String message,
		Instant createdAt,
		Instant updatedAt) {
	public static CampaignApplicationResponse from(
			CampaignApplication application) {
		return new CampaignApplicationResponse(
				application.id(),
				application.campaignId(),
				application.applicantId(),
				application.characterId(),
				application.status(),
				application.message(),
				application.createdAt(),
				application.updatedAt());
	}
}
