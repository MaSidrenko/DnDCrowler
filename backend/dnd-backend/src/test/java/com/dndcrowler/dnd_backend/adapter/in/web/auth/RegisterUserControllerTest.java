package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dndcrowler.dnd_backend.application.port.in.RegisterUserUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.exception.user.InvalidUserEmailPatternException;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegisterUserControllerTest {
	@Mock
	private RegisterUserUseCase useCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new RegisterUserController(useCase))
				.setControllerAdvice(new RegisterUserExceptionHandler())
				.build();
	}

	@Test
	void returnsCreatedUserWithoutPassword() throws Exception {
		Instant now = Instant.now();
		when(useCase.registerUser(any(RegisterUserCommand.class)))
				.thenReturn(new User(42L, "alice", "alice@example.com", UserRole.USER, now, now, null));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userName":"alice","email":"alice@example.com","password":"secret123"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.userName").value("alice"))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist());

		ArgumentCaptor<RegisterUserCommand> command = ArgumentCaptor.forClass(RegisterUserCommand.class);
		verify(useCase).registerUser(command.capture());
		assertEquals("secret123", command.getValue().password());
	}

	@Test
	void rejectsBlankPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userName":"alice","email":"alice@example.com","password":" "}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(useCase);
	}

	@Test
	void rejectsInvalidEmail() throws Exception {
		when(useCase.registerUser(any(RegisterUserCommand.class)))
				.thenThrow(new InvalidUserEmailPatternException());

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userName":"alice","email":"invalid","password":"secret123"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void rejectsDuplicateEmail() throws Exception {
		when(useCase.registerUser(any(RegisterUserCommand.class)))
				.thenThrow(new EmailAlreadyRegisteredException());

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userName":"alice","email":"alice@example.com","password":"secret123"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
