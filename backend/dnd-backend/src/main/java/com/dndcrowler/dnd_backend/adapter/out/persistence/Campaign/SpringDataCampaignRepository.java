package com.dndcrowler.dnd_backend.adapter.out.persistence.Campaign;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

public interface SpringDataCampaignRepository extends 
	JpaRepository<CampaignJpaEntity, Long>,
	JpaSpecificationExecutor<CampaignJpaEntity> {
	List<CampaignJpaEntity> findByGameMasterId(Long gameMasterId);

	Optional<CampaignJpaEntity> findByIdAndStatusIn(
		Long campaignId,
		Collection<CampaignStatus> statuses
	);
}
