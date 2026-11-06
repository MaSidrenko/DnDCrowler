package com.dndcrowler.dnd_backend.application.service.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.LoginUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.LoginUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.EmailNotVerifiedException;
import com.dndcrowler.dnd_backend.application.service.exception.InvalidCredentialsException;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;
import com.dndcrowler.dnd_backend.domain.model.User.User;

@Service
public class LoginUserService implements LoginUserUseCase {
	private static final Logger log = LoggerFactory.getLogger(LoginUserService.class);

	private final UserRepositoryPort userRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort;

	public LoginUserService(UserRepositoryPort userRepositoryPort,
		 PasswordHasherPort passwordHasherPort, PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.passwordHasherPort = passwordHasherPort;
		this.passwordCredentialsRepositoryPort = passwordCredentialsRepositoryPort;
	}

	@Override
	public User loginUser(LoginUserCommand command) {
		User user = userRepositoryPort.findByEmail(command.email())
				.orElseThrow(() -> {
					log.debug("User login rejected: invalid credentials");
					return new InvalidCredentialsException();
				});

		PasswordCredentials credentials = passwordCredentialsRepositoryPort.findByUserId(user.id())
				.orElseThrow(() -> {
					log.warn("User login rejected: password credentials missing, userId={}", user.id());
					return new InvalidCredentialsException();
				});

		if(!passwordHasherPort.matches(command.password(), credentials.passwordHash())) {
			log.debug("User login rejected: invalid credentials, userId={}", user.id());
			throw new InvalidCredentialsException();
		}

		if (user.emailVerifiedAt() == null) {
			log.debug("User login rejected: email is not verified, userId={}", user.id());
			throw new EmailNotVerifiedException();
		}

		log.info("User login complete: userId={}", user.id());

		return user;
	}
	
}
