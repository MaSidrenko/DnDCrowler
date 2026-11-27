package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;

public class InvalidCampaignApplicationStatusTransitionException extends IllegalStateException {
	public InvalidCampaignApplicationStatusTransitionException(
			CampaignApplicationStatus currentStatus,
			CampaignApplicationStatus targetStatus) {
		super("Campaign application status cannot be changed from "
				+ currentStatus + " to " + targetStatus);
	}
}
