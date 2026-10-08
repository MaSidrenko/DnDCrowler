package com.dndcrowler.dnd_backend.application.port.in.command.User;

import com.dndcrowler.dnd_backend.application.exception.InvalidRegisterPasswordException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserEmailException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserUsernameException;

public record RegisterUserCommand (
	String username,
	String email,
	String password
) {
	public RegisterUserCommand {
		if(username == null || username.isBlank()) 
			throw new InvalidUserUsernameException();

		if(email == null || email.isBlank()) 
			throw new InvalidUserEmailException();

		if(password == null || password.isBlank())
			throw new InvalidRegisterPasswordException();
	}

	@Override 
	public String toString() {
		return "RegisterUserCommand[username=" + username
		+ ", email=" + email
		+ ", password=[REDACTED]"
		+ "]";
	}
}
