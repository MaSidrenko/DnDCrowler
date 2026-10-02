package com.dndcrowler.dnd_backend.domain.exception.passwordCredentials;

public class InvalidPasswordCredentialsUserIdException extends IllegalArgumentException {

	public InvalidPasswordCredentialsUserIdException() {
		super("User ID cannot be null");
	}
	
}
