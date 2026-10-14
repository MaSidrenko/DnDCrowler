package com.dndcrowler.dnd_backend.application.service.User;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

import jakarta.transaction.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort;

	public RegisterUserService(
			UserRepositoryPort userRepositoryPort,
			PasswordHasherPort passwordHasherPort,
			PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.passwordHasherPort = passwordHasherPort;
		this.passwordCredentialsRepositoryPort = passwordCredentialsRepositoryPort;
	}

	@Override
	@Transactional
	public User registerUser(RegisterUserCommand command) {
		if (userRepositoryPort.existsByEmail(command.email()))
			throw new EmailAlreadyRegisteredException();

		User newUser = User.create(
				command.username(),
				command.email());
		String hashedPassword = passwordHasherPort.hashPassword(command.password());

		User savedUser = userRepositoryPort.save(newUser);
		PasswordCredentials credentials = PasswordCredentials.create(
				savedUser.id(),
				hashedPassword);
		passwordCredentialsRepositoryPort.save(credentials);

		return savedUser;
	}

}
