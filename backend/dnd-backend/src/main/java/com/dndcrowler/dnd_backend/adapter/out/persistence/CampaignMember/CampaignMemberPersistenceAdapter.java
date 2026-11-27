package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember;

import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.out.CampaignMember.CampaignMemberRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;

@Repository 
public class CampaignMemberPersistenceAdapter implements CampaignMemberRepositoryPort {
	private final SpringDataCampaignMemberRepository repository;

	public CampaignMemberPersistenceAdapter(
			SpringDataCampaignMemberRepository repository) {
		this.repository = repository;
	}

	@Override
	public CampaignMember save(CampaignMember member) {
		return repository
				.saveAndFlush(new CampaignMemberJpaEntity(member))
				.toDomain();
	}

	@Override
	public boolean existsByCampaignIdAndUserId(Long campaignId, Long userId) {
		return repository
				.existsByCampaignIdAndUserId(campaignId, userId);
	}

	@Override
	public long countByCampaignId(Long campaignId) {
		return repository
				.countByCampaignId(campaignId);
	}

}
