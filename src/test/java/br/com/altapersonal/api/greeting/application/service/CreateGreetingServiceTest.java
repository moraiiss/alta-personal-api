package br.com.altapersonal.api.greeting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import br.com.altapersonal.api.greeting.application.InMemoryGreetingRepository;
import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingCommand;
import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;
import br.com.altapersonal.api.greeting.domain.GreetingId;
import br.com.altapersonal.api.greeting.domain.InvalidPersonNameException;

class CreateGreetingServiceTest {

    private final InMemoryGreetingRepository repository = new InMemoryGreetingRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC);
    private final CreateGreetingService service = new CreateGreetingService(repository, clock);

    @Test
    void createsAndSavesGreeting() {
        GreetingResult result = service.create(new CreateGreetingCommand("Maria"));

        assertThat(result.name()).isEqualTo("Maria");
        assertThat(result.message()).isEqualTo("Olá, Maria!");
        assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-10-17T12:00:00Z"));
        assertThat(repository.findById(new GreetingId(result.id()))).isPresent();
    }

    @Test
    void rejectsInvalidNameWithoutSaving() {
        assertThatThrownBy(() -> service.create(new CreateGreetingCommand(" ")))
                .isInstanceOf(InvalidPersonNameException.class);
        assertThat(repository.count()).isZero();
    }
}
