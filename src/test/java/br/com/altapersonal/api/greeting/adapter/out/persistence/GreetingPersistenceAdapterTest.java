package br.com.altapersonal.api.greeting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

import br.com.altapersonal.api.greeting.domain.Greeting;
import br.com.altapersonal.api.greeting.domain.GreetingId;
import br.com.altapersonal.api.greeting.domain.PersonName;

// Usa o Postgres do compose, com as migrations do Flyway; cada teste faz rollback
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(GreetingPersistenceAdapter.class)
class GreetingPersistenceAdapterTest {

    @Autowired
    private GreetingPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndFindsGreeting() {
        Greeting greeting = Greeting.restore(
                GreetingId.generate(), new PersonName("Maria"), Instant.parse("2026-10-17T12:00:00.123456Z"));

        adapter.save(greeting);
        // Força o INSERT e esvazia o cache do Hibernate, para a leitura vir do banco
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findById(greeting.id())).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(greeting.id());
            assertThat(found.name()).isEqualTo(greeting.name());
            assertThat(found.createdAt()).isEqualTo(greeting.createdAt());
        });
    }

    @Test
    void returnsEmptyWhenMissing() {
        assertThat(adapter.findById(GreetingId.generate())).isEmpty();
    }
}
