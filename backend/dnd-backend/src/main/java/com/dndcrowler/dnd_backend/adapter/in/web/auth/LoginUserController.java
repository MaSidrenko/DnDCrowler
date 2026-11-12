package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request.LoginRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Response.CurrentUserResponse;
import com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Response.LoginResponse;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class LoginUserController {
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	public LoginUserController(
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			SessionAuthenticationStrategy sessionAuthenticationStrategy) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
				request.email(), request.password());

		Authentication authentication = authenticationManager.authenticate(authenticationRequest);

		sessionAuthenticationStrategy.onAuthentication(
				authentication,
				httpRequest,
				httpResponse);

		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);

		securityContextRepository.saveContext(
				securityContext,
				httpRequest,
				httpResponse);

		AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();

		return ResponseEntity.ok(
				new LoginResponse(
						principal.userId(),
						principal.username(),
						principal.email(),
						principal.role()));
	}

	@GetMapping("/me")
	public CurrentUserResponse currentUserResponse(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CurrentUserResponse.from(principal);
	}
}
