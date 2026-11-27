package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignApplicationAlreadyExistsException extends RuntimeException {
	public CampaignApplicationAlreadyExistsException(
			Long campaignId,
			Long applicantId) {
		super("A pending campaign application already exists: campaignId="
				+ campaignId + ", applicantId=" + applicantId);
	}
}
