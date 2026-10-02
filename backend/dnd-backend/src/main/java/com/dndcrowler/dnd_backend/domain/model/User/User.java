package com.dndcrowler.dnd_backend.domain.model.User;

import java.time.Instant;
import java.util.regex.Pattern;

import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserEmailException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserEmailPatternException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserRoleException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserUpdatedAtBeforeCreatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserUpdatedAtException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserUsernameException;

public record User(
		Long id,
		String username,
		String email,
		UserRole role,
		Instant createdAt,
		Instant updatedAt,
		Instant emailVerifiedAt) {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	public User {

		if (username == null || username.isBlank())
			throw new InvalidUserUsernameException();

		if (role == null)
			throw new InvalidUserRoleException();

		if (email == null || email.isBlank())
			throw new InvalidUserEmailException();

		if (createdAt == null)
			throw new InvalidUserCreatedAtException();

		if (updatedAt == null)
			throw new InvalidUserUpdatedAtException();

		if (!EMAIL_PATTERN.matcher(email).matches())
			throw new InvalidUserEmailPatternException();

		if (updatedAt.isBefore(createdAt))
			throw new InvalidUserUpdatedAtBeforeCreatedAtException();

	}

	public static User create(String username, String email) {
		Instant now = Instant.now();
		return new User(null, username, email, UserRole.USER, now, now, null);
	}
}
