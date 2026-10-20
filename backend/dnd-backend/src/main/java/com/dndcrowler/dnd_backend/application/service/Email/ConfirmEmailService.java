package com.dndcrowler.dnd_backend.application.service.Email;



import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.ConfirmEmailUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.EmailVerificationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationCodeHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;
import com.dndcrowler.dnd_backend.domain.model.User.User;

import jakarta.transaction.Transactional;

@Service 
public class ConfirmEmailService implements ConfirmEmailUseCase {

	private final EmailVerificationRepositoryPort emailPort;
	private final VerificationCodeHasherPort verificationPort;
	private final UserRepositoryPort userPort;

	public ConfirmEmailService(EmailVerificationRepositoryPort emailPort,
		VerificationCodeHasherPort verificationPort,
		UserRepositoryPort userPort) {
		this.emailPort = emailPort;
		this.verificationPort = verificationPort;
		this.userPort = userPort;
	}

	@Override
	@Transactional 
	public boolean confirmEmail(ConfirmEmailCommand command) {
		Optional<EmailVerificationToken> token = 
			emailPort.findCurrentByEmail(command.email());
		
		if(token.isEmpty())
			return false;

		EmailVerificationToken tokenGet = token.get();

		if(!tokenGet.expiresAt().isAfter(Instant.now()))
			return false;

		if(tokenGet.usedAt() != null)
			return false;

		if(!verificationPort.matches(command.code(), tokenGet.tokenHash()))
			return false;

		Optional<User> user = userPort.findById(tokenGet.userId());
		
		if(user.isEmpty())
			return false;

		User verifiedUser = user.get().emailVerifyAt();


		userPort.save(verifiedUser);

		EmailVerificationToken verifiedToken =tokenGet.markAsUsed();

		emailPort.save(verifiedToken);

		return true;

	}
	
}
