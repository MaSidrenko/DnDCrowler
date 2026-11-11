package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Response;

import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

public record LoginResponse(
	Long userId,
	String userName,
	String email,
	UserRole role
) {
	
}
