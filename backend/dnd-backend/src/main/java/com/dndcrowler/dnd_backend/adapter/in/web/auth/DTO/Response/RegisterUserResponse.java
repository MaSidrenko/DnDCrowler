package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Response;

public record RegisterUserResponse(
	Long userId,
	String userName,
	String email
) {
	
}
