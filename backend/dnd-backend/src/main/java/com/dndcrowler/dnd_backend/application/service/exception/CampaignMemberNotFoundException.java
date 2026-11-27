package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignMemberNotFoundException extends RuntimeException {
	public CampaignMemberNotFoundException(Long memberId) {
		super("Campaign member not found: id=" + memberId);
	}

	public CampaignMemberNotFoundException(
			Long campaignId,
			Long userId) {
		super("Campaign membership not found: campaignId="
				+ campaignId + ", userId=" + userId);
	}
}
