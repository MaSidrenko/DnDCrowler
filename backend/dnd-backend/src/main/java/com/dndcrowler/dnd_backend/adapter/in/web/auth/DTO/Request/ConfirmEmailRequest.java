package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmEmailRequest(
	@NotBlank 
	@Email 
	String email,
	
	@NotBlank 
	@Pattern(regexp = "\\d{6}")
	String code) {

}
