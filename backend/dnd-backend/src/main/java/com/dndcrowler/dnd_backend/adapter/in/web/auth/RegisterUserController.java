package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request.RegisterUserRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Response.RegisterUserResponse;
import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.domain.model.User.User;

@RestController
@RequestMapping("/api/auth")
public class RegisterUserController {
	private final RegisterUserUseCase useCase;

	public RegisterUserController(RegisterUserUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterUserResponse> register(@RequestBody RegisterUserRequest entity) {
		RegisterUserCommand command = new RegisterUserCommand(entity.userName(), entity.email(), entity.password());

		User newUser = useCase.registerUser(command);

		return ResponseEntity.status(201)
				.body(new RegisterUserResponse(newUser.id(), newUser.username(), newUser.email()));
	}

}
