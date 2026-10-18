# Arquitetura da API

Guia obrigatório para implementar qualquer feature nesta API. Ele foi escrito para ser seguido à risca, inclusive por agentes de IA. As regras de dependência daqui são verificadas por `src/test/java/br/com/altapersonal/api/ArchitectureTest.java`: se você violar uma, o build quebra. Nesse caso, corrija o código, não a regra.

A implementação de referência é a feature `greeting` (`br.com.altapersonal.api.greeting`). Na dúvida, copie o que ela faz.

## 1. Por que esta estrutura existe

O problema que ela evita: services gigantes que misturam regra de negócio com JPA e HTTP, difíceis de manter e de testar. A resposta é a arquitetura hexagonal (ports & adapters) com conceitos de arquitetura limpa:

- O **domínio** guarda as regras e é Java puro.
- Os **casos de uso** orquestram o domínio. Cada um é uma classe pequena e não conhece HTTP nem banco.
- Os **adapters** traduzem o mundo externo (HTTP, Postgres) para as portas e de volta.

Isso permite testar regra e caso de uso em milissegundos, sem subir o Spring, e trocar detalhes de infraestrutura sem tocar na regra.

## 2. Mapa de pacotes

Pacote por feature e, dentro de cada feature, pacote por camada:

```
br.com.altapersonal.api
├── AltaPersonalApiApplication
├── shared/                         # transversal, sem regra de feature
│   ├── domain/                     # DomainException (erro de regra → 400)
│   ├── application/                # NotFoundException (→ 404)
│   ├── adapter/in/web/             # GlobalExceptionHandler (Problem Details)
│   └── config/                     # ClockConfig (bean de java.time.Clock)
└── <feature>/                      # ex.: greeting
    ├── domain/                     # agregado, value objects, exceções de regra
    ├── application/
    │   ├── port/in/                # XxxUseCase, XxxCommand, XxxResult, exceções do contrato
    │   ├── port/out/               # XxxRepository (porta de saída)
    │   └── service/                # XxxService: um por caso de uso
    ├── adapter/
    │   ├── in/web/                 # XxxController, XxxRequest, XxxResponse
    │   └── out/persistence/        # XxxJpaEntity, XxxJpaRepository, XxxPersistenceAdapter, XxxPersistenceMapper
    └── config/                     # XxxConfig: monta os casos de uso como @Bean
```

Todo subpacote direto de `br.com.altapersonal.api`, exceto `shared`, é uma feature.

## 3. O que cada camada pode conhecer

| # | Regra | Teste no `ArchitectureTest` |
|---|-------|-----------------------------|
| 1 | `domain` depende só de `java.*` e de outros pacotes `domain` (incluindo `shared.domain`). Nada de Spring, Jakarta, Hibernate ou Jackson. | `domainIsFrameworkFree` |
| 2 | `application` depende só de `java.*`, `domain`, `application` e `org.springframework.transaction.annotation` (o `@Transactional`). Nada de `@Service`/`@Component`. | `applicationOnlyUsesTransactionalFromSpring` |
| 3 | `adapter.in` fala só com `application.port.in`. Não importa `domain` (exceto `shared.domain`), `application.service`, `application.port.out` nem `adapter.out`. E `adapter.out` não importa `adapter.in`. | `webAdapterTalksOnlyToInputPorts`, `outputAdaptersDoNotDependOnInputAdapters` |
| 4 | `jakarta.persistence` e `org.springframework.data` só aparecem em `adapter.out.persistence`. | `persistenceStaysInPersistenceAdapter` |
| 5 | Uma feature não importa outra, exceto pelo `application.port.in` dela. Todas podem usar `shared`. | `featuresDoNotDependOnEachOther` |
| 6 | Só `config` (e os próprios services) conhecem as classes de `application.service`. | `onlyConfigKnowsServices` |

As dependências apontam sempre para dentro: adapters → portas → domínio.

## 4. Passo a passo para criar uma feature

Siga nesta ordem, com TDD: escreva o teste, veja falhar, implemente. Os nomes abaixo usam `Student` como exemplo.

1. **Domínio** (`student/domain`): o agregado e os value objects, com as regras.
   - O agregado é uma classe `final`, sem setters, com construtor privado e duas fábricas:
     - `create(...)`: nasce um novo e aplica as regras; gera id e data.
     - `restore(...)`: reconstrói a partir da persistência, sem gerar nada.
   - Value objects são `record`s que validam no construtor compacto e lançam uma subclasse de `DomainException`.
   - O tempo vem de um `java.time.Clock` recebido como parâmetro, nunca de `Instant.now()`.
   - Em `create`, trunque instantes para microssegundos (`clock.instant().truncatedTo(ChronoUnit.MICROS)`). O Postgres guarda microssegundos e o relógio do sistema pode ter nanos; sem o truncamento, o valor devolvido na criação difere do lido depois. Cubra isso com um teste de domínio que usa `Clock.fixed` com nanos.
2. **Portas** (`student/application/port`):
   - `in/`: uma interface por caso de uso (`CreateStudentUseCase`) com um único método, mais os `record`s `XxxCommand` e `XxxResult` com **tipos simples** (`String`, `UUID`, `Instant`...), nunca tipos do domínio. Exceções que fazem parte do contrato (ex.: `StudentNotFoundException extends NotFoundException`) também ficam aqui.
   - `out/`: a porta de saída do agregado (`StudentRepository`), com o que os casos de uso precisam.
3. **Casos de uso** (`student/application/service`): uma classe por caso de uso (`CreateStudentService implements CreateStudentUseCase`), anotada com `@Transactional` (`readOnly = true` para leitura). Ela converte o command em value objects, chama o domínio, usa a porta de saída e devolve `XxxResult.from(agregado)`.
4. **Adapter de persistência** (`student/adapter/out/persistence`):
   - `StudentJpaEntity` (package-private), separada do domínio;
   - `StudentJpaRepository extends JpaRepository` (package-private);
   - `StudentPersistenceMapper` (package-private, métodos estáticos `toEntity`/`toDomain`);
   - `@Component StudentPersistenceAdapter implements StudentRepository`.
5. **Adapter web** (`student/adapter/in/web`): `@RestController` que só traduz HTTP ↔ command/result, com `XxxRequest` e `XxxResponse` em `record`. Sem Bean Validation: a regra mora no domínio. Na criação, devolva `201` com `Location`.
6. **Config** (`student/config`): `@Configuration StudentConfig` com um `@Bean` por caso de uso, com o tipo de retorno na porta de entrada.
7. **Migration**: `make migration name=create_student` e escreva o SQL. O Hibernate roda com `ddl-auto: validate`, então entidade sem migration não sobe.
8. **Testes**: veja a seção 7. Rode `make test`; o `ArchitectureTest` precisa continuar verde.

Trechos da referência (`greeting`):

```java
// domain: value object que valida a si mesmo
public record PersonName(String value) {
    public static final int MAX_LENGTH = 100;
    public PersonName {
        value = value == null ? "" : stripInvisible(value); // remove espaços, inclusive U+00A0 e U+200B, das pontas
        if (value.isEmpty()) {
            throw new InvalidPersonNameException("O nome é obrigatório");
        }
        // ...
    }
}

// domain: fábrica de criação, com o tempo vindo do Clock e truncado para a precisão do Postgres
public static Greeting create(PersonName name, Clock clock) {
    return new Greeting(GreetingId.generate(), name, clock.instant().truncatedTo(ChronoUnit.MICROS));
}

// application: um caso de uso por classe, sem estereótipo do Spring
@Transactional
public class CreateGreetingService implements CreateGreetingUseCase {
    public GreetingResult create(CreateGreetingCommand command) {
        Greeting greeting = Greeting.create(new PersonName(command.name()), clock);
        repository.save(greeting);
        return GreetingResult.from(greeting);
    }
}

// config: único lugar que conhece o service concreto
@Bean
CreateGreetingUseCase createGreetingUseCase(GreetingRepository greetingRepository, Clock clock) {
    return new CreateGreetingService(greetingRepository, clock);
}
```

## 5. Convenções de nomes

| Peça | Nome | Pacote |
|------|------|--------|
| Porta de entrada | `XxxUseCase` (ex.: `CreateStudentUseCase`) | `application.port.in` |
| Entrada / saída do caso de uso | `XxxCommand` / `XxxResult` | `application.port.in` |
| Implementação do caso de uso | `XxxService` (ex.: `CreateStudentService`) | `application.service` |
| Porta de saída | `XxxRepository` | `application.port.out` |
| Entidade JPA | `XxxJpaEntity` | `adapter.out.persistence` |
| Repositório Spring Data | `XxxJpaRepository` | `adapter.out.persistence` |
| Implementação da porta de saída | `XxxPersistenceAdapter` | `adapter.out.persistence` |
| Conversão domínio ↔ JPA | `XxxPersistenceMapper` | `adapter.out.persistence` |
| Controller / DTOs HTTP | `XxxController` / `XxxRequest` / `XxxResponse` | `adapter.in.web` |
| Montagem dos beans | `XxxConfig` | `config` |

Outras convenções:

- Sem Lombok e sem MapStruct: use `record` e mapeamento manual.
- Nomes de código em inglês. Mensagens para o usuário em português.

## 6. Erros

- Violação de regra: lance uma subclasse de `shared.domain.DomainException` → **400**.
- Recurso inexistente: lance uma subclasse de `shared.application.NotFoundException` → **404**.
- O `GlobalExceptionHandler` converte as duas em Problem Details (RFC 9457, `application/problem+json`), com a mensagem da exceção em `detail`.
- JSON malformado e parâmetro de path com tipo inválido (ex.: `/greetings/abc`) já viram 400 em Problem Details pela classe base `ResponseEntityExceptionHandler`.
- Não crie handlers por feature: estenda as bases.

## 7. Testes por camada

| Camada | Tipo | Como |
|--------|------|------|
| `domain` | unitário puro | JUnit 5 + AssertJ, sem Spring. Use `Clock.fixed(...)` para o tempo. |
| `application` | unitário puro | Fake em memória da porta de saída (ex.: `InMemoryGreetingRepository` em `src/test`) e `Clock.fixed`. Prefira fakes a Mockito: são mais legíveis e reutilizáveis. |
| `adapter.in.web` | fatia | `@WebMvcTest(XxxController.class)` + `MockMvcTester`, com `@MockitoBean` nas portas de entrada. Cubra os status de sucesso e os de erro em Problem Details. |
| `adapter.out.persistence` | fatia | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(XxxPersistenceAdapter.class)`, contra o Postgres do compose. Use `TestEntityManager.flush()`/`clear()` antes de ler, para a leitura vir do banco. O truncamento para micros é coberto no teste de domínio, porque aqui o agregado chega via `restore`. |
| arquitetura | ArchUnit | `ArchitectureTest`, que já cobre features novas. |
| contexto | `@SpringBootTest` | `contextLoads` garante que tudo se liga. |

Rode tudo com `make test`.

## 8. Quando segregar portas (ISP)

Comece com uma porta de saída por agregado (`XxxRepository`). Só divida em interfaces menores (ex.: `LoadStudentPort`, `SaveStudentPort`) quando a porta crescer e casos de uso diferentes passarem a usar subconjuntos diferentes dela. Antes disso, segregar é só cerimônia.

O mesmo vale para camadas e abstrações em geral: cada interface, mapper ou classe precisa ter uma função clara.

## 9. A feature de referência `greeting` e como removê-la

`greeting` é um domínio falso, criado só como modelo:

- `POST /greetings` com `{"name": "Maria"}` → 201 com `Location`;
- `GET /greetings/{id}` → 200 ou 404.

Quando a primeira feature real existir, ela pode ser apagada:

1. Apague `src/main/java/br/com/altapersonal/api/greeting` e `src/test/java/br/com/altapersonal/api/greeting`.
2. **Não** apague nem altere a migration `V..._create_greeting.sql`, porque ela já foi aplicada. Crie uma nova com `make migration name=drop_greeting` contendo `drop table greeting;`.
3. Atualize as referências a `greeting` neste documento para a nova feature de referência. Mantenha os trechos de código da seção 4.
