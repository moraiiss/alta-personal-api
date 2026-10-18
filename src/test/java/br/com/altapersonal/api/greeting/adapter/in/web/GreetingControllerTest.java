package br.com.altapersonal.api.greeting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingCommand;
import br.com.altapersonal.api.greeting.application.port.in.CreateGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GetGreetingUseCase;
import br.com.altapersonal.api.greeting.application.port.in.GreetingNotFoundException;
import br.com.altapersonal.api.greeting.application.port.in.GreetingResult;
import br.com.altapersonal.api.greeting.domain.InvalidPersonNameException;

@WebMvcTest(GreetingController.class)
class GreetingControllerTest {

    private static final UUID ID = UUID.fromString("0199f0a5-6a3e-7c1d-9b2f-4e5d6c7b8a90");
    private static final GreetingResult RESULT =
            new GreetingResult(ID, "Maria", "Olá, Maria!", Instant.parse("2026-10-17T12:00:00Z"));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateGreetingUseCase createGreeting;

    @MockitoBean
    private GetGreetingUseCase getGreeting;

    @Test
    void createReturns201WithLocation() {
        given(createGreeting.create(any())).willReturn(RESULT);

        assertThat(mvc.post().uri("/greetings").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria\"}"))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/greetings/" + ID)
                .bodyJson().isLenientlyEqualTo("""
                        {"id": "%s", "name": "Maria", "message": "Olá, Maria!", "createdAt": "2026-10-17T12:00:00Z"}
                        """.formatted(ID));
        verify(createGreeting).create(new CreateGreetingCommand("Maria"));
    }

    @Test
    void getReturns200() {
        given(getGreeting.getById(ID)).willReturn(RESULT);

        assertThat(mvc.get().uri("/greetings/{id}", ID))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"id": "%s", "name": "Maria", "message": "Olá, Maria!"}
                        """.formatted(ID));
    }

    @Test
    void invalidNameReturns400ProblemDetail() {
        given(createGreeting.create(any())).willThrow(new InvalidPersonNameException("O nome é obrigatório"));

        assertThat(mvc.post().uri("/greetings").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").isEqualTo("O nome é obrigatório");
    }

    @Test
    void missingBodyReturns400ProblemDetail() {
        assertThat(mvc.post().uri("/greetings").contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void malformedJsonReturns400ProblemDetail() {
        assertThat(mvc.post().uri("/greetings").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void nonUuidIdReturns400ProblemDetail() {
        assertThat(mvc.get().uri("/greetings/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void missingGreetingReturns404ProblemDetail() {
        given(getGreeting.getById(ID)).willThrow(new GreetingNotFoundException(ID));

        assertThat(mvc.get().uri("/greetings/{id}", ID))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").isEqualTo("Saudação " + ID + " não encontrada");
    }
}
