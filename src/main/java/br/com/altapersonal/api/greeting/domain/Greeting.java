package br.com.altapersonal.api.greeting.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Greeting {

    private final GreetingId id;
    private final PersonName name;
    private final Instant createdAt;

    private Greeting(GreetingId id, PersonName name, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * Cria uma saudação nova. O instante é truncado em microssegundos, a precisão do Postgres,
     * para o valor devolvido na criação ser o mesmo lido depois do banco.
     */
    public static Greeting create(PersonName name, Clock clock) {
        return new Greeting(GreetingId.generate(), name, clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    /**
     * Reconstrói uma saudação já existente (ex.: lida do banco), sem gerar id nem data.
     */
    public static Greeting restore(GreetingId id, PersonName name, Instant createdAt) {
        return new Greeting(id, name, createdAt);
    }

    public GreetingId id() {
        return id;
    }

    public PersonName name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String message() {
        return "Olá, " + name.value() + "!";
    }
}
