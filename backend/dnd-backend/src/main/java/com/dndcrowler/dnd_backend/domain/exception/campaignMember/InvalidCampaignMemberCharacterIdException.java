package com.dndcrowler.dnd_backend.domain.exception.campaignMember;

public class InvalidCampaignMemberCharacterIdException extends IllegalArgumentException {
	public InvalidCampaignMemberCharacterIdException() {
		super("Campaign member character id must be positive");
	}
}
