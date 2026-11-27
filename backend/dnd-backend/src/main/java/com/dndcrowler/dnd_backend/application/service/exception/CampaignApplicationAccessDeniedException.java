package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignApplicationAccessDeniedException extends RuntimeException {
	public CampaignApplicationAccessDeniedException() {
		super("You do not have permission to modify this campaign application");
	}
}
