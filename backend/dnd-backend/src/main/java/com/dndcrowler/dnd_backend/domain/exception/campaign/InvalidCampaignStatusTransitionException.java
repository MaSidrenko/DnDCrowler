package com.dndcrowler.dnd_backend.domain.exception.campaign;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

public class InvalidCampaignStatusTransitionException extends IllegalStateException {
	public InvalidCampaignStatusTransitionException(
			CampaignStatus currentStatus,
			CampaignStatus targetStatus) {
		super("Campaign status cannot be changed from "
				+ currentStatus + " to " + targetStatus);
	}
}
