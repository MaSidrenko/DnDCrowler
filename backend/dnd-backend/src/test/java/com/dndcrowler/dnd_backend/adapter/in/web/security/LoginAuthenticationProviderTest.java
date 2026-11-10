package com.dndcrowler.dnd_backend.adapter.in.web.security;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.dndcrowler.dnd_backend.application.port.in.LoginUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.LoginUserCommand;
import com.dndcrowler.dnd_backend.application.service.exception.EmailNotVerifiedException;
import com.dndcrowler.dnd_backend.application.service.exception.InvalidCredentialsException;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
public class LoginAuthenticationProviderTest {

	@Mock
	private LoginUserUseCase loginUserUseCase;

	@InjectMocks
	private LoginAuthenticationProvider provider;

	@Test
	void authenticatesUserAndCreatesPrincipal() {
		LoginUserCommand command = new LoginUserCommand("alice@example.com", "secret123");

		User user = verifiedUser();

		when(loginUserUseCase.loginUser(command)).thenReturn(user);

		Authentication request = UsernamePasswordAuthenticationToken
				.unauthenticated(command.email(), command.password());

		Authentication result = provider.authenticate(request);

		assertTrue(result.isAuthenticated());
		assertNull(result.getCredentials());

		assertEquals(new AuthenticatedUserPrincipal(user.id(), user.username(), user.email(), user.role()),
				result.getPrincipal());

		assertEquals(Set.of(new SimpleGrantedAuthority("ROLE_USER")),
				Set.copyOf(result.getAuthorities()));

		verify(loginUserUseCase).loginUser(command);
	}

	@Test
	void translatesInvalidCredentials() {
		LoginUserCommand command = new LoginUserCommand("alice@example.com", "wrong-password");

		when(loginUserUseCase.loginUser(command))
				.thenThrow(new InvalidCredentialsException());

		Authentication request = UsernamePasswordAuthenticationToken.unauthenticated(command.email(),
				command.password());

		BadCredentialsException result = assertThrows(
			BadCredentialsException.class, 
			() -> provider.authenticate(request)
		);

		assertInstanceOf(InvalidCredentialsException.class, result.getCause());
	}

	@Test
	void translatesUnverifiedEmail() {
		LoginUserCommand command =
			new LoginUserCommand("alice@example.com", "secret123");
		
		when(loginUserUseCase.loginUser(command))
			.thenThrow(new EmailNotVerifiedException());

		Authentication request =
			UsernamePasswordAuthenticationToken.unauthenticated(command.email(), command.password());

		DisabledException result = assertThrows(DisabledException.class, 
			() -> provider.authenticate(request)
		);

		assertInstanceOf(EmailNotVerifiedException.class, result.getCause());
	}

	@Test 
	void supportsUsernamePasswordAuthentication() {
		assertTrue(provider.supports(
			UsernamePasswordAuthenticationToken.class
		));

		assertFalse(provider.supports(
			TestingAuthenticationToken.class
		));
	}

	private User verifiedUser() {
		Instant now = Instant.now();

		return new User(
			1L,
			"Alice",
			"alice@example.com",
			UserRole.USER,
			now,
			now,
			now
		);
	}
}
