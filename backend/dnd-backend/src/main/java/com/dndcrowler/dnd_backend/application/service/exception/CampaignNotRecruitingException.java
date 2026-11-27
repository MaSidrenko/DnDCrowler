package com.dndcrowler.dnd_backend.application.service.exception;

import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

public class CampaignNotRecruitingException extends RuntimeException {
	public CampaignNotRecruitingException(
			Long campaignId,
			CampaignStatus status) {
		super("Campaign is not recruiting: id=" + campaignId
				+ ", status=" + status);
	}
}
