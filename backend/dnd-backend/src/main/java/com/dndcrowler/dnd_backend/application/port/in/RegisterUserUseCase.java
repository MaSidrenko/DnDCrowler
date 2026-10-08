package com.dndcrowler.dnd_backend.application.port.in;

import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.domain.model.User.User;

public interface RegisterUserUseCase {
	User registerUser(RegisterUserCommand command);
}
