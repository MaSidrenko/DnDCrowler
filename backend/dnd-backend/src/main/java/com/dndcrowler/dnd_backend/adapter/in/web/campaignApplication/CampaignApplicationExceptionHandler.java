package com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationAlreadyExistsException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignFullException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignMemberAlreadyExistsException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotRecruitingException;
import com.dndcrowler.dnd_backend.application.service.exception.CannotApplyToOwnCampaignException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterLevelNotAllowedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterNotFoundException;
import com.dndcrowler.dnd_backend.domain.exception.campaignApplication.InvalidCampaignApplicationStatusTransitionException;

@RestControllerAdvice(assignableTypes = CampaignApplicationController.class)
public class CampaignApplicationExceptionHandler {
	@ExceptionHandler({
			CampaignApplicationNotFoundException.class,
			CampaignNotFoundException.class,
			CharacterNotFoundException.class
	})
	public ResponseEntity<ProblemDetail> handleNotFound(
			RuntimeException exception) {
		return problem(HttpStatus.NOT_FOUND, exception);
	}

	@ExceptionHandler({
			CampaignApplicationAccessDeniedException.class,
			CampaignAccessDeniedException.class,
			CharacterAccessDeniedException.class
	})
	public ResponseEntity<ProblemDetail> handleAccessDenied(
			RuntimeException exception) {
		return problem(HttpStatus.FORBIDDEN, exception);
	}

	@ExceptionHandler({
			CampaignApplicationAlreadyExistsException.class,
			CampaignMemberAlreadyExistsException.class,
			CampaignNotRecruitingException.class,
			CampaignFullException.class,
			CannotApplyToOwnCampaignException.class,
			InvalidCampaignApplicationStatusTransitionException.class
	})
	public ResponseEntity<ProblemDetail> handleConflict(
			RuntimeException exception) {
		return problem(HttpStatus.CONFLICT, exception);
	}

	@ExceptionHandler({
			CharacterLevelNotAllowedException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ProblemDetail> handleBadRequest(
			RuntimeException exception) {
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
