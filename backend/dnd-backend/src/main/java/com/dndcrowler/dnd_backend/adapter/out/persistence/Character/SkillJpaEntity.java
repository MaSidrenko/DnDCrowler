package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.Effect;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "skills")
public class SkillJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	@Enumerated(EnumType.STRING)
	private ActivationType activationType;
	@Enumerated(EnumType.STRING)
	private UsagePolicy usagePolicy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "character_id", nullable = false)
	private CharacterJpaEntity character;

	@BatchSize(size = 25)
	@OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SkillEffectJpaEntity> effects = new ArrayList<>();

	protected SkillJpaEntity() {
	}

	SkillJpaEntity(Skills skill) {
		this.id = skill.id();
		this.name = skill.name();
		this.description = skill.description();
		this.activationType = skill.activationType();
		this.usagePolicy = skill.usagePolicy();

		skill.effects().stream()
				.map(SkillEffectJpaEntity::new)
				.forEach(this::addEffect);
	}

	void setCharacter(CharacterJpaEntity character) {
		this.character = character;
	}

	void addEffect(SkillEffectJpaEntity effect) {
		effects.add(effect);
		effect.setSkill(this);
	}

	Skills toDomain() {
		List<Effect> domainEffects = effects.stream()
				.map(SkillEffectJpaEntity::toDomain)
				.toList();

		return new Skills(
				id, name, description,
				activationType, domainEffects,
				usagePolicy);
	}
}
