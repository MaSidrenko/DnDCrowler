package com.dndcrowler.dnd_backend.application.service.Email;

public record VerificationCodeIssuedEvent(String email, String code) {
	@Override 
	public String toString() {
		return "VerificationCodeIssuedEvent[code=[REDACTED]]";
	}
}
