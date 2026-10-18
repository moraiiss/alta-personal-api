package br.com.altapersonal.api.greeting.domain;

public record PersonName(String value) {

    public static final int MAX_LENGTH = 100;

    public PersonName {
        value = value == null ? "" : stripInvisible(value);
        if (value.isEmpty()) {
            throw new InvalidPersonNameException("O nome é obrigatório");
        }
        // Conta code points, não chars: um emoji ocupa dois chars em UTF-16
        if (value.codePointCount(0, value.length()) > MAX_LENGTH) {
            throw new InvalidPersonNameException("O nome deve ter no máximo " + MAX_LENGTH + " caracteres");
        }
    }

    /**
     * Remove das pontas espaços e caracteres invisíveis. O {@link String#strip()} não cobre o espaço
     * sem quebra (U+00A0) nem o de largura zero (U+200B), comuns em texto colado da web.
     */
    private static String stripInvisible(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isInvisible(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isInvisible(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        return value.substring(start, end);
    }

    private static boolean isInvisible(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || Character.getType(codePoint) == Character.FORMAT;
    }
}
