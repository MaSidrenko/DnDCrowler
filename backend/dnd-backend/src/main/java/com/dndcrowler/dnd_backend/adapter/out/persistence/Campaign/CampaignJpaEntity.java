package com.dndcrowler.dnd_backend.adapter.out.persistence.Campaign;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "campaigns")
public class CampaignJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long gameMasterId;
	private String name;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private CampaignStatus status;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private CampaignFormat format;
	private int maxPlayerCount;
	private int minLevel;
	private int maxLevel;
	@Embedded
	private CampaignScheduleJpaEmbeddable schedule;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String requirements;
	private Instant createdAt;
	private Instant updatedAt;

	protected CampaignJpaEntity() {
	}

	public CampaignJpaEntity(Campaign campaign) {

		this.id = campaign.id();
		this.gameMasterId = campaign.gameMasterId();
		this.name = campaign.name();
		this.description = campaign.description();
		this.status = campaign.status();
		this.format = campaign.format();
		this.maxPlayerCount = campaign.maxPlayerCount();
		this.minLevel = campaign.minLevel();
		this.maxLevel = campaign.maxLevel();
		this.schedule = new CampaignScheduleJpaEmbeddable(
				campaign.schedule());
		this.requirements = campaign.requirements();
		this.createdAt = campaign.createdAt();
		this.updatedAt = campaign.updatedAt();
	}

	public Campaign toDomain() {
		return new Campaign(id, gameMasterId, name,
				description, status, format,
				maxPlayerCount, minLevel, maxLevel, schedule.toDomain(),
				requirements, createdAt, updatedAt);
	}
}
