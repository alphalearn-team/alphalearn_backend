package com.example.demo.game.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GameLobbyCodeGeneratorTest {

    private final GameLobbyCodeGenerator generator = new GameLobbyCodeGenerator();

    @Test
    void generatesEightCharacterUppercaseCodeFromExpectedCharset() {
        String code = generator.generate();

        assertThat(code).hasSize(8);
        assertThat(code).matches("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");
    }
}
