package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import com.dndcrowler.dnd_backend.domain.model.Character.Stat;
import com.dndcrowler.dnd_backend.domain.model.Skills.DamageBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.Effect;
import com.dndcrowler.dnd_backend.domain.model.Skills.StatBonus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table(name = "skill_effects")
public class SkillEffectJpaEntity {
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Enumerated(EnumType.STRING)
	private EffectType effectType;
	@Enumerated(EnumType.STRING)
	private Stat stat;
	private int amount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "skill_id", nullable = false)
	private SkillJpaEntity skill;

	protected SkillEffectJpaEntity() {}

	SkillEffectJpaEntity(Effect effect) {
		switch (effect) {
			case StatBonus bonus -> {
				this.effectType = EffectType.STAT_BONUS;
				this.stat = bonus.stat();
				this.amount = bonus.amount();
			}
			case DamageBonus bonus -> {
				this.effectType = EffectType.DAMAGE_BONUS;
				this.stat = null;
				this.amount = bonus.amount();
			}				
		}
	}

	void setSkill(SkillJpaEntity skill) {
		this.skill = skill;
	}

	Effect toDomain() {
		return switch(effectType) {
			case STAT_BONUS -> new StatBonus(stat, amount);
			case DAMAGE_BONUS -> new DamageBonus(amount);
		};
	}
}

enum EffectType {
	STAT_BONUS,
	DAMAGE_BONUS
}
