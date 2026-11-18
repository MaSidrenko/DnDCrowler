package com.dndcrowler.dnd_backend.application.service.Character;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dndcrowler.dnd_backend.application.port.in.CharacterUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateSkillCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.application.port.out.Character.CharacterRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;

@Service
public class CharacterService implements CharacterUseCase {
	private final CharacterRepositoryPort characterRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;

	public CharacterService(CharacterRepositoryPort characterRepositoryPort,
			UserRepositoryPort userRepositoryPort) {
		this.characterRepositoryPort = characterRepositoryPort;
		this.userRepositoryPort = userRepositoryPort;
	}

	@Override
	@Transactional
	public Character createCharacter(CreateCharacterCommand command) {
		userRepositoryPort.findById(command.currentUserId())
				.orElseThrow(() -> new UserNotFoundException(command.currentUserId()));

		List<Skills> skills = command.skills()
				.stream()
				.map(CreateSkillCommand::toDomain)
				.toList();

		Character newChar = Character.create(
				command.name(),
				command.description(),
				command.currentUserId(),
				command.race(),
				command.abilityScores(),
				skills,
				command.characterClass(),
				command.visibility());

		return characterRepositoryPort.save(newChar);
	}

	@Override
	public List<Character> getAllCharacter() {
		return characterRepositoryPort.findAll();
	}

	@Override
	public Character getCharacterById(Long id, Long currentUserId) {
		Character character = characterRepositoryPort.findById(id).orElseThrow(
				() -> new CharacterNotFoundException(id));

		boolean isOwner = character.ownerID().equals(currentUserId);
		boolean isPublic = character.characterVisibility() == CharacterVisibility.PUBLIC;

		if (!isPublic && !isOwner) {
			throw new CharacterNotFoundException(id);
		}

		return character;
	}

	@Override
	public List<Character> getCharacterByVisibility(CharacterVisibility visibility) {
		return characterRepositoryPort.findCharacterByVisibility(visibility);
	}

	@Override
	public List<Character> getMyCharacters(Long ownerId) {
		return characterRepositoryPort.findByOwnerId(ownerId);
	}

	@Override
	public List<Character> getPublicCharacters() {
		return characterRepositoryPort.findCharacterByVisibility(CharacterVisibility.PUBLIC);
	}

	@Override
	@Transactional
	public Character updateCharacter(Long id, UpdateCharacterCommand command) {
		Character existingCharacter = characterRepositoryPort.findById(id)
				.orElseThrow(() -> new CharacterNotFoundException(id));

		if (!existingCharacter.ownerID().equals(command.currentUserId())) {
			throw new CharacterAccessDeniedException();
		}

		List<Skills> skills = command.skills()
				.stream()
				.map(CreateSkillCommand::toDomain)
				.toList();
		Character newDataCharacter = new Character(id,
				command.name(), command.description(), existingCharacter.ownerID(),
				command.level(), command.race(), command.abilityScores(), skills,
				command.characterClass(), command.visibility());

		return characterRepositoryPort.save(newDataCharacter);
	}

	@Override
	public List<Character> searchPublicCharacters(CharacterSearchCriteria characterSearchCriteria) {
		return  characterRepositoryPort.findPublic(characterSearchCriteria);
	}
}
