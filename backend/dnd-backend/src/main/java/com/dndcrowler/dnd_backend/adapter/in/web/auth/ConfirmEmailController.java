package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request.ConfirmEmailRequest;
import com.dndcrowler.dnd_backend.application.port.in.ConfirmEmailUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth/email")
public class ConfirmEmailController {
	private final ConfirmEmailUseCase useCase;

	public ConfirmEmailController(ConfirmEmailUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping("/confirm")
	public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmEmailRequest request) {
		ConfirmEmailCommand command = new ConfirmEmailCommand(request.email(), request.code());
		
		boolean confirmed = useCase.confirmEmail(command);

		if(!confirmed) {
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.noContent().build();
	}
	
}
