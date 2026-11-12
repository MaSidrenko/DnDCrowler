package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Response;

import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

public record CurrentUserResponse(
		Long userId,
		String userName,
		String email,
		UserRole role) {
	public static CurrentUserResponse from(AuthenticatedUserPrincipal principal) {
		return new CurrentUserResponse(
				principal.userId(), principal.username(), principal.email(), principal.role());
	}
}
