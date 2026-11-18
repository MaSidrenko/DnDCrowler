package com.dndcrowler.dnd_backend.application.port.in.query;

import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

public record CharacterSearchCriteria(
		String name,
		Race race,
		CharacterClass characterClass) {
	public CharacterSearchCriteria {
		name = name == null || name.isBlank()
				? null
				: name.trim();
	}

}
