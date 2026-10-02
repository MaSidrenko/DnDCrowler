package com.dndcrowler.dnd_backend.domain.exception.passwordCredentials;

public class InvalidPasswordCredentialsChangedAtException extends IllegalArgumentException {

	public InvalidPasswordCredentialsChangedAtException() {
		super("Password changed at cannot be null");
	}
	
}
