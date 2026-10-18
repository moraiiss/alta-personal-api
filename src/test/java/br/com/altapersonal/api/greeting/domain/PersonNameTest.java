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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
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
