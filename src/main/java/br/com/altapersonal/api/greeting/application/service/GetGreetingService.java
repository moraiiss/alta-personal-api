package br.com.altapersonal.api.greeting.application.service;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import br.com.altapersonal.api.greeting.application.port.in.GetGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GreetingNotFoundException;
import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;
import br.com.altapersonal.api.greeting.application.port.out.GreetingRepository;
import br.com.altapersonal.api.greeting.domain.GreetingId;

@Transactional(readOnly = true)
public class GetGreetingService implements GetGreetingUseCase {

    private final GreetingRepository repository;

    public GetGreetingService(GreetingRepository repository) {
        this.repository = repository;
    }

    @Override
    public GreetingResult getById(UUID id) {
        return repository.findById(new GreetingId(id))
                .map(GreetingResult::from)
                .orElseThrow(() -> new GreetingNotFoundException(id));
    }
}
