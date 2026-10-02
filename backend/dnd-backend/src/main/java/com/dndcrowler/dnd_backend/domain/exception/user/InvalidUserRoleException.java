package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserRoleException extends IllegalArgumentException {

	public InvalidUserRoleException() {
		super("Role can`t be empty");
	}
	
}
