package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "campaign_members")
public class CampaignMemberJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long campaignId;
	private Long userId;
	private Long characterId;
	private Instant joinedAt;

	protected CampaignMemberJpaEntity() {
	}

	public CampaignMemberJpaEntity(CampaignMember member) {
		this.id = member.id();
		this.campaignId = member.campaignId();
		this.userId = member.userId();
		this.characterId = member.characterId();
		this.joinedAt = member.joinedAt();
	}

	public CampaignMember toDomain() {
		return new CampaignMember(id, campaignId, userId,
				characterId, joinedAt);
	}
}
