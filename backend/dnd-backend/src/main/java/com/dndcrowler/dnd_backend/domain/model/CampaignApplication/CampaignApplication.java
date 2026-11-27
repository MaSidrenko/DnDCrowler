package com.dndcrowler.dnd_backend.domain.model.CampaignApplication;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationApplicantIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationCampaignIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationCharacterIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationStatusException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationStatusTransitionException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationUpdatedAtBeforeCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationUpdatedAtException;

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
		if (campaignId == null || campaignId <= 0) {
			throw new InvalidCampaignApplicationCampaignIdException();
		}

		if (applicantId == null || applicantId <= 0) {
			throw new InvalidCampaignApplicationApplicantIdException();
		}

		if (characterId == null || characterId <= 0) {
			throw new InvalidCampaignApplicationCharacterIdException();
		}

		if (status == null) {
			throw new InvalidCampaignApplicationStatusException();
		}

		if (createdAt == null) {
			throw new InvalidCampaignApplicationCreatedAtException();
		}

		if (updatedAt == null) {
			throw new InvalidCampaignApplicationUpdatedAtException();
		}

		if (updatedAt.isBefore(createdAt)) {
			throw new InvalidCampaignApplicationUpdatedAtBeforeCreatedAtException();
		}
	}

	public CampaignApplication accept() {
		if (status != CampaignApplicationStatus.PENDING) {
			throw new InvalidCampaignApplicationStatusTransitionException(
					status,
					CampaignApplicationStatus.ACCEPTED);
		}

		return withStatus(CampaignApplicationStatus.ACCEPTED);
	}

	public CampaignApplication reject() {
		if (status != CampaignApplicationStatus.PENDING) {
			throw new InvalidCampaignApplicationStatusTransitionException(
					status,
					CampaignApplicationStatus.REJECTED);
		}

		return withStatus(CampaignApplicationStatus.REJECTED);
	}

	public CampaignApplication withdraw() {
		if (status != CampaignApplicationStatus.PENDING) {
			throw new InvalidCampaignApplicationStatusTransitionException(
					status,
					CampaignApplicationStatus.WITHDRAWN);
		}

		return withStatus(CampaignApplicationStatus.WITHDRAWN);
	}

	public static CampaignApplication create(Long campaignId,
			Long applicantId, Long characterId, String message) {
		Instant now = Instant.now();
		return new CampaignApplication(null, campaignId,
				applicantId, characterId, CampaignApplicationStatus.PENDING,
				message, now, now);
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
