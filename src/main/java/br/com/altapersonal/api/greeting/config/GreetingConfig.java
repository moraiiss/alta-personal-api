package br.com.altapersonal.api.greeting.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GetGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.out.GreetingRepository;
import br.com.altapersonal.api.greeting.application.service.CreateGreetingService;
import br.com.altapersonal.api.greeting.application.service.GetGreetingService;

/**
 * Único ponto da feature que conhece os services concretos. Os casos de uso ficam sem anotações
 * de estereótipo e são expostos ao resto da aplicação só pelas portas de entrada.
 */
@Configuration
public class GreetingConfig {

    @Bean
    CreateGreetingUseCase createGreetingUseCase(GreetingRepository greetingRepository, Clock clock) {
        return new CreateGreetingService(greetingRepository, clock);
    }

    @Bean
    GetGreetingUseCase getGreetingUseCase(GreetingRepository greetingRepository) {
        return new GetGreetingService(greetingRepository);
    }
}
