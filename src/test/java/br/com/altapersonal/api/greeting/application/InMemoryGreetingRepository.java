package br.com.altapersonal.api.greeting.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import br.com.altapersonal.api.greeting.application.port.out.GreetingRepository;
import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.GreetingId;

/**
 * Fake da porta de saída para testes de caso de uso, sem Spring nem banco.
 */
public class InMemoryGreetingRepository implements GreetingRepository {

    private final Map<GreetingId, Greeting> greetings = new HashMap<>();

    @Override
    public void save(Greeting greeting) {
        greetings.put(greeting.id(), greeting);
    }

    @Override
    public Optional<Greeting> findById(GreetingId id) {
        return Optional.ofNullable(greetings.get(id));
    }

    public int count() {
        return greetings.size();
    }
}
