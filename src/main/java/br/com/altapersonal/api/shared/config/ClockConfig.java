package br.com.altapersonal.api.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    // Injetado onde houver regra dependente do tempo, para os testes poderem fixar o relógio
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
