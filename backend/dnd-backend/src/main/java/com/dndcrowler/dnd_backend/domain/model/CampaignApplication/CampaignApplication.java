package com.dndcrowler.dnd_backend.domain.model.CampaignApplication;

import java.time.Instant;

public record CampaignApplication(
		Long id,
		Long campaignId,
		Long applicantId,
		Long characterId,
		CampaignApplicationStatus status,
		String message,
		Instant createdAt,
		Instant updatedAt) {
	public CampaignApplication {
		if (campaignId == null) {
			throw new IllegalArgumentException();
		}

		if (applicantId == null) {
			throw new IllegalArgumentException();
		}

		if (characterId == null) {
			throw new IllegalArgumentException();
		}

		if (status == null) {
			throw new IllegalArgumentException();
		}

		if (createdAt == null) {
			throw new IllegalArgumentException();
		}

		if (updatedAt == null) {
			throw new IllegalArgumentException();
		}
	}

	public CampaignApplication accept() {
		if (status != CampaignApplicationStatus.PENDING) {
			throw new IllegalArgumentException();
		}

		return withStatus(CampaignApplicationStatus.ACCEPTED);
	}

	public CampaignApplication reject() {
		if (status != CampaignApplicationStatus.PENDING) {
			throw new IllegalArgumentException();
		}

		return withStatus(CampaignApplicationStatus.REJECTED);
	}

	public CampaignApplication withdraw() {
		if (status != CampaignApplicationStatus.PENDING) {
			throw new IllegalArgumentException();
		}

		return withStatus(CampaignApplicationStatus.WITHDRAWN);
	}

	private CampaignApplication withStatus(CampaignApplicationStatus newStatus) {
		return new CampaignApplication(
				id,
				campaignId,
				applicantId,
				characterId,
				newStatus,
				message,
				createdAt,
				Instant.now());
	}
}
