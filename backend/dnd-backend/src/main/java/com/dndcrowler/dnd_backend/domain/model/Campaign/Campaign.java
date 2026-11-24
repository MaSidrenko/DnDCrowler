package com.dndcrowler.dnd_backend.domain.model.Campaign;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignDescriptionException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignFormatException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignGameMasterIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignLevelRangeException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignNameException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignPlayerCountException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignRequirementsException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignScheduleException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusTransitionException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignUpdatedAtBeforeCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignUpdatedAtException;

public record Campaign(
		Long id,
		Long gameMasterId,
		String name,
		String description,
		CampaignStatus status,
		CampaignFormat format,
		int maxPlayerCount,
		int minLevel,
		int maxLevel,
		CampaignSchedule schedule,
		String requirements,
		Instant createdAt,
		Instant updatedAt) {

	public Campaign {
		if (gameMasterId == null || gameMasterId <= 0) {
			throw new InvalidCampaignGameMasterIdException();
		}
		if (name == null || name.isBlank()) {
			throw new InvalidCampaignNameException();
		}
		if (description == null || description.isBlank()) {
			throw new InvalidCampaignDescriptionException();
		}
		if (status == null) {
			throw new InvalidCampaignStatusException();
		}
		if (format == null) {
			throw new InvalidCampaignFormatException();
		}
		if (maxPlayerCount < 1) {
			throw new InvalidCampaignPlayerCountException();
		}
		if (minLevel < 1 || maxLevel > 20 || minLevel > maxLevel) {
			throw new InvalidCampaignLevelRangeException();
		}
		if (schedule == null) {
			throw new InvalidCampaignScheduleException();
		}
		if (requirements == null) {
			throw new InvalidCampaignRequirementsException();
		}
		if (createdAt == null) {
			throw new InvalidCampaignCreatedAtException();
		}
		if (updatedAt == null) {
			throw new InvalidCampaignUpdatedAtException();
		}
		if (updatedAt.isBefore(createdAt)) {
			throw new InvalidCampaignUpdatedAtBeforeCreatedAtException();
		}
	}

	public static Campaign create(
			Long gameMasterId,
			String name,
			String description,
			CampaignFormat format,
			int maxPlayerCount,
			int minLevel,
			int maxLevel,
			CampaignSchedule schedule,
			String requirements) {
		Instant now = Instant.now();

		return new Campaign(
				null,
				gameMasterId,
				name,
				description,
				CampaignStatus.DRAFT,
				format,
				maxPlayerCount,
				minLevel,
				maxLevel,
				schedule,
				requirements,
				now,
				now);
	}

	public Campaign update(
			String name,
			String description,
			CampaignFormat format,
			int maxPlayerCount,
			int minLevel,
			int maxLevel,
			CampaignSchedule schedule,
			String requirements) {
		return new Campaign(
				id,
				gameMasterId,
				name,
				description,
				status,
				format,
				maxPlayerCount,
				minLevel,
				maxLevel,
				schedule,
				requirements,
				createdAt,
				Instant.now());
	}

	public Campaign publish() {
		return changeStatus(
				CampaignStatus.DRAFT,
				CampaignStatus.RECRUITING);
	}

	public Campaign start() {
		if (status != CampaignStatus.RECRUITING
				&& status != CampaignStatus.FULL) {
			throw new InvalidCampaignStatusTransitionException(
					status,
					CampaignStatus.IN_PROGRESS);
		}

		return withStatus(CampaignStatus.IN_PROGRESS);
	}

	public Campaign complete() {
		return changeStatus(
			CampaignStatus.IN_PROGRESS, 
			CampaignStatus.COMPLETED
		);
	}

	public Campaign cancel() {
		if (status == CampaignStatus.COMPLETED
				|| status == CampaignStatus.CANCELLED) {
			throw new InvalidCampaignStatusTransitionException(
					status,
					CampaignStatus.CANCELLED);
		}

		return withStatus(CampaignStatus.CANCELLED);
	}

	private Campaign changeStatus(
			CampaignStatus expected,
			CampaignStatus target) {
		if (status != expected) {
			throw new InvalidCampaignStatusTransitionException(status, target);
		}

		return withStatus(target);
	}

	private Campaign withStatus(CampaignStatus newStatus) {
		return new Campaign(
				id,
				gameMasterId,
				name,
				description,
				newStatus,
				format,
				maxPlayerCount,
				minLevel,
				maxLevel,
				schedule,
				requirements,
				createdAt,
				Instant.now());
	}
}
