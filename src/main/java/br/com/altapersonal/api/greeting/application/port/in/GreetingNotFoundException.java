package br.com.altapersonal.api.greeting.application.port.in;

import java.util.UUID;

import br.com.altapersonal.api.shared.application.NotFoundException;

public final class GreetingNotFoundException extends NotFoundException {

    public GreetingNotFoundException(UUID id) {
        super("Saudação " + id + " não encontrada");
    }
}
