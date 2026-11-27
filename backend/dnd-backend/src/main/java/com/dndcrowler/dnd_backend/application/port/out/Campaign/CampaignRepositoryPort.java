package com.dndcrowler.dnd_backend.application.port.out.Campaign;

import java.util.List;
import java.util.Optional;

import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;

public interface CampaignRepositoryPort {
	Campaign save(Campaign campaign);

	Optional<Campaign> findById(Long campaignId);

	Optional<Campaign> findByIdForUpdate(Long campaignId);

	Optional<Campaign> findPubliclyVisibleById(Long campaignId);

	List<Campaign> findByGameMasterId(Long gameMasterId);

	List<Campaign> searchRecruiting(
			CampaignSearchCriteria criteria);
}
