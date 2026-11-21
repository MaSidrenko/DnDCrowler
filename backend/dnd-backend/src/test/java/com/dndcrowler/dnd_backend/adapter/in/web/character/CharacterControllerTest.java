package com.dndcrowler.dnd_backend.adapter.in.web.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CharacterUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterNotFoundException;
import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;
import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.DamageBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
class CharacterControllerTest {
	private static final Long OWNER_ID = 42L;
	private static final Long CHARACTER_ID = 100L;

	@Mock
	private CharacterUseCase useCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				OWNER_ID,
				"alice",
				"alice@example.com",
				UserRole.USER);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);

		mockMvc = MockMvcBuilders.standaloneSetup(new CharacterController(useCase))
				.setControllerAdvice(new CharacterExceptionHandler())
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createsCharacterForAuthenticatedUser() throws Exception {
		when(useCase.createCharacter(any(CreateCharacterCommand.class)))
				.thenReturn(character(CHARACTER_ID, OWNER_ID, 1, CharacterVisibility.PRIVATE));

		mockMvc.perform(post("/api/characters")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestJson("""
						{"type":"DAMAGE_BONUS","amount":4}
						""")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(CHARACTER_ID))
				.andExpect(jsonPath("$.ownerId").value(OWNER_ID))
				.andExpect(jsonPath("$.level").value(1))
				.andExpect(jsonPath("$.skills[0].effects[0].type").value("DAMAGE_BONUS"));

		ArgumentCaptor<CreateCharacterCommand> captor =
				ArgumentCaptor.forClass(CreateCharacterCommand.class);
		verify(useCase).createCharacter(captor.capture());
		assertEquals(OWNER_ID, captor.getValue().currentUserId());
		DamageBonus effect = assertInstanceOf(
				DamageBonus.class,
				captor.getValue().skills().getFirst().effects().getFirst());
		assertEquals(4, effect.amount());
	}

	@Test
	void updatesCharacterForAuthenticatedUser() throws Exception {
		when(useCase.updateCharacter(eq(CHARACTER_ID), any(UpdateCharacterCommand.class)))
				.thenReturn(character(CHARACTER_ID, OWNER_ID, 3, CharacterVisibility.PUBLIC));

		mockMvc.perform(put("/api/characters/{characterId}", CHARACTER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequestJson(3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CHARACTER_ID))
				.andExpect(jsonPath("$.level").value(3))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));

		ArgumentCaptor<UpdateCharacterCommand> captor =
				ArgumentCaptor.forClass(UpdateCharacterCommand.class);
		verify(useCase).updateCharacter(eq(CHARACTER_ID), captor.capture());
		assertEquals(OWNER_ID, captor.getValue().currentUserId());
		assertEquals(3, captor.getValue().level());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 21})
	void rejectsLevelOutsideAllowedRange(int level) throws Exception {
		mockMvc.perform(put("/api/characters/{characterId}", CHARACTER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequestJson(level)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(useCase);
	}

	@Test
	void rejectsStatBonusWithoutStat() throws Exception {
		mockMvc.perform(post("/api/characters")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestJson("""
						{"type":"STAT_BONUS","amount":2}
						""")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(useCase);
	}

	@Test
	void returnsForbiddenWhenAnotherUserUpdatesCharacter() throws Exception {
		when(useCase.updateCharacter(eq(CHARACTER_ID), any(UpdateCharacterCommand.class)))
				.thenThrow(new CharacterAccessDeniedException());

		mockMvc.perform(put("/api/characters/{characterId}", CHARACTER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequestJson(3)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void returnsNotFoundWhenCharacterDoesNotExist() throws Exception {
		when(useCase.getCharacterById(CHARACTER_ID, OWNER_ID))
				.thenThrow(new CharacterNotFoundException(CHARACTER_ID));

		mockMvc.perform(get("/api/characters/{characterId}", CHARACTER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void returnsCharactersOwnedByAuthenticatedUser() throws Exception {
		when(useCase.getMyCharacters(OWNER_ID))
				.thenReturn(List.of(
						character(CHARACTER_ID, OWNER_ID, 1, CharacterVisibility.PRIVATE)));

		mockMvc.perform(get("/api/characters/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(CHARACTER_ID))
				.andExpect(jsonPath("$[0].ownerId").value(OWNER_ID));

		verify(useCase).getMyCharacters(OWNER_ID);
	}

	@Test
	void returnsCharacterVisibleToAuthenticatedUser() throws Exception {
		when(useCase.getCharacterById(CHARACTER_ID, OWNER_ID))
				.thenReturn(character(
						CHARACTER_ID, OWNER_ID, 1, CharacterVisibility.PRIVATE));

		mockMvc.perform(get("/api/characters/{characterId}", CHARACTER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CHARACTER_ID));

		verify(useCase).getCharacterById(CHARACTER_ID, OWNER_ID);
	}

	@Test
	void passesAllPublicSearchFiltersToUseCase() throws Exception {
		when(useCase.searchPublicCharacters(any(CharacterSearchCriteria.class)))
				.thenReturn(List.of(
						character(CHARACTER_ID, OWNER_ID, 1, CharacterVisibility.PUBLIC)));

		mockMvc.perform(get("/api/characters/public")
				.param("name", "  aria  ")
				.param("race", "ELF")
				.param("characterClass", "WIZARD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(CHARACTER_ID))
				.andExpect(jsonPath("$[0].visibility").value("PUBLIC"));

		ArgumentCaptor<CharacterSearchCriteria> captor =
				ArgumentCaptor.forClass(CharacterSearchCriteria.class);
		verify(useCase).searchPublicCharacters(captor.capture());
		assertEquals("aria", captor.getValue().name());
		assertEquals(Race.ELF, captor.getValue().race());
		assertEquals(CharacterClass.WIZARD, captor.getValue().characterClass());
	}

	@Test
	void passesEmptyPublicSearchCriteriaWhenFiltersAreMissing() throws Exception {
		when(useCase.searchPublicCharacters(any(CharacterSearchCriteria.class)))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/characters/public"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		ArgumentCaptor<CharacterSearchCriteria> captor =
				ArgumentCaptor.forClass(CharacterSearchCriteria.class);
		verify(useCase).searchPublicCharacters(captor.capture());
		assertNull(captor.getValue().name());
		assertNull(captor.getValue().race());
		assertNull(captor.getValue().characterClass());
	}

	private Character character(
			Long id,
			Long ownerId,
			int level,
			CharacterVisibility visibility) {
		Skills skill = new Skills(
				200L,
				"Power Strike",
				"Deals additional damage",
				ActivationType.ACTIVE,
				List.of(new DamageBonus(4)),
				UsagePolicy.ONE_FIGHT);

		return new Character(
				id,
				"Aria",
				"Test character",
				ownerId,
				level,
				Race.ELF,
				AbilityScores.create(10, 12, 14, 16, 11, 13),
				List.of(skill),
				CharacterClass.WIZARD,
				visibility);
	}

	private String createRequestJson(String effectJson) {
		return """
				{
				  "name": "Aria",
				  "description": "Test character",
				  "race": "ELF",
				  "abilityScores": {
				    "strength": 10,
				    "dexterity": 12,
				    "constitution": 14,
				    "intelligence": 16,
				    "wisdom": 11,
				    "charisma": 13
				  },
				  "skills": [{
				    "name": "Power Strike",
				    "description": "Deals additional damage",
				    "activationType": "ACTIVE",
				    "effects": [%s],
				    "usagePolicy": "ONE_FIGHT"
				  }],
				  "characterClass": "WIZARD",
				  "visibility": "PRIVATE"
				}
				""".formatted(effectJson);
	}

	private String updateRequestJson(int level) {
		return """
				{
				  "name": "Aria",
				  "description": "Updated character",
				  "race": "ELF",
				  "level": %d,
				  "abilityScores": {
				    "strength": 10,
				    "dexterity": 12,
				    "constitution": 14,
				    "intelligence": 16,
				    "wisdom": 11,
				    "charisma": 13
				  },
				  "skills": [{
				    "name": "Power Strike",
				    "description": "Deals additional damage",
				    "activationType": "ACTIVE",
				    "effects": [{"type":"DAMAGE_BONUS","amount":4}],
				    "usagePolicy": "ONE_FIGHT"
				  }],
				  "characterClass": "WIZARD",
				  "visibility": "PUBLIC"
				}
				""".formatted(level);
	}
}
