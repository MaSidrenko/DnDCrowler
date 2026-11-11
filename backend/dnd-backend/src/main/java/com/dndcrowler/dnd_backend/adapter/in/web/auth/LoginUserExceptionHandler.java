package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LoginUserController.class)
public class LoginUserExceptionHandler {
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ProblemDetail> handleBadCredentials() {
		return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ProblemDetail> handleDisabledUser() {
		return problem(HttpStatus.FORBIDDEN, "Email address is not verified");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status)
				.body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}
