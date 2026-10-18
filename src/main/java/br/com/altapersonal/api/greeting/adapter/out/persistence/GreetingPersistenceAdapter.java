package br.com.altapersonal.api.greeting.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.altapersonal.api.greeting.application.port.out.GreetingRepository;
import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.GreetingId;

@Component
public class GreetingPersistenceAdapter implements GreetingRepository {

    private final GreetingJpaRepository jpaRepository;

    GreetingPersistenceAdapter(GreetingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Greeting greeting) {
        jpaRepository.save(GreetingPersistenceMapper.toEntity(greeting));
    }

    @Override
    public Optional<Greeting> findById(GreetingId id) {
        return jpaRepository.findById(id.value()).map(GreetingPersistenceMapper::toDomain);
    }
}
