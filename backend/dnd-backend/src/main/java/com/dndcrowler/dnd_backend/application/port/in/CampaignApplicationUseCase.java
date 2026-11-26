package com.dndcrowler.dnd_backend.application.port.in;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication.SubmitCampaignApplicationCommand;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;

public interface CampaignApplicationUseCase {
	CampaignApplication submit(SubmitCampaignApplicationCommand command);

	CampaignApplication accept(
			Long applicationId,
			Long currentUserId);

	CampaignApplication reject(
			Long applicationId,
			Long currentUserId);

	CampaignApplication withdraw(
			Long applicationId,
			Long currentUserId);

	List<CampaignApplication> getMyApplications(Long currentUserId);

	List<CampaignApplication> getCampaignApplications(
			Long campaignId,
			Long currentUserId);
}
