package com.dndcrowler.dnd_backend.application.service.Character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateSkillCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.application.port.out.Character.CharacterRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;
import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;
import com.dndcrowler.dnd_backend.domain.model.Character.Stat;
import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;
import com.dndcrowler.dnd_backend.domain.model.Skills.StatBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {
	private static final Long OWNER_ID = 10L;
	private static final Long OTHER_USER_ID = 20L;
	private static final Long CHARACTER_ID = 100L;
	private static final AbilityScores DEFAULT_SCORES =
			AbilityScores.create(10, 12, 14, 16, 11, 13);

	@Mock
	CharacterRepositoryPort characterRepositoryPort;

	@Mock
	UserRepositoryPort userRepositoryPort;

	@InjectMocks
	CharacterService service;

	@Test
	void createsCharacterForExistingUser() {
		CreateCharacterCommand command = createCommand(OWNER_ID);
		when(userRepositoryPort.findById(OWNER_ID))
				.thenReturn(Optional.of(user(OWNER_ID)));
		when(characterRepositoryPort.save(any(Character.class)))
				.thenAnswer(invocation -> withId(invocation.getArgument(0), CHARACTER_ID));

		Character result = service.createCharacter(command);

		assertEquals(CHARACTER_ID, result.id());
		ArgumentCaptor<Character> captor = ArgumentCaptor.forClass(Character.class);
		verify(characterRepositoryPort).save(captor.capture());
		Character characterToSave = captor.getValue();
		assertNull(characterToSave.id());
		assertEquals(OWNER_ID, characterToSave.ownerID());
		assertEquals(1, characterToSave.level());
		assertEquals(command.name(), characterToSave.name());
		assertEquals(command.abilityScores(), characterToSave.abilityScores());
		assertEquals(1, characterToSave.skills().size());
		assertEquals(command.skills().getFirst().name(), characterToSave.skills().getFirst().name());
		verify(userRepositoryPort).findById(OWNER_ID);
	}

	@Test
	void rejectsCreationWhenUserDoesNotExist() {
		CreateCharacterCommand command = createCommand(OWNER_ID);
		when(userRepositoryPort.findById(OWNER_ID)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> service.createCharacter(command));

		verifyNoInteractions(characterRepositoryPort);
	}

	@Test
	void returnsPublicCharacterToAnotherUser() {
		Character character = character(
				CHARACTER_ID, OWNER_ID, CharacterVisibility.PUBLIC);
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character));

		Character result = service.getCharacterById(CHARACTER_ID, OTHER_USER_ID);

		assertSame(character, result);
	}

	@Test
	void returnsPrivateCharacterToOwner() {
		Character character = character(
				CHARACTER_ID, OWNER_ID, CharacterVisibility.PRIVATE);
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character));

		Character result = service.getCharacterById(CHARACTER_ID, OWNER_ID);

		assertSame(character, result);
	}

	@Test
	void hidesPrivateCharacterFromAnotherUser() {
		Character character = character(
				CHARACTER_ID, OWNER_ID, CharacterVisibility.PRIVATE);
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.of(character));

		assertThrows(CharacterNotFoundException.class,
				() -> service.getCharacterById(CHARACTER_ID, OTHER_USER_ID));
	}

	@Test
	void rejectsReadingCharacterThatDoesNotExist() {
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.empty());

		assertThrows(CharacterNotFoundException.class,
				() -> service.getCharacterById(CHARACTER_ID, OWNER_ID));
	}

	@Test
	void ownerCanUpdateCharacter() {
		Character existing = character(
				CHARACTER_ID, OWNER_ID, CharacterVisibility.PRIVATE);
		UpdateCharacterCommand command = updateCommand(OWNER_ID);
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.of(existing));
		when(characterRepositoryPort.save(any(Character.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Character result = service.updateCharacter(CHARACTER_ID, command);

		assertEquals(CHARACTER_ID, result.id());
		assertEquals(OWNER_ID, result.ownerID());
		assertEquals(command.name(), result.name());
		assertEquals(command.description(), result.description());
		assertEquals(command.level(), result.level());
		assertEquals(command.visibility(), result.characterVisibility());
		assertEquals(1, result.skills().size());
		verify(characterRepositoryPort).save(result);
	}

	@Test
	void anotherUserCannotUpdateCharacter() {
		Character existing = character(
				CHARACTER_ID, OWNER_ID, CharacterVisibility.PRIVATE);
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.of(existing));

		assertThrows(CharacterAccessDeniedException.class,
				() -> service.updateCharacter(
						CHARACTER_ID, updateCommand(OTHER_USER_ID)));

		verify(characterRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsUpdateWhenCharacterDoesNotExist() {
		when(characterRepositoryPort.findById(CHARACTER_ID))
				.thenReturn(Optional.empty());

		assertThrows(CharacterNotFoundException.class,
				() -> service.updateCharacter(
						CHARACTER_ID, updateCommand(OWNER_ID)));

		verify(characterRepositoryPort, never()).save(any());
	}

	@Test
	void delegatesCombinedPublicSearchCriteria() {
		CharacterSearchCriteria criteria = new CharacterSearchCriteria(
				"mage", Race.ELF, CharacterClass.WIZARD);
		List<Character> expected = List.of(
				character(CHARACTER_ID, OWNER_ID, CharacterVisibility.PUBLIC));
		when(characterRepositoryPort.findPublic(criteria)).thenReturn(expected);

		List<Character> result = service.searchPublicCharacters(criteria);

		assertSame(expected, result);
		verify(characterRepositoryPort).findPublic(criteria);
	}

	@Test
	void getsCharactersOnlyForRequestedOwner() {
		List<Character> ownedCharacters = List.of(
				character(CHARACTER_ID, OWNER_ID, CharacterVisibility.PRIVATE));
		when(characterRepositoryPort.findByOwnerId(OWNER_ID))
				.thenReturn(ownedCharacters);

		List<Character> result = service.getMyCharacters(OWNER_ID);

		assertSame(ownedCharacters, result);
		verify(characterRepositoryPort).findByOwnerId(OWNER_ID);
	}

	private CreateCharacterCommand createCommand(Long currentUserId) {
		return new CreateCharacterCommand(
				currentUserId,
				"Aria",
				"Test character",
				Race.ELF,
				DEFAULT_SCORES,
				List.of(skillCommand()),
				CharacterClass.WIZARD,
				CharacterVisibility.PRIVATE);
	}

	private UpdateCharacterCommand updateCommand(Long currentUserId) {
		return new UpdateCharacterCommand(
				currentUserId,
				"Updated Aria",
				"Updated character",
				3,
				Race.HALF_ELF,
				AbilityScores.create(12, 14, 16, 18, 10, 8),
				List.of(skillCommand()),
				CharacterClass.MONK,
				CharacterVisibility.PUBLIC);
	}

	private CreateSkillCommand skillCommand() {
		return new CreateSkillCommand(
				"Arcane Knowledge",
				"Increases intelligence",
				ActivationType.PASSIVE,
				List.of(new StatBonus(Stat.INT, 2)),
				UsagePolicy.NONE);
	}

	private Character character(
			Long id,
			Long ownerId,
			CharacterVisibility visibility) {
		Skills skill = skillCommand().toDomain();
		return new Character(
				id,
				"Aria",
				"Test character",
				ownerId,
				1,
				Race.ELF,
				DEFAULT_SCORES,
				List.of(skill),
				CharacterClass.WIZARD,
				visibility);
	}

	private Character withId(Character character, Long id) {
		return new Character(
				id,
				character.name(),
				character.description(),
				character.ownerID(),
				character.level(),
				character.race(),
				character.abilityScores(),
				character.skills(),
				character.characterClass(),
				character.characterVisibility());
	}

	private User user(Long id) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new User(
				id,
				"character-owner",
				"owner-" + id + "@example.com",
				UserRole.USER,
				now,
				now,
				now);
	}
}
