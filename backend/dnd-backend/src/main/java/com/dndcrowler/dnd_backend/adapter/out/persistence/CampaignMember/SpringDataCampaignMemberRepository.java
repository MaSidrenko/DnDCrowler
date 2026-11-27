package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCampaignMemberRepository
		extends JpaRepository<CampaignMemberJpaEntity, Long> {
	boolean existsByCampaignIdAndUserId(
			Long campaignId,
			Long userId);

	long countByCampaignId(Long campaignId);

	List<CampaignMemberJpaEntity> findByCampaignId(Long campaignId);

	List<CampaignMemberJpaEntity> findByUserId(Long userId);

	Optional<CampaignMemberJpaEntity> findByCampaignIdAndUserId(
			Long campaignId,
			Long userId);
}
