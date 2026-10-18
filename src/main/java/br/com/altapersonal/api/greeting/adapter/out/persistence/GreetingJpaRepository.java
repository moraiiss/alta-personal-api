package br.com.altapersonal.api.greeting.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface GreetingJpaRepository extends JpaRepository<GreetingJpaEntity, UUID> {
}
