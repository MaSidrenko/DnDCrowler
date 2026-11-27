package com.dndcrowler.dnd_backend.adapter.in.web.campaignMember;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignMemberNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignRosterChangeNotAllowedException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice(assignableTypes = CampaignMemberController.class)
public class CampaignMemberExceptionHandler {
	@ExceptionHandler({
			CampaignMemberNotFoundException.class,
			CampaignNotFoundException.class
	})
	public ResponseEntity<ProblemDetail> handleNotFound(
			RuntimeException exception) {
		return problem(HttpStatus.NOT_FOUND, exception);
	}

	@ExceptionHandler(CampaignAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(
			CampaignAccessDeniedException exception) {
		return problem(HttpStatus.FORBIDDEN, exception);
	}

	@ExceptionHandler(CampaignRosterChangeNotAllowedException.class)
	public ResponseEntity<ProblemDetail> handleConflict(
			CampaignRosterChangeNotAllowedException exception) {
		return problem(HttpStatus.CONFLICT, exception);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleInvalidPathParameter(
			ConstraintViolationException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception);
	}

	private ResponseEntity<ProblemDetail> problem(
			HttpStatus status,
			Exception exception) {
		return ResponseEntity.status(status)
				.body(ProblemDetail.forStatusAndDetail(
						status,
						exception.getMessage()));
	}
}
