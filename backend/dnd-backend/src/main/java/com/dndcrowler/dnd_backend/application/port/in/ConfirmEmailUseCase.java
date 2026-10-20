package com.dndcrowler.dnd_backend.application.port.in;

import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;

public interface ConfirmEmailUseCase {
	boolean confirmEmail(ConfirmEmailCommand command);
} 
