package br.com.altapersonal.api.greeting.application.service;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingCommand;
import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;
import br.com.altapersonal.api.greeting.application.port.out.GreetingRepository;
import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.PersonName;

@Transactional
public class CreateGreetingService implements CreateGreetingUseCase {

    private final GreetingRepository repository;
    private final Clock clock;

    public CreateGreetingService(GreetingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public GreetingResult create(CreateGreetingCommand command) {
        Greeting greeting = Greeting.create(new PersonName(command.name()), clock);
        repository.save(greeting);
        return GreetingResult.from(greeting);
    }
}
