package com.dndcrowler.dnd_backend.application.service.exception;

public class CampaignAccessDeniedException extends RuntimeException {
	public CampaignAccessDeniedException() {
		super("You do not have permission to access this campaign");
	}
}
