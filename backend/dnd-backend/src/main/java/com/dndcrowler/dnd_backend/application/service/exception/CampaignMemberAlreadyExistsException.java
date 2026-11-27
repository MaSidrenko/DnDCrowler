package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignMemberAlreadyExistsException extends RuntimeException {
	public CampaignMemberAlreadyExistsException(
			Long campaignId,
			Long userId) {
		super("User is already a campaign member: campaignId="
				+ campaignId + ", userId=" + userId);
	}
}
