package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserUpdatedAtException extends IllegalArgumentException {

	public InvalidUserUpdatedAtException() {
		super("User updated at can`t be null!");
	}
	
}
