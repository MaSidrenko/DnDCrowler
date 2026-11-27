package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignApplication;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.out.CampaignApplication.CampaignApplicationRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;

@Repository 
public class CampaignApplicationPersistenceAdapter implements CampaignApplicationRepositoryPort {
	private final SpringDataCampaignApplicationRepository repository;

	public CampaignApplicationPersistenceAdapter(
			SpringDataCampaignApplicationRepository repository) {
		this.repository = repository;
	}

	@Override
	public CampaignApplication save(CampaignApplication application) {
		return repository.saveAndFlush(new CampaignApplicationJpaEntity(application)).toDomain();
	}

	@Override
	public Optional<CampaignApplication> findById(Long id) {
		return repository.findById(id).map(
				CampaignApplicationJpaEntity::toDomain);
	}

	@Override
	public Optional<CampaignApplication> findByIdForUpdate(Long applicationId) {
		return repository.findByIdForUpdate(applicationId)
				.map(CampaignApplicationJpaEntity::toDomain);
	}

	@Override
	public boolean existsActiveByCampaignIdAndApplicantId(Long campaignId, Long applicantId) {
		return repository.existsByCampaignIdAndApplicantIdAndStatus(
				campaignId, applicantId, CampaignApplicationStatus.PENDING);
	}

	@Override
	public List<CampaignApplication> findByApplicantId(Long applicantId) {
		return repository.findByApplicantIdOrderByCreatedAtDesc(applicantId)
				.stream()
				.map(CampaignApplicationJpaEntity::toDomain)
				.toList();
	}

	@Override
	public List<CampaignApplication> findByCampaignId(Long campaignId) {
		return repository.findByCampaignIdOrderByCreatedAtDesc(campaignId)
				.stream()
				.map(CampaignApplicationJpaEntity::toDomain)
				.toList();
	}

}
