package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserUpdatedAtBeforeCreatedAtException extends IllegalArgumentException {

	public InvalidUserUpdatedAtBeforeCreatedAtException() {
		super("Updated time precedes creation");
	}
	
}
