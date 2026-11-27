package com.dndcrowler.dnd_backend.domain.exception.campaignApplication;

public class InvalidCampaignApplicationApplicantIdException extends IllegalArgumentException {
	public InvalidCampaignApplicationApplicantIdException() {
		super("Campaign application applicant id must be positive");
	}
}
