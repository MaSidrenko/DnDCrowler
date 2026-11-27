package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignApplication;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "campaign_applications")
public class CampaignApplicationJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long campaignId;
	private Long applicantId;
	private Long characterId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private CampaignApplicationStatus status;
	@Column(columnDefinition = "TEXT")
	private String message;
	private Instant createdAt;
	private Instant updatedAt;

	protected CampaignApplicationJpaEntity() {
	}

	public CampaignApplicationJpaEntity(CampaignApplication campaignApplication) {
		this.id = campaignApplication.id();
		this.campaignId = campaignApplication.campaignId();
		this.applicantId = campaignApplication.applicantId();
		this.characterId = campaignApplication.characterId();
		this.status = campaignApplication.status();
		this.message = campaignApplication.message();
		this.createdAt = campaignApplication.createdAt();
		this.updatedAt = campaignApplication.updatedAt();
	}

	public CampaignApplication toDomain() {
		return new CampaignApplication(id, campaignId, applicantId, characterId,
				status, message, createdAt, updatedAt);
	}
}
