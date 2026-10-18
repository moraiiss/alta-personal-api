package br.com.altapersonal.api.greeting.domain;

public record PersonName(String value) {

    public static final int MAX_LENGTH = 100;

    public PersonName {
        if (value == null || value.isBlank()) {
            throw new InvalidPersonNameException("O nome é obrigatório");
        }
        value = value.strip();
        // Conta code points, não chars: um emoji ocupa dois chars em UTF-16
        if (value.codePointCount(0, value.length()) > MAX_LENGTH) {
            throw new InvalidPersonNameException("O nome deve ter no máximo " + MAX_LENGTH + " caracteres");
        }
    }
}
