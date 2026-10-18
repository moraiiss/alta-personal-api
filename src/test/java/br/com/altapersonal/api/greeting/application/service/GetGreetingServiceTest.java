package br.com.altapersonal.api.greeting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.altapersonal.api.greeting.application.InMemoryGreetingRepository;
import br.com.altapersonal.api.greeting.application.port.in.GreetingNotFoundException;
import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;
import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.GreetingId;
import br.com.altapersonal.api.greeting.domain.PersonName;

class GetGreetingServiceTest {

    private final InMemoryGreetingRepository repository = new InMemoryGreetingRepository();
    private final GetGreetingService service = new GetGreetingService(repository);

    @Test
    void returnsExistingGreeting() {
        Greeting greeting = Greeting.restore(
                GreetingId.generate(), new PersonName("Maria"), Instant.parse("2026-10-17T12:00:00Z"));
        repository.save(greeting);

        GreetingResult result = service.getById(greeting.id().value());

        assertThat(result).isEqualTo(new GreetingResult(
                greeting.id().value(), "Maria", "Olá, Maria!", Instant.parse("2026-10-17T12:00:00Z")));
    }

    @Test
    void throwsWhenMissing() {
        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(GreetingNotFoundException.class);
    }
}
