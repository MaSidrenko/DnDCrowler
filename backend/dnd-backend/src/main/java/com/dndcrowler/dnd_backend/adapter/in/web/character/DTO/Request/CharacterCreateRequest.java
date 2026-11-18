package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CharacterCreateRequest(
		@NotBlank 
		String name,
		@NotBlank 
		String description,
		@NotNull 
		Race race,
		@NotNull 
		@Valid 
		AbilityScoresRequest abilityScores,
		@NotNull 
		List<@NotNull @Valid CreateSkillRequest> skills,
		@NotNull 
		CharacterClass characterClass,
		@NotNull 
		CharacterVisibility visibility) {
	public CreateCharacterCommand toCommand(Long currentUserId) {
		return new CreateCharacterCommand(currentUserId,
				name,
				description,
				race,
				abilityScores.toDomain(),
				skills.stream()
						.map(CreateSkillRequest::toCommand)
						.toList(),
				characterClass,
				visibility);
	}
}