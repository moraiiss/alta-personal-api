package br.com.altapersonal.api.greeting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class GreetingTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00.123456789Z"), ZoneOffset.UTC);

    @Test
    void createBuildsMessageFromName() {
        Greeting greeting = Greeting.create(new PersonName("Maria"), clock);

        assertThat(greeting.message()).isEqualTo("Olá, Maria!");
    }

    @Test
    void createGeneratesIdAndTruncatesCreatedAtToMicros() {
        Greeting greeting = Greeting.create(new PersonName("Maria"), clock);

        assertThat(greeting.id()).isNotNull();
        assertThat(greeting.createdAt()).isEqualTo(Instant.parse("2026-10-17T12:00:00.123456Z"));
    }

    @Test
    void restoreKeepsGivenValues() {
        GreetingId id = GreetingId.generate();
        PersonName name = new PersonName("Maria");
        Instant createdAt = Instant.parse("2026-10-17T12:00:00Z");

        Greeting greeting = Greeting.restore(id, name, createdAt);

        assertThat(greeting.id()).isEqualTo(id);
        assertThat(greeting.name()).isEqualTo(name);
        assertThat(greeting.createdAt()).isEqualTo(createdAt);
    }
}
