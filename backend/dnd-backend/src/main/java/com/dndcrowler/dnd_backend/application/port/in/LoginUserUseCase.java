package com.dndcrowler.dnd_backend.application.port.in;

import com.dndcrowler.dnd_backend.application.port.in.command.User.LoginUserCommand;
import com.dndcrowler.dnd_backend.domain.model.User.User;

public interface LoginUserUseCase {
	User loginUser(LoginUserCommand command);
}
