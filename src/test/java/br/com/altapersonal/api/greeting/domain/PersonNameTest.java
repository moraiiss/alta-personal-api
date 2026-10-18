package br.com.altapersonal.api.greeting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PersonNameTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(new PersonName("  Maria  ").value()).isEqualTo("Maria");
    }

    @Test
    void stripsInvisibleCharactersAtTheEdges() {
        assertThat(new PersonName("\u00A0Maria\u200B\u202F").value()).isEqualTo("Maria");
    }

    @ParameterizedTest
    @NullSource
    // Inclui espaço sem quebra (U+00A0, U+202F) e espaço de largura zero (U+200B), comuns em texto colado
    @ValueSource(strings = {"", "   ", "\u00A0", "\u200B", "\u202F \u00A0"})
    void rejectsMissingName(String value) {
        assertThatThrownBy(() -> new PersonName(value))
                .isInstanceOf(InvalidPersonNameException.class)
                .hasMessage("O nome é obrigatório");
    }

    @Test
    void acceptsMaxLengthInCodePoints() {
        String name = "😀".repeat(PersonName.MAX_LENGTH);

        assertThat(new PersonName(name).value()).isEqualTo(name);
    }

    @Test
    void rejectsAboveMaxLength() {
        assertThatThrownBy(() -> new PersonName("a".repeat(101)))
                .isInstanceOf(InvalidPersonNameException.class)
                .hasMessage("O nome deve ter no máximo 100 caracteres");
    }
}
