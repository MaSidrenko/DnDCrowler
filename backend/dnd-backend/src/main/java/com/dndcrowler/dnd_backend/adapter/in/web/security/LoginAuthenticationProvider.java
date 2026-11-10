package com.dndcrowler.dnd_backend.adapter.in.web.security;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.dndcrowler.dnd_backend.application.port.in.LoginUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.LoginUserCommand;
import com.dndcrowler.dnd_backend.application.service.exception.EmailNotVerifiedException;
import com.dndcrowler.dnd_backend.application.service.exception.InvalidCredentialsException;
import com.dndcrowler.dnd_backend.domain.model.User.User;

@Component
public class LoginAuthenticationProvider implements AuthenticationProvider {
	private final LoginUserUseCase loginUserUseCase;

	public LoginAuthenticationProvider(LoginUserUseCase loginUserUseCase) {
		this.loginUserUseCase = loginUserUseCase;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String email = authentication.getName();

		Object credentials = authentication.getCredentials();
		String password = credentials == null ? null : credentials.toString();

		try {
			User user = loginUserUseCase.loginUser(new LoginUserCommand(email, password));
			AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);

			var authorities = List.of(
					new SimpleGrantedAuthority(
							"ROLE_" + user.role().name()));

			return UsernamePasswordAuthenticationToken
					.authenticated(principal, null, authorities);

		} catch (EmailNotVerifiedException exception) {
			throw new DisabledException(
					"Email address is not verified",
					exception);
		} catch (InvalidCredentialsException | IllegalArgumentException exception) {
			throw new BadCredentialsException(
					"Invalid email or password",
					exception);
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class
			.isAssignableFrom(authentication);
	}

}
