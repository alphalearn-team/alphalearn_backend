package com.example.demo.game.imposter.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImposterLobbyCodeGeneratorTest {

    private final ImposterLobbyCodeGenerator generator = new ImposterLobbyCodeGenerator();

    @Test
    void generatesEightCharacterUppercaseCodeFromExpectedCharset() {
        String code = generator.generate();

        assertThat(code).hasSize(8);
        assertThat(code).matches("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");
    }
}
