# Arquitetura hexagonal + "Hello World" de saudação: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar a estrutura hexagonal por feature e a feature de exemplo `greeting` (HTTP → caso de uso → domínio → JPA → Postgres), com regras de arquitetura verificadas por ArchUnit e documentadas em `docs/ARCHITECTURE.md`.

**Architecture:** Pacote por feature (`greeting/`) dividido em `domain` (Java puro), `application` (portas `in`/`out` + um service por caso de uso), `adapter.in.web`, `adapter.out.persistence` e `config` (monta os casos de uso como `@Bean`). Itens transversais em `shared/`. O ArchUnit quebra o build quando uma regra de dependência é violada.

**Tech Stack:** Java 25, Spring Boot 4.1.1 (Web MVC, Data JPA, Flyway), Jackson 3, Postgres 18, JUnit 5, AssertJ, Mockito, MockMvcTester, ArchUnit 1.5.1.

**Spec:** `docs/specs/2026-10-17-hexagonal-architecture-hello-world-design.md` (leia antes de começar; este plano não repete as justificativas)

## Global Constraints

- Pacote base `br.com.altapersonal.api`. Abreviações abaixo: `api.` = `br.com.altapersonal.api.`.
- `domain` só depende de `java.*` e `api.shared.domain`. Nada de Spring, Jakarta, Hibernate ou Jackson.
- `application` sem `@Service`/`@Component`. Do Spring, só `org.springframework.transaction.annotation` (`@Transactional`).
- Commands e resultados de `port.in` carregam só tipos simples (`String`, `UUID`, `Instant`). `adapter.in.web` não importa `domain`.
- Validação de regra só no domínio; o controller não usa Bean Validation.
- Sem Lombok e sem MapStruct; DTOs são `record`s; mapeamento manual.
- Nome: obrigatório, `trim`, no máximo **100 code points**. Mensagem: `"Olá, <nome>!"`, calculada, não persistida.
- Erros em Problem Details (RFC 9457): `DomainException` → 400, `NotFoundException` → 404, JSON malformado → 400, id que não é UUID → 400.
- Tabela `greeting`: `id uuid primary key`, `name varchar(100) not null`, `created_at timestamptz not null`, criada via `make migration name=create_greeting`. Nunca altere uma migration depois de aplicada.
- Versões fora do BOM do Spring Boot entram em `gradle/libs.versions.toml` com versão explícita.
- Testes rodam no container: `make up` antes de começar. Para um teste específico: `docker compose exec api ./gradlew --project-cache-dir /gradle-project-cache test --tests '<padrão>'` (abreviado abaixo como `GT --tests '<padrão>'`). Suíte completa: `make test`. Não use `docker compose run`.
- Branch `feat/hexagonal-architecture-hello-world`. Commits em Conventional Commits, em inglês, **sem `Co-Authored-By` nem qualquer atribuição**.

## Review Focus

1. Nome só com espaços (`"   "`) ou `null`/ausente (`{}`) → 400, nunca 500 nem nome vazio salvo. Teste: `PersonNameTest` (Task 1).
2. Nome com acentos/emoji: 100 code points são aceitos mesmo com mais de 100 `char`s; 101 code points são recusados. Teste: `PersonNameTest` (Task 1).
3. Espaços nas pontas (`"  Maria  "`) → nome `"Maria"` e mensagem `"Olá, Maria!"`. Teste: `PersonNameTest` e `GreetingTest` (Task 1).
4. `createdAt` da resposta do POST precisa ser igual ao do GET seguinte; o Postgres guarda microssegundos e o `Clock` do sistema pode ter nanos. `Greeting.create` trunca para micros. Teste: `GreetingTest` (Task 1).
5. POST sem corpo → 400 em Problem Details, não 500. Teste: `GreetingControllerTest` (Task 4).

---

### Task 1: Branch, docs da spec e domínio da saudação

**Files:**
- Create: `src/main/java/br/com/altapersonal/api/shared/domain/DomainException.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/domain/GreetingId.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/domain/PersonName.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/domain/InvalidPersonNameException.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/domain/Greeting.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/domain/PersonNameTest.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/domain/GreetingTest.java`

**Interfaces:**
- Produces:
  - `public abstract class DomainException extends RuntimeException` com `protected DomainException(String message)`
  - `public record GreetingId(UUID value)` (compact ctor: `Objects.requireNonNull`), `public static GreetingId generate()` → `UUID.randomUUID()`
  - `public record PersonName(String value)`, `public static final int MAX_LENGTH = 100`; compact ctor faz `trim` e lança `InvalidPersonNameException`
  - `public final class InvalidPersonNameException extends DomainException` com `InvalidPersonNameException(String message)`
  - `public final class Greeting`: `static Greeting create(PersonName name, Clock clock)`, `static Greeting restore(GreetingId id, PersonName name, Instant createdAt)`, acessores `GreetingId id()`, `PersonName name()`, `Instant createdAt()`, `String message()`

- [ ] **Step 1: Criar a branch e commitar a documentação já existente**

```bash
git switch -c feat/hexagonal-architecture-hello-world
git add CLAUDE.md docs/specs/2026-10-17-hexagonal-architecture-hello-world-design.md docs/plans/2026-10-17-hexagonal-architecture-hello-world.md
git commit -m "docs: add hexagonal architecture spec and plan"
```

- [ ] **Step 2: Escrever os testes que falham**

`PersonNameTest`:
- `trimsSurroundingWhitespace`: `new PersonName("  Maria  ").value()` é `"Maria"`
- `rejectsNull`, `rejectsBlank` (`""` e `"   "`): `assertThatThrownBy(...)` é `InvalidPersonNameException` com mensagem `"O nome é obrigatório"`
- `acceptsMaxLengthInCodePoints`: `"😀".repeat(100)` (200 `char`s) é aceito
- `rejectsAboveMaxLength`: `"a".repeat(101)` lança `InvalidPersonNameException` com mensagem `"O nome deve ter no máximo 100 caracteres"`

`GreetingTest` (com `Clock.fixed(Instant.parse("2026-10-17T12:00:00.123456789Z"), ZoneOffset.UTC)`):
- `createBuildsMessageFromName`: `Greeting.create(new PersonName("Maria"), clock).message()` é `"Olá, Maria!"`
- `createGeneratesIdAndTruncatesCreatedAtToMicros`: `id()` não é nulo; `createdAt()` é `Instant.parse("2026-10-17T12:00:00.123456Z")`
- `restoreKeepsGivenValues`: `restore(id, name, instant)` devolve exatamente esses `id()`, `name()`, `createdAt()`

- [ ] **Step 3: Rodar e ver falhar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.domain.*'`
Expected: FAIL na compilação (classes inexistentes)

- [ ] **Step 4: Implementar as classes de domínio listadas em Interfaces**

`Greeting`: construtor privado, campos `final`, sem setters. `create` usa `GreetingId.generate()` e `clock.instant().truncatedTo(ChronoUnit.MICROS)`. `message()` devolve `"Olá, " + name.value() + "!"`. A contagem de tamanho de `PersonName` usa `value.codePointCount(0, value.length())`, depois do `trim`.

- [ ] **Step 5: Rodar e ver passar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.domain.*'`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/altapersonal/api/shared src/main/java/br/com/altapersonal/api/greeting/domain src/test/java/br/com/altapersonal/api/greeting/domain
git commit -m "feat: add greeting domain model"
```

---

### Task 2: Casos de uso e portas

**Files:**
- Create: `src/main/java/br/com/altapersonal/api/shared/application/NotFoundException.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/port/in/CreateGreetingUseCase.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/port/in/CreateGreetingCommand.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/port/in/GetGreetingUseCase.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/port/in/GreetingResult.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/port/in/GreetingNotFoundException.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/port/out/GreetingRepository.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/service/CreateGreetingService.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/application/service/GetGreetingService.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/application/InMemoryGreetingRepository.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/application/service/CreateGreetingServiceTest.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/application/service/GetGreetingServiceTest.java`

**Interfaces:**
- Consumes: tudo de Task 1
- Produces:
  - `public abstract class NotFoundException extends RuntimeException` com `protected NotFoundException(String message)`
  - `public interface CreateGreetingUseCase { GreetingResult create(CreateGreetingCommand command); }`
  - `public record CreateGreetingCommand(String name)`
  - `public interface GetGreetingUseCase { GreetingResult getById(UUID id); }`
  - `public record GreetingResult(UUID id, String name, String message, Instant createdAt)` com `public static GreetingResult from(Greeting greeting)`
  - `public final class GreetingNotFoundException extends NotFoundException` com `GreetingNotFoundException(UUID id)`, mensagem `"Saudação " + id + " não encontrada"`
  - `public interface GreetingRepository { void save(Greeting greeting); Optional<Greeting> findById(GreetingId id); }`
  - `public class CreateGreetingService implements CreateGreetingUseCase`, ctor `(GreetingRepository repository, Clock clock)`, anotado `@Transactional`
  - `public class GetGreetingService implements GetGreetingUseCase`, ctor `(GreetingRepository repository)`, anotado `@Transactional(readOnly = true)`
  - (teste) `InMemoryGreetingRepository implements GreetingRepository`, apoiado em `Map<GreetingId, Greeting>`

- [ ] **Step 1: Escrever o fake e os testes que falham**

`CreateGreetingServiceTest` (repositório em memória + `Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC)`):
- `createsAndSavesGreeting`: `create(new CreateGreetingCommand("Maria"))` devolve `name` `"Maria"`, `message` `"Olá, Maria!"`, `createdAt` `2026-10-17T12:00:00Z`; `repository.findById(new GreetingId(result.id()))` está presente
- `rejectsInvalidNameWithoutSaving`: `create(new CreateGreetingCommand(" "))` lança `InvalidPersonNameException` e o repositório continua vazio

`GetGreetingServiceTest`:
- `returnsExistingGreeting`: após salvar um `Greeting.restore(...)` no fake, `getById(id)` devolve os mesmos `id`, `name`, `message`, `createdAt`
- `throwsWhenMissing`: `getById(UUID.randomUUID())` lança `GreetingNotFoundException`

- [ ] **Step 2: Rodar e ver falhar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.application.*'`
Expected: FAIL na compilação

- [ ] **Step 3: Implementar as portas, exceções e services listados em Interfaces**

Os services constroem os value objects (`new PersonName(command.name())`, `new GreetingId(id)`) e convertem a saída com `GreetingResult.from`.

- [ ] **Step 4: Rodar e ver passar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.application.*'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/altapersonal/api/shared/application src/main/java/br/com/altapersonal/api/greeting/application src/test/java/br/com/altapersonal/api/greeting/application
git commit -m "feat: add greeting use cases and ports"
```

---

### Task 3: Adapter de persistência e migration

**Files:**
- Create: `src/main/resources/db/migration/V<timestamp>__create_greeting.sql` (gerado por `make migration name=create_greeting`)
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/out/persistence/GreetingJpaEntity.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/out/persistence/GreetingJpaRepository.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/out/persistence/GreetingPersistenceMapper.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/out/persistence/GreetingPersistenceAdapter.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/adapter/out/persistence/GreetingPersistenceAdapterTest.java`

**Interfaces:**
- Consumes: `GreetingRepository`, `Greeting`, `GreetingId`, `PersonName`
- Produces:
  - `@Entity @Table(name = "greeting") class GreetingJpaEntity` (package-private): `@Id UUID id` (atribuído, sem geração), `String name` (`@Column(length = 100)`), `Instant createdAt` (`@Column(name = "created_at")`); construtor `protected` sem argumentos + construtor completo + getters
  - `interface GreetingJpaRepository extends JpaRepository<GreetingJpaEntity, UUID>` (package-private)
  - `final class GreetingPersistenceMapper` (package-private) com `static GreetingJpaEntity toEntity(Greeting)` e `static Greeting toDomain(GreetingJpaEntity)` (usa `Greeting.restore`)
  - `@Component public class GreetingPersistenceAdapter implements GreetingRepository`, ctor `(GreetingJpaRepository jpaRepository)`

- [ ] **Step 1: Criar e preencher a migration**

Run: `make migration name=create_greeting`. Preencha o arquivo gerado com o `create table greeting` das Global Constraints.

- [ ] **Step 2: Escrever o teste que falha**

`GreetingPersistenceAdapterTest`: `@DataJpaTest` (`org.springframework.boot.data.jpa.test.autoconfigure`), `@AutoConfigureTestDatabase(replace = Replace.NONE)` (`org.springframework.boot.jdbc.test.autoconfigure`), `@Import(GreetingPersistenceAdapter.class)`. Assim o teste usa o Postgres do compose, com as migrations do Flyway, e cada teste faz rollback.
- `savesAndFindsGreeting`: salva `Greeting.restore(GreetingId.generate(), new PersonName("Maria"), Instant.parse("2026-10-17T12:00:00.123456Z"))`; `findById` devolve id, nome e `createdAt` iguais
- `returnsEmptyWhenMissing`: `findById(GreetingId.generate())` é `Optional.empty()`

- [ ] **Step 3: Rodar e ver falhar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.adapter.out.persistence.*'`
Expected: FAIL na compilação

- [ ] **Step 4: Implementar a entidade, o repositório Spring Data, o mapper e o adapter**

Se o Spring Data ou o Hibernate recusarem tipos package-private, torne `public` apenas o tipo recusado.

- [ ] **Step 5: Rodar e ver passar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.adapter.out.persistence.*'`
Expected: PASS (o Hibernate valida o schema com `ddl-auto: validate`)

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration src/main/java/br/com/altapersonal/api/greeting/adapter/out src/test/java/br/com/altapersonal/api/greeting/adapter/out
git commit -m "feat: add greeting persistence adapter"
```

---

### Task 4: Adapter web, Problem Details e montagem dos beans

**Files:**
- Create: `src/main/java/br/com/altapersonal/api/shared/config/ClockConfig.java`
- Create: `src/main/java/br/com/altapersonal/api/shared/adapter/in/web/GlobalExceptionHandler.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/config/GreetingConfig.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/in/web/GreetingController.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/in/web/CreateGreetingRequest.java`
- Create: `src/main/java/br/com/altapersonal/api/greeting/adapter/in/web/GreetingResponse.java`
- Test: `src/test/java/br/com/altapersonal/api/greeting/adapter/in/web/GreetingControllerTest.java`

**Interfaces:**
- Consumes: `CreateGreetingUseCase`, `GetGreetingUseCase`, `CreateGreetingCommand`, `GreetingResult`, `GreetingNotFoundException`, `InvalidPersonNameException` (só no teste), `CreateGreetingService`, `GetGreetingService`, `GreetingRepository`
- Produces:
  - `@Configuration ClockConfig` com `@Bean Clock clock()` → `Clock.systemUTC()`
  - `@Configuration GreetingConfig` com `@Bean CreateGreetingUseCase createGreetingUseCase(GreetingRepository, Clock)` e `@Bean GetGreetingUseCase getGreetingUseCase(GreetingRepository)`
  - `@RestControllerAdvice GlobalExceptionHandler extends ResponseEntityExceptionHandler` com `@ExceptionHandler(DomainException.class) ProblemDetail handleDomain(DomainException)` (400, title `"Requisição inválida"`, detail = mensagem) e `@ExceptionHandler(NotFoundException.class) ProblemDetail handleNotFound(NotFoundException)` (404, title `"Recurso não encontrado"`, detail = mensagem). JSON malformado e `TypeMismatchException` já viram 400 em Problem Details pela classe base.
  - `public record CreateGreetingRequest(String name)`
  - `public record GreetingResponse(UUID id, String name, String message, Instant createdAt)` com `static GreetingResponse from(GreetingResult)`
  - `@RestController @RequestMapping("/greetings") GreetingController`, ctor `(CreateGreetingUseCase, GetGreetingUseCase)`:
    - `@PostMapping ResponseEntity<GreetingResponse> create(@RequestBody CreateGreetingRequest)` → 201, `Location` via `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")`
    - `@GetMapping("/{id}") GreetingResponse getById(@PathVariable UUID id)`

- [ ] **Step 1: Escrever o teste que falha**

`GreetingControllerTest`: `@WebMvcTest(GreetingController.class)` (`org.springframework.boot.webmvc.test.autoconfigure`), `MockMvcTester`, `@MockitoBean` para os dois casos de uso. Use `result = new GreetingResult(id, "Maria", "Olá, Maria!", Instant.parse("2026-10-17T12:00:00Z"))`.
- `createReturns201WithLocation`: POST `{"name":"Maria"}` → 201; `Location` é `http://localhost/greetings/{id}`; JSON `$.message` = `"Olá, Maria!"`, `$.createdAt` = `"2026-10-17T12:00:00Z"`; o caso de uso recebeu `new CreateGreetingCommand("Maria")`
- `getReturns200`: GET `/greetings/{id}` → 200 com `$.id`, `$.name`, `$.message`
- `invalidNameReturns400ProblemDetail`: o mock lança `new InvalidPersonNameException("O nome é obrigatório")` → 400, content type `application/problem+json`, `$.detail` = `"O nome é obrigatório"`
- `missingBodyReturns400ProblemDetail`: POST sem corpo → 400, `application/problem+json`
- `malformedJsonReturns400ProblemDetail`: POST `{"name":` → 400, `application/problem+json`
- `nonUuidIdReturns400ProblemDetail`: GET `/greetings/abc` → 400, `application/problem+json`
- `missingGreetingReturns404ProblemDetail`: o mock lança `new GreetingNotFoundException(id)` → 404, `application/problem+json`, `$.detail` = `"Saudação " + id + " não encontrada"`

- [ ] **Step 2: Rodar e ver falhar**

Run: `GT --tests 'br.com.altapersonal.api.greeting.adapter.in.web.*'`
Expected: FAIL na compilação

- [ ] **Step 3: Implementar as classes listadas em Interfaces**

- [ ] **Step 4: Rodar e ver passar, incluindo a montagem completa**

Run: `GT --tests 'br.com.altapersonal.api.greeting.adapter.in.web.*'` e depois `make test`
Expected: PASS em tudo, incluindo `contextLoads` (prova que `GreetingConfig`, `ClockConfig` e o adapter de persistência se ligam)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/altapersonal/api/shared src/main/java/br/com/altapersonal/api/greeting/config src/main/java/br/com/altapersonal/api/greeting/adapter/in src/test/java/br/com/altapersonal/api/greeting/adapter/in
git commit -m "feat: add greeting http endpoints with problem details"
```

---

### Task 5: Regras de arquitetura com ArchUnit

**Files:**
- Modify: `gradle/libs.versions.toml` (versão `archunit = "1.5.1"` e biblioteca `archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }`)
- Modify: `build.gradle` (`testImplementation libs.archunit.junit5`)
- Test: `src/test/java/br/com/altapersonal/api/ArchitectureTest.java`

**Interfaces:**
- Consumes: os pacotes criados nas Tasks 1–4

- [ ] **Step 1: Adicionar a dependência e escrever as regras**

`@AnalyzeClasses(packages = "br.com.altapersonal.api", importOptions = ImportOption.DoNotIncludeTests.class)`, um `@ArchTest static final ArchRule` por regra, cada um com `.because("...")` em português, citando a regra:
1. `domainIsFrameworkFree`: classes em `..domain..` só dependem de `java..` e `..domain..`
2. `applicationOnlyUsesTransactionalFromSpring`: classes em `..application..` só dependem de `java..`, `..domain..`, `..application..` e `org.springframework.transaction.annotation..`
3. `webAdapterTalksOnlyToInputPorts`: classes em `..adapter.in..` não dependem de `..adapter.out..`, `..application.service..`, `..application.port.out..`, nem de `..domain..` (exceto `br.com.altapersonal.api.shared.domain..`, que o `GlobalExceptionHandler` usa); e classes em `..adapter.out..` não dependem de `..adapter.in..`
4. `persistenceStaysInPersistenceAdapter`: nenhuma classe fora de `..adapter.out.persistence..` depende de `jakarta.persistence..` ou `org.springframework.data..`
5. `featuresDoNotDependOnEachOther`: `slices().matching("br.com.altapersonal.api.(*)..")` com `notDependOnEachOther()`, ignorando dependências para `br.com.altapersonal.api.shared..` e para `..application.port.in..`
6. `onlyConfigKnowsServices`: nenhuma classe fora de `..config..` e `..application.service..` depende de `..application.service..`

- [ ] **Step 2: Rodar e ver passar**

Run: `GT --tests 'br.com.altapersonal.api.ArchitectureTest'`
Expected: PASS

- [ ] **Step 3: Provar que as regras mordem**

Adicione temporariamente `import jakarta.persistence.Entity;` e um campo `private static final Class<?> X = Entity.class;` em `Greeting`. Rode o mesmo comando e confira FAIL citando as regras 1 e 4. Depois reverta com `git checkout src/main/java/br/com/altapersonal/api/greeting/domain/Greeting.java` e rode de novo para ver PASS.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml build.gradle src/test/java/br/com/altapersonal/api/ArchitectureTest.java
git commit -m "test: enforce hexagonal architecture rules with archunit"
```

---

### Task 6: `docs/ARCHITECTURE.md`, `CLAUDE.md` e verificação final

**Files:**
- Create: `docs/ARCHITECTURE.md`
- Modify: `CLAUDE.md` (seção "Estado do projeto" e nova referência ao `ARCHITECTURE.md`)

- [ ] **Step 1: Escrever `docs/ARCHITECTURE.md`**

Em português, para um agente de IA seguir, e autossuficiente (com trechos curtos de código) para continuar útil depois que a saudação for apagada. Seções, nesta ordem:
1. Por que esta estrutura existe: evitar services gigantes que misturam regra com JPA/HTTP.
2. Mapa de pacotes: a árvore da spec.
3. O que cada camada pode conhecer: as 6 regras, com a indicação de que `ArchitectureTest` as garante.
4. Passo a passo para criar uma feature: domínio → portas → caso de uso → adapters → config → migration (`make migration`) → testes.
5. Padrões e convenções de nomes: `XxxUseCase` / `XxxCommand` / `XxxResult` / `XxxService` / `XxxRepository` (porta) / `XxxJpaEntity` / `XxxJpaRepository` / `XxxPersistenceAdapter` / `XxxPersistenceMapper` / `XxxController` / `XxxRequest` / `XxxResponse` / `XxxConfig`; um caso de uso por classe; `create`/`restore` no agregado; value objects como `record`.
6. Erros: as bases `DomainException`/`NotFoundException` e o Problem Details.
7. Testes por camada: a tabela da spec, mais o fake em memória como padrão.
8. Quando segregar portas (ISP): só quando casos de uso passarem a usar subconjuntos diferentes.
9. Implementação de referência: `greeting/`. Como removê-la no futuro: apagar o código e criar uma nova migration `drop table greeting`, sem tocar na migration original.

- [ ] **Step 2: Atualizar o `CLAUDE.md`**

- "Estado do projeto": a estrutura hexagonal existe e `greeting` é a feature de exemplo.
- Nova seção curta "Arquitetura": leitura obrigatória de `docs/ARCHITECTURE.md` antes de implementar qualquer feature, e o `ArchitectureTest` quebra o build se as regras forem violadas. Não copie as regras para o `CLAUDE.md`.

- [ ] **Step 3: Verificação final**

- `make test` → todos os testes passam.
- Com `make up`:
  - `curl -si -X POST localhost:8080/greetings -H 'Content-Type: application/json' -d '{"name":"  Maria  "}'` → 201 com `Location` e `"message":"Olá, Maria!"`
  - um GET no `Location` devolvido → 200 com o mesmo `createdAt`
  - `curl -si localhost:8080/greetings/abc` → 400 `application/problem+json`
  - `curl -si -X POST localhost:8080/greetings -H 'Content-Type: application/json' -d '{}'` → 400 com `"detail":"O nome é obrigatório"`
  - `/swagger-ui.html` lista os dois endpoints

- [ ] **Step 4: Commit**

```bash
git add docs/ARCHITECTURE.md CLAUDE.md
git commit -m "docs: add architecture guide and reference it from claude.md"
```
