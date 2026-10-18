package br.com.altapersonal.api.greeting.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingCommand;
import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GetGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;

/**
 * Só traduz HTTP para os casos de uso e de volta. Validação e regra ficam no domínio.
 */
@RestController
@RequestMapping("/greetings")
public class GreetingController {

    private final CreateGreetingUseCase createGreeting;
    private final GetGreetingUseCase getGreeting;

    GreetingController(CreateGreetingUseCase createGreeting, GetGreetingUseCase getGreeting) {
        this.createGreeting = createGreeting;
        this.getGreeting = getGreeting;
    }

    @PostMapping
    ResponseEntity<GreetingResponse> create(@RequestBody CreateGreetingRequest request) {
        GreetingResult result = createGreeting.create(new CreateGreetingCommand(request.name()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(GreetingResponse.from(result));
    }

    @GetMapping("/{id}")
    GreetingResponse getById(@PathVariable UUID id) {
        return GreetingResponse.from(getGreeting.getById(id));
    }
}
