package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank 
	@Email 
	String email,

	@NotBlank 
	String password
) {
	@Override 
	public String toString() {
		return "LoginRequest[email=" + email + ", password=[REDACTED]]";
	}
} 
