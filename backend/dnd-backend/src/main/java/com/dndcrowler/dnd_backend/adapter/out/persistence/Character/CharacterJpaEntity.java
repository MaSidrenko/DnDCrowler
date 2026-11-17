package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "characters")
public class CharacterJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String description;
	private Long ownerId;
	private int level;
	@Enumerated(EnumType.STRING)
	private Race race;
	private int strength;
	private int dexterity;
	private int constitution;
	private int intelligence;
	private int wisdom;
	private int charisma;
	@OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchSize(size = 25)
	private List<SkillJpaEntity> skills = new ArrayList<>();
	@Enumerated(EnumType.STRING)
	private CharacterClass characterClass;
	@Column(name = "visibility", nullable = false)
	@Enumerated(EnumType.STRING)
	private CharacterVisibility characterVisibility;

	void addSkill(SkillJpaEntity skill) {
		skills.add(skill);
		skill.setCharacter(this);
	}

	protected CharacterJpaEntity() {
	}

	public CharacterJpaEntity(Character character) {
		this.id = character.id();
		this.name = character.name();
		this.description = character.description();
		this.ownerId = character.ownerID();
		this.level = character.level();
		this.race = character.race();

		AbilityScores scores = character.abilityScores();
		this.strength = scores.strength();
		this.dexterity = scores.dexterity();
		this.constitution = scores.constitution();
		this.intelligence = scores.intelligence();
		this.wisdom = scores.wisdom();
		this.charisma = scores.charisma();

		this.characterClass = character.characterClass();
		this.characterVisibility = character.characterVisibility();

		character.skills().stream()
				.map(SkillJpaEntity::new)
				.forEach(this::addSkill);

	}

	public Character toDomain() {
		AbilityScores scores = new AbilityScores(strength,
				dexterity, constitution, intelligence,
				wisdom, charisma);

		List<Skills> domainSkills = skills.stream()
				.map(SkillJpaEntity::toDomain)
				.toList();

		return new Character(id, name, description,
				ownerId, level, race, scores, domainSkills,
				characterClass, characterVisibility);
	}

	void updateFrom(Character character) {
		this.name = character.name();
		this.description = character.description();
		this.level = character.level();
		this.race = character.race();

		AbilityScores scores = character.abilityScores();
		this.strength = scores.strength();
		this.dexterity = scores.dexterity();
		this.constitution = scores.constitution();
		this.intelligence = scores.intelligence();
		this.wisdom = scores.wisdom();
		this.charisma = scores.charisma();

		this.characterClass = character.characterClass();
		this.characterVisibility = character.characterVisibility();

		skills.clear();

		character.skills().stream()
			.map(SkillJpaEntity::new)
			.forEach(this::addSkill);

	}
}
