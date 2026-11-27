package com.dndcrowler.dnd_backend.domain.exception.campaignMember;

public class InvalidCampaignMemberUserIdException extends IllegalArgumentException {
	public InvalidCampaignMemberUserIdException() {
		super("Campaign member user id must be positive");
	}
}
