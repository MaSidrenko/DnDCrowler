package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailRequest(
	@NotBlank 
	@Email 
	String email
) {
	
}
