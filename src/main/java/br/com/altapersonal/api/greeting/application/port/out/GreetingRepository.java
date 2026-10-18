package br.com.altapersonal.api.greeting.application.port.out;

import java.util.Optional;

import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.GreetingId;

public interface GreetingRepository {

    void save(Greeting greeting);

    Optional<Greeting> findById(GreetingId id);
}
