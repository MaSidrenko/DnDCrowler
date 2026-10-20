package com.dndcrowler.dnd_backend.application.port.out.Email;

import java.util.Optional;

import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;

public interface EmailVerificationRepositoryPort {
	EmailVerificationToken save(EmailVerificationToken token);
	Optional<EmailVerificationToken> findCurrentByEmail(String email);
}
