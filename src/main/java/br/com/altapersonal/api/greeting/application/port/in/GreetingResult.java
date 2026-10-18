package br.com.altapersonal.api.greeting.application.port.in;

import java.time.Instant;
import java.util.UUID;

import br.com.altapersonal.api.greeting.domain.Greeting;

/**
 * Saída dos casos de uso. Só tipos simples, para os adapters de entrada não dependerem do domínio.
 */
public record GreetingResult(UUID id, String name, String message, Instant createdAt) {

    public static GreetingResult from(Greeting greeting) {
        return new GreetingResult(
                greeting.id().value(), greeting.name().value(), greeting.message(), greeting.createdAt());
    }
}
