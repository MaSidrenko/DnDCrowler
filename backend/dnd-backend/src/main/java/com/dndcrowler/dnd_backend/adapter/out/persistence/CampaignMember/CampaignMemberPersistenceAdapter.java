package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember;

import java.util.List;
import java.util.Optional;

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

	@Override
	public void deleteById(Long memberId) {
		repository.deleteById(memberId);
	}

	@Override
	public List<CampaignMember> findByCampaignId(Long campaignId) {
		return repository.findByCampaignId(campaignId)
				.stream()
				.map(CampaignMemberJpaEntity::toDomain)
				.toList();
	}

	@Override
	public Optional<CampaignMember> findByCampaignIdAndUserId(Long campaignId, Long userId) {
		return repository.findByCampaignIdAndUserId(campaignId, userId)
				.map(CampaignMemberJpaEntity::toDomain);
	}

	@Override
	public Optional<CampaignMember> findById(Long memberId) {
		return repository.findById(memberId)
				.map(CampaignMemberJpaEntity::toDomain);
	}

	@Override
	public List<CampaignMember> findByUserId(Long userId) {
		return repository.findByUserId(userId)
				.stream()
				.map(CampaignMemberJpaEntity::toDomain)
				.toList();
	}

}
