package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.Character.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCharacterRequest(
		@NotBlank
		String name,
		@NotBlank 
		String description,
		@NotNull 
		Race race,
		@Min(1)
		@Max(20)
		int level,
		@NotNull 
		@Valid 
		AbilityScoresRequest abilityScores,
		@NotNull 
		List<@NotNull @Valid CreateSkillRequest> skills,
		@NotNull 
		CharacterClass characterClass,
		@NotNull 
		CharacterVisibility visibility) {
	public UpdateCharacterCommand toCommand(Long currentUserId) {
		return new UpdateCharacterCommand(currentUserId,
				name, description,
				level,
				race,
				abilityScores.toDomain(),
				skills.stream()
						.map(CreateSkillRequest::toCommand)
						.toList(),
				characterClass,
				visibility);
	}
}
