package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dndcrowler.dnd_backend.application.port.out.Character.CharacterRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

@Component
public class CharacterPersistenceAdapter implements CharacterRepositoryPort {
	private final SpringDataCharacterRepository repository;

	public CharacterPersistenceAdapter(SpringDataCharacterRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional 
	public Character save(Character newCharacter) {
		if (newCharacter.id() == null) {
			return repository.saveAndFlush(new CharacterJpaEntity(newCharacter)).toDomain();
		}

		CharacterJpaEntity entity = repository.findById(newCharacter.id())
				.orElseThrow();

		entity.updateFrom(newCharacter);
		repository.flush();

		return  entity.toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Character> findById(Long id) {
		return repository.findById(id).map(CharacterJpaEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findByOwnerId(Long ownerId) {
		return repository.findByOwnerId(ownerId).stream()
				.map(CharacterJpaEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findAll() {
		return repository.findAll().stream()
				.map(CharacterJpaEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findCharacterByClassAndVisibility(CharacterClass characterClass,
			CharacterVisibility visibility) {
		return repository.findByCharacterClassAndCharacterVisibility(characterClass, visibility)
				.stream().map(CharacterJpaEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findCharacterByNameAndVisibility(String name, CharacterVisibility visibility) {
		return repository.findByNameContainingIgnoreCaseAndCharacterVisibility(name, visibility)
				.stream().map(CharacterJpaEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findCharacterByRaceAndVisibility(Race race, CharacterVisibility visibility) {
		return repository.findByRaceAndCharacterVisibility(race, visibility)
				.stream().map(CharacterJpaEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findCharacterByVisibility(CharacterVisibility visibility) {
		return repository.findByCharacterVisibility(visibility)
				.stream().map(CharacterJpaEntity::toDomain).toList();
	}

}
