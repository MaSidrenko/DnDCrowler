package com.dndcrowler.dnd_backend.application.port.in;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.CreateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.UpdateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;

public interface CampaignUseCase {
	Campaign createCampaign(CreateCampaignCommand command);

	Campaign updateCampaign(
			Long campaignId,
			UpdateCampaignCommand command);

	Campaign publishCampaign(
			Long campaignId,
			Long currentUserId);

	Campaign startCampaign(
			Long campaignId,
			Long currentUserId);

	Campaign completeCampaign(
			Long campaignId,
			Long currentUserId);

	Campaign cancelCampaign(
			Long campaignId,
			Long currentUserId);

	Campaign getPublicCampaignById(Long campaignId);

	Campaign getMyCampaignById(
			Long campaignId,
			Long currentUserId);

	List<Campaign> getMyCampaigns(Long currentUserId);

	List<Campaign> searchCampaigns(
			CampaignSearchCriteria criteria);
}
