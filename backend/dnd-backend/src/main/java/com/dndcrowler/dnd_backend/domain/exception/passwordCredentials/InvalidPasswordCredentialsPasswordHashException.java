package com.dndcrowler.dnd_backend.domain.exception.passwordCredentials;

public class InvalidPasswordCredentialsPasswordHashException extends IllegalArgumentException {

	public InvalidPasswordCredentialsPasswordHashException() {
		super("Password hash can`t be null");
	}
	
}
