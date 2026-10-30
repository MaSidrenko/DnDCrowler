package com.dndcrowler.dnd_backend.application.service.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.Email.IssueEmailVerificationService;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;
import com.dndcrowler.dnd_backend.domain.model.User.User;

import jakarta.transaction.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {
	private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

	private final UserRepositoryPort userRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort;
	private final IssueEmailVerificationService issueEmailVerificationService;

	public RegisterUserService(
			UserRepositoryPort userRepositoryPort,
			PasswordHasherPort passwordHasherPort,
			PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort,
			IssueEmailVerificationService issueEmailVerificationService
		) {
		this.userRepositoryPort = userRepositoryPort;
		this.passwordHasherPort = passwordHasherPort;
		this.passwordCredentialsRepositoryPort = passwordCredentialsRepositoryPort;
		this.issueEmailVerificationService = issueEmailVerificationService;
	}

	@Override
	@Transactional
	public User registerUser(RegisterUserCommand command) {
		if (userRepositoryPort.existsByEmail(command.email()))
		{
			log.debug("User registration rejected: email already registered");
			throw new EmailAlreadyRegisteredException();
		}

		User newUser = User.create(
				command.username(),
				command.email());
		String hashedPassword = passwordHasherPort.hashPassword(command.password());

		User savedUser = userRepositoryPort.save(newUser);
		PasswordCredentials credentials = PasswordCredentials.create(
				savedUser.id(),
				hashedPassword);
		passwordCredentialsRepositoryPort.save(credentials);
		
		issueEmailVerificationService.issue(savedUser.id(), savedUser.email());

		log.info("User registration complete");

		return savedUser;
	}

}
