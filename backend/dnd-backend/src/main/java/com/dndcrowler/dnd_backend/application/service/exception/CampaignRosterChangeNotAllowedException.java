package com.dndcrowler.dnd_backend.application.service.exception;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

public class CampaignRosterChangeNotAllowedException extends RuntimeException {
	public CampaignRosterChangeNotAllowedException(
			Long campaignId,
			CampaignStatus status) {
		super("Campaign roster cannot be changed: id=" + campaignId
				+ ", status=" + status);
	}
}
