package br.com.altapersonal.api.greeting.domain;

import java.util.Objects;
import java.util.UUID;

public record GreetingId(UUID value) {

    public GreetingId {
        Objects.requireNonNull(value, "value");
    }

    public static GreetingId generate() {
        return new GreetingId(UUID.randomUUID());
    }
}
