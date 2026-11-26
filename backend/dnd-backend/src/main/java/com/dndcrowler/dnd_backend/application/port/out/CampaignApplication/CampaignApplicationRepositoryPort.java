package com.dndcrowler.dnd_backend.application.port.out.CampaignApplication;

import java.util.List;
import java.util.Optional;

import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;

public interface CampaignApplicationRepositoryPort {
	CampaignApplication save(CampaignApplication application);

	Optional<CampaignApplication> findById(Long id);

	boolean existsActiveByCampaignIdAndApplicantId(
			Long campaignId,
			Long applicationId);

	List<CampaignApplication> findByApplicantId(Long applicantId);

	List<CampaignApplication> findByCampaignId(Long campaignId);
}
