package com.dndcrowler.dnd_backend.adapter.in.web.character;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;

@RestControllerAdvice(assignableTypes = CharacterController.class)
public class CharacterExceptionHandler {
	@ExceptionHandler({CharacterNotFoundException.class, UserNotFoundException.class})
	public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
		return problem(HttpStatus.NOT_FOUND, exception);
	}

	@ExceptionHandler(CharacterAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(CharacterAccessDeniedException exception) {
		return problem(HttpStatus.FORBIDDEN, exception);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ProblemDetail> handleInvalidCharacter(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception);
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, Exception exception) {
		return ResponseEntity.status(status)
				.body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
	}
}
