package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignMember;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCampaignMemberRepository 
	extends JpaRepository<CampaignMemberJpaEntity, Long> {
	boolean existsByCampaignIdAndUserId(
		Long campaignId,
		Long userId
	);

	long countByCampaignId(Long campaignId);
}
