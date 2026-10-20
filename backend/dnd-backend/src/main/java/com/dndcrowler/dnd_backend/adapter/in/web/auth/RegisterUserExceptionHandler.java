package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dndcrowler.dnd_backend.application.exception.InvalidRegisterPasswordException;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserEmailException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserEmailPatternException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserUsernameException;

@RestControllerAdvice(assignableTypes = RegisterUserController.class)
public class RegisterUserExceptionHandler {
	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	public ResponseEntity<ProblemDetail> handleDuplicateEmail(EmailAlreadyRegisteredException exception) {
		return problem(HttpStatus.CONFLICT, exception);
	}

	@ExceptionHandler({InvalidUserUsernameException.class, InvalidUserEmailException.class,
			InvalidUserEmailPatternException.class, InvalidRegisterPasswordException.class})
	public ResponseEntity<ProblemDetail> handleInvalidRegistration(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception);
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, Exception exception) {
		return ResponseEntity.status(status)
				.body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
	}
}
