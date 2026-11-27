package com.dndcrowler.dnd_backend.domain.exception.campaignMember;

public class InvalidCampaignMemberCampaignIdException extends IllegalArgumentException {
	public InvalidCampaignMemberCampaignIdException() {
		super("Campaign member campaign id must be positive");
	}
}
