package br.com.altapersonal.api.greeting.application.port.in;

public interface CreateGreetingUseCase {

    GreetingResult create(CreateGreetingCommand command);
}
