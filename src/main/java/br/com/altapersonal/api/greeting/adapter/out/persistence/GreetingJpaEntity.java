package br.com.altapersonal.api.greeting.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representação da tabela {@code greeting}. Não é o modelo de domínio: só existe dentro deste adapter.
 */
@Entity
@Table(name = "greeting")
class GreetingJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GreetingJpaEntity() {
    }

    GreetingJpaEntity(UUID id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
