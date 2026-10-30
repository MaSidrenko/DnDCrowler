package com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken;

public class InvalidEmailTokenCreatedAtException extends IllegalArgumentException {

	public InvalidEmailTokenCreatedAtException() {
		super("Created at can`t be null!");
	}
}
