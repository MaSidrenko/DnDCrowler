package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request.ConfirmEmailRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request.ResendEmailRequest;
import com.dndcrowler.dnd_backend.application.port.in.ConfirmEmailUseCase;
import com.dndcrowler.dnd_backend.application.port.in.ResendEmailVerificationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ResendEmailVerificationCommand;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth/email")
public class ConfirmEmailController {
	private final ConfirmEmailUseCase confirmUseCase;
	private final ResendEmailVerificationUseCase resendUseCase;

	public ConfirmEmailController(ConfirmEmailUseCase confirmUseCase, ResendEmailVerificationUseCase resendUseCase) {
		this.confirmUseCase = confirmUseCase;
		this.resendUseCase = resendUseCase;
	}

	@PostMapping("/confirm")
	public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmEmailRequest request) {
		ConfirmEmailCommand command = new ConfirmEmailCommand(request.email(), request.code());
		
		boolean confirmed = confirmUseCase.confirmEmail(command);

		if(!confirmed) {
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.noContent().build();
	}

	@PostMapping("/resend")
	public ResponseEntity<Void> resend(@Valid @RequestBody ResendEmailRequest request) {
		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand(request.email());

		resendUseCase.resend(command);

		return ResponseEntity.noContent().build();
	}
	
	
}
