package com.dndcrowler.dnd_backend.domain.exception.campaignMember;

public class InvalidCampaignMemberJoinedAtException extends IllegalArgumentException {
	public InvalidCampaignMemberJoinedAtException() {
		super("Campaign member join time is required");
	}
}
