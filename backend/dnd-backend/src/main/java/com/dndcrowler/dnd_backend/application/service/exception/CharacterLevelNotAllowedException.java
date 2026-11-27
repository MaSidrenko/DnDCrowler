package com.dndcrowler.dnd_backend.application.service.exception;

public class CharacterLevelNotAllowedException extends RuntimeException {
	public CharacterLevelNotAllowedException(
			Long characterId,
			int characterLevel,
			int minLevel,
			int maxLevel) {
		super("Character level is outside the campaign range: characterId="
				+ characterId + ", level=" + characterLevel
				+ ", allowedRange=" + minLevel + "-" + maxLevel);
	}
}
