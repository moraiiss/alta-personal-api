package br.com.altapersonal.api.greeting.application.port.in;

import java.util.UUID;

public interface GetGreetingUseCase {

    GreetingResult getById(UUID id);
}
