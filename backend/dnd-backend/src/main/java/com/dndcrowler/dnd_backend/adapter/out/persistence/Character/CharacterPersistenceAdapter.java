package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.application.port.out.Character.CharacterRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;

import jakarta.persistence.criteria.Predicate;

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

		return entity.toDomain();
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
	public List<Character> findCharacterByVisibility(CharacterVisibility visibility) {
		return repository.findByCharacterVisibility(visibility)
				.stream().map(CharacterJpaEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Character> findPublic(CharacterSearchCriteria characterSearchCriteria) {
		Specification<CharacterJpaEntity> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();

			predicates.add(builder.equal(
					root.get("characterVisibility"),
					CharacterVisibility.PUBLIC));

			if (characterSearchCriteria.name() != null) {
				predicates.add(builder.like(
						builder.lower(root.get("name")),
						"%" + characterSearchCriteria.name()
								.toLowerCase(Locale.ROOT) + "%"));
			}

			if (characterSearchCriteria.race() != null) {
				predicates.add(builder.equal(root.get("race"),
						characterSearchCriteria.race()));
			}

			if (characterSearchCriteria.characterClass() != null) {
				predicates.add(builder.equal(
						root.get("characterClass"),
						characterSearchCriteria.characterClass()));
			}

			return builder.and(
					predicates.toArray(Predicate[]::new));

		};

		return repository.findAll(specification)
				.stream()
				.map(CharacterJpaEntity::toDomain)
				.toList();
	}

}
