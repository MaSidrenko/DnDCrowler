package com.dndcrowler.dnd_backend.domain.model.Password;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.exception.passwordCredentials.InvalidPasswordCredentialsChangedAtException;
import com.dndcrowler.dnd_backend.domain.exception.passwordCredentials.InvalidPasswordCredentialsPasswordHashException;
import com.dndcrowler.dnd_backend.domain.exception.passwordCredentials.InvalidPasswordCredentialsUserIdException;


public record PasswordCredentials(
	Long userId,
	String passwordHash,
	Instant passwordChangedAt
) {
	public PasswordCredentials {
		if(userId == null) 
			throw new InvalidPasswordCredentialsUserIdException();
		
		if(passwordHash == null || passwordHash.isBlank()) 
			throw new InvalidPasswordCredentialsPasswordHashException();
		
		if(passwordChangedAt == null) 
			throw new InvalidPasswordCredentialsChangedAtException();

	}

	public static PasswordCredentials create(Long userId, String passwordHash) {
		return new PasswordCredentials(userId, passwordHash, Instant.now());
	}

	
	@Override
	public String toString() {
		return "PasswordCredentials[userId=" + userId
        + ", passwordHash=[REDACTED]"
        + ", passwordChangedAt=" + passwordChangedAt 
        + "]";
	}
}
