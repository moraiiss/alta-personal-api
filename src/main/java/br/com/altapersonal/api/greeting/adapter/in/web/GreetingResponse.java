package br.com.altapersonal.api.greeting.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;

public record GreetingResponse(UUID id, String name, String message, Instant createdAt) {

    static GreetingResponse from(GreetingResult result) {
        return new GreetingResponse(result.id(), result.name(), result.message(), result.createdAt());
    }
}
