package com.dndcrowler.dnd_backend.adapter.in.web.security;

import java.io.Serial;
import java.io.Serializable;

import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

public record AuthenticatedUserPrincipal(
	Long userId,
	String username,
	String email,
	UserRole role
) implements Serializable {
	
	@Serial 
	private static final long serialVersionUID  = 1L;

	public static AuthenticatedUserPrincipal from(User user) {
		return new AuthenticatedUserPrincipal(
			user.id(),
			user.username(),
			user.email(),
			user.role()
		);
	}
}
