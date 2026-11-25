package com.dndcrowler.dnd_backend.adapter.in.web.campaign;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusTransitionException;

@RestControllerAdvice(assignableTypes = CampaignController.class)
public class CampaignExceptionHandler {
	@ExceptionHandler({CampaignNotFoundException.class, UserNotFoundException.class})
	public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
		return problem(HttpStatus.NOT_FOUND, exception);
	}

	@ExceptionHandler(CampaignAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(CampaignAccessDeniedException exception) {
		return problem(HttpStatus.FORBIDDEN, exception);
	}

	@ExceptionHandler(InvalidCampaignStatusTransitionException.class)
	public ResponseEntity<ProblemDetail> handleStatusConflict(
			InvalidCampaignStatusTransitionException exception) {
		return problem(HttpStatus.CONFLICT, exception);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ProblemDetail> handleInvalidCampaign(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception);
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, Exception exception) {
		return ResponseEntity.status(status)
				.body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
	}
}
