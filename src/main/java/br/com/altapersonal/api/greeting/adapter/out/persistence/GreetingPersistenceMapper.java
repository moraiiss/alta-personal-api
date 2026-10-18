package br.com.altapersonal.api.greeting.adapter.out.persistence;

import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.GreetingId;
import br.com.altapersonal.api.greeting.domain.PersonName;

final class GreetingPersistenceMapper {

    private GreetingPersistenceMapper() {
    }

    static GreetingJpaEntity toEntity(Greeting greeting) {
        return new GreetingJpaEntity(greeting.id().value(), greeting.name().value(), greeting.createdAt());
    }

    static Greeting toDomain(GreetingJpaEntity entity) {
        return Greeting.restore(
                new GreetingId(entity.getId()), new PersonName(entity.getName()), entity.getCreatedAt());
    }
}
