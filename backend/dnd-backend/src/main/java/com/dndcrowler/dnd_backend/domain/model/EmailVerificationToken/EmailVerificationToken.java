package com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken;

import java.time.Instant;
import java.util.regex.Pattern;

import com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken.InvalidEmailTokenEmailException;
import com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken.InvalidEmailTokenExpiresAtException;
import com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken.InvalidEmailTokenPatternEmailException;
import com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken.InvalidEmailTokenTokenException;
import com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken.InvalidEmailTokenUserIdException;

public record EmailVerificationToken(
	Long userId,
	String email,
	String tokenHash,
	Instant expiresAt,
	Instant usedAt
) {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	public EmailVerificationToken {
		if(userId == null) 
			throw new InvalidEmailTokenUserIdException();

		if(email == null || email.isBlank()) 
			throw new InvalidEmailTokenEmailException();
		
		if(tokenHash == null || tokenHash.isBlank()) 
			throw new InvalidEmailTokenTokenException();

		if(expiresAt == null) 
			throw new InvalidEmailTokenExpiresAtException();

		if(!EMAIL_PATTERN.matcher(email).matches()) 
			throw new InvalidEmailTokenPatternEmailException();
	}

	public static EmailVerificationToken create(Long userId, String email, 
			String tokenHash, Instant expiresAt) {
				return new EmailVerificationToken(userId, email, tokenHash, expiresAt, null);
	}

}
