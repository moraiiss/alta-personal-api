# Arquitetura base (hexagonal) com o "Hello World" de saudação

Data: 2026-10-17
Status: aprovado

## Intenção

Definir a estrutura de código que todas as features futuras da API vão seguir, e entregar um exemplo completo e funcionando ("saudação") que sirva de modelo.

Quem vai escrever as próximas features são **agentes de IA**. Eles vão copiar o exemplo e seguir o documento de arquitetura. Por isso as regras precisam estar escritas de forma explícita e **verificadas automaticamente pelo build**, não só documentadas.

A dor que o usuário quer evitar, vinda de projetos anteriores: **services gigantes que misturam regra de negócio com JPA e HTTP**. Toda decisão abaixo existe para impedir isso: o domínio e os casos de uso não conhecem Spring Web, JPA nem HTTP, e cada caso de uso é uma classe pequena.

## Objetivos

- Uma estrutura de pacotes por feature com arquitetura hexagonal (ports & adapters) e conceitos de arquitetura limpa, seguindo SOLID sempre que possível.
- Uma feature de exemplo, `greeting`, que atravessa todas as camadas: HTTP → caso de uso → domínio → porta de saída → JPA → Postgres (com migration Flyway).
- Testes de cada camada no nível certo, sendo domínio e casos de uso testáveis sem subir o Spring.
- Um teste ArchUnit que quebra o build quando as regras de dependência são violadas.
- `docs/ARCHITECTURE.md` com as regras e o passo a passo para criar uma feature. O `CLAUDE.md` aponta para ele.

## Não-objetivos

- Nenhuma feature real do produto (aluno, treino etc.). A saudação é um domínio falso e pode ser apagada quando a primeira feature real existir.
- Autenticação/autorização, paginação, cache, eventos de domínio, mensageria, observabilidade.
- Multi-módulo Gradle. A separação é por pacotes, garantida pelo ArchUnit.

## Anti-objetivos (falha mesmo que funcione)

- Regra de negócio dentro de controller, entidade JPA ou adapter de persistência.
- Uma classe "Service" que concentra vários casos de uso.
- Domínio ou caso de uso importando `jakarta.persistence`, `org.springframework.web`, `org.springframework.data` ou classes de adapter.
- Uma entidade JPA usada como modelo de domínio ou devolvida na resposta HTTP.
- Regras de arquitetura que só existem no texto, sem teste que as garanta.
- Cerimônia que não se paga: camadas, interfaces ou mappers sem função clara. O exemplo precisa ser pequeno o bastante para um agente copiar sem errar.

## Restrições

- Stack existente: Java 25, Spring Boot 4.1.1, Gradle 9.8.1 com Version Catalog (`gradle/libs.versions.toml`), Spring Web MVC, Spring Data JPA, Flyway, Postgres 18, springdoc-openapi.
- Pacote base `br.com.altapersonal.api`.
- `ddl-auto: validate`: toda tabela nasce de migration Flyway, criada com `make migration name=...`.
- Testes rodam com `make test`, dentro do container, contra o Postgres do compose.
- Dependências fora do BOM do Spring Boot (ex.: ArchUnit) entram no catálogo com versão explícita.
- Convenções do `CLAUDE.md`: Conventional Commits em inglês, sem nenhuma co-autoria ou atribuição. Specs, plans e tasks ficam em `docs/specs`, `docs/plans` e `docs/tasks`.

## Decisões

Itens marcados com *(decisão da IA)* foram propostos por agentes e aceitos pelo usuário sem discussão. Os demais foram pedidos explícitos do usuário.

### Comportamento da saudação

- `POST /greetings` com corpo `{"name": "Maria"}` cria uma saudação. A resposta é `201 Created`, com header `Location: /greetings/{id}` e corpo `{"id": "...", "name": "Maria", "message": "Olá, Maria!", "createdAt": "..."}`.
- `GET /greetings/{id}` devolve `200` com o mesmo corpo, ou `404` se não existir. Um id que não é UUID válido (ex.: `/greetings/abc`) devolve `400` em Problem Details *(decisão do Claude)*. O path variable é `UUID`, e o erro de conversão é tratado no handler global.
- Regras do domínio:
  - o nome é obrigatório, sofre `trim` e tem no máximo 100 caracteres; violar isso gera erro de domínio e resposta 400;
  - a mensagem `"Olá, <nome>!"` é **calculada pelo domínio** e não é persistida, para mostrar que o comportamento mora no domínio;
  - o id é UUID gerado pelo domínio ao criar;
  - `createdAt` vem de um `java.time.Clock` injetado, para mostrar como tornar o tempo testável.

### Organização de pacotes

Por feature primeiro e, dentro de cada feature, por camada:

```
br.com.altapersonal.api
├── AltaPersonalApiApplication
├── shared/                         # transversal, sem regra de feature
│   ├── domain/                     # DomainException (base de erros de regra → 400)
│   ├── application/                # NotFoundException (base de "não encontrado" → 404)
│   ├── adapter/in/web/             # GlobalExceptionHandler (Problem Details)
│   └── config/                     # bean de Clock
└── greeting/
    ├── domain/                     # Greeting, GreetingId, PersonName, exceções de regra
    ├── application/
    │   ├── port/in/                # CreateGreetingUseCase, GetGreetingUseCase (+ Command/records)
    │   ├── port/out/               # GreetingRepository
    │   └── service/                # CreateGreetingService, GetGreetingService
    ├── adapter/
    │   ├── in/web/                 # GreetingController, CreateGreetingRequest, GreetingResponse
    │   └── out/persistence/        # GreetingJpaEntity, GreetingJpaRepository,
    │                               # GreetingPersistenceAdapter, GreetingPersistenceMapper
    └── config/                     # GreetingConfig (@Configuration que monta os casos de uso)
```

Os nomes de classe acima são a proposta. O builder pode ajustá-los se houver motivo, mas a divisão em pacotes e as responsabilidades são decididas.

### Regras de dependência (verificadas pelo ArchUnit)

1. `domain` depende apenas de `java.*` e de `shared.domain`. Nada de Spring, Jakarta, Hibernate ou Jackson.
2. `application` depende de `domain`, das próprias portas e de `shared.domain`/`shared.application`. Do Spring, **só** `org.springframework.transaction.annotation.Transactional` é permitido *(decisão do Claude)*: é a fronteira da transação, e fica no caso de uso.
3. `adapter.in.*` e `adapter.out.*` não dependem um do outro. O controller conversa só com as portas de entrada (`application.port.in`), nunca com os services concretos nem com persistência. `adapter.in.web` **não importa `domain`** *(decisão do Claude)*: commands e resultados de `port.in` carregam só tipos simples (`String`, `UUID`, `Instant`), e é o caso de uso que constrói os value objects (`PersonName`, `GreetingId`). Assim a validação de regra roda sempre dentro do caso de uso, e o controller fica só como tradutor.
4. Nada fora de `adapter.out.persistence` usa `jakarta.persistence` ou `org.springframework.data`.
5. Uma feature não importa pacotes internos de outra. Se precisar, usa apenas o `application.port.in` dela *(decisão do Claude; hoje só existe uma feature, mas a regra já nasce escrita de forma genérica para valer para as próximas)*.
6. `config` é o único lugar da feature que conhece as classes concretas para montar os beans.

### Padrões de código *(decisão do Claude)*

- **Um caso de uso por classe** (SRP), cada um atrás de uma interface de porta de entrada (DIP), com um único método público (ex.: `GreetingResult create(CreateGreetingCommand command)`). Esse é o antídoto direto para os "services gigantes".
- **Casos de uso sem `@Service`/`@Component`:** são registrados como `@Bean` em `GreetingConfig`. Assim o código da camada não depende de anotações de estereótipo. O `@Transactional` na classe continua funcionando, porque o Spring aplica o proxy a beans criados por `@Bean`.
- **Porta de saída única por agregado** (`GreetingRepository` com `save` e `findById`). Com dois métodos, segregar em várias interfaces seria cerimônia. Se uma porta crescer e casos de uso passarem a usar subconjuntos diferentes dela, aí se segrega (ISP). Essa orientação vai para o `ARCHITECTURE.md`.
- **Domínio:**
  - `Greeting` é uma classe final, sem setters, com fábrica `create(...)` (nova saudação, aplica as regras) e `restore(...)` (reconstrução a partir da persistência, sem gerar id ou data);
  - os value objects são `record`s com validação no construtor compacto (`GreetingId`, `PersonName`);
  - as regras de negócio validam no domínio, e o controller **não** duplica com Bean Validation.
- **Persistência:**
  - a entidade JPA (`GreetingJpaEntity`) é separada do domínio;
  - o mapper é manual (sem MapStruct) e o adapter implementa a porta de saída;
  - o `GreetingJpaRepository` (Spring Data) é detalhe interno do adapter.
- **Web:**
  - DTOs de entrada e saída são `record`s, sem Lombok;
  - o controller só traduz HTTP ↔ comando/resultado do caso de uso. O caso de uso devolve um resultado próprio (record em `port.in`), e o controller nunca expõe o modelo de domínio diretamente.
- **Erros:**
  - formato Problem Details (RFC 9457), via um `@RestControllerAdvice` global em `shared`;
  - `DomainException` e subclasses viram 400, e `NotFoundException` e subclasses viram 404;
  - JSON malformado também devolve 400 em Problem Details;
  - exceções específicas da feature estendem as bases de `shared` (ex.: `InvalidPersonNameException`, `GreetingNotFoundException`).

### Banco

- Migration criada com `make migration name=create_greeting`. Ela cria a tabela `greeting` com:
  - `id uuid primary key`;
  - `name varchar(100) not null`;
  - `created_at timestamptz not null`.

### Testes

| Camada | Tipo | Como |
|---|---|---|
| domain | unitário puro | JUnit 5 + AssertJ, sem Spring |
| application | unitário puro | repositório falso em memória implementando `GreetingRepository` e `Clock.fixed` *(decisão do Claude: fake em vez de Mockito, mais legível e reutilizável)* |
| adapter.in.web | fatia | `@WebMvcTest` com as portas de entrada mockadas. Cobre 201 + `Location`, 200, 400 (nome inválido, JSON malformado e id que não é UUID) e 404, conferindo o formato Problem Details |
| adapter.out.persistence | fatia | `@DataJpaTest` contra o Postgres do compose (sem trocar por banco embutido e sem Testcontainers), com ida e volta pelo adapter |
| arquitetura | ArchUnit | as regras 1–6 acima |
| contexto | existente | `contextLoads` continua passando |

Todos os testes passam com `make test`.

### Documentação

- **`docs/ARCHITECTURE.md`** (novo), escrito para um agente de IA seguir:
  - visão das camadas e o que cada uma pode conhecer;
  - as regras de dependência e o fato de serem verificadas pelo ArchUnit (com o nome do teste);
  - o **passo a passo para criar uma feature nova**, na ordem: domínio → portas → caso de uso → adapters → config → migration → testes;
  - convenções de nomes;
  - que tipo de teste vai em cada camada;
  - quando segregar portas;
  - um apontamento para `greeting` como implementação de referência;
  - o documento precisa ser autossuficiente (com trechos curtos de código) para continuar útil depois que a saudação for apagada.
- **`CLAUDE.md`**:
  - atualizar "Estado do projeto";
  - adicionar uma referência curta e explícita a `docs/ARCHITECTURE.md` como leitura obrigatória antes de implementar qualquer feature;
  - não duplicar as regras lá.
- O `ARCHITECTURE.md` registra como remover a saudação no futuro: a migration aplicada não pode ser apagada nem alterada, então a remoção é feita com uma nova migration `drop table greeting`.

## Deixado para o builder

- Versão do ArchUnit (a estável mais recente compatível com Java 25) e a forma de declarar no catálogo.
- Nomes exatos de métodos, testes e classes auxiliares, desde que respeitem as responsabilidades acima.
- Configuração do `@DataJpaTest` para usar o datasource real (ex.: `@AutoConfigureTestDatabase(replace = NONE)`) no pacote correto do Spring Boot 4.
- Detalhes do Problem Details (campos `type`/`title`/`detail`), contanto que o formato seja RFC 9457 e consistente.
- Anotações springdoc no controller: só se agregarem; o mínimo é o endpoint aparecer no Swagger.
- Ordem dos commits na branch de implementação. O primeiro commit da branch (`docs: ...`) inclui esta spec, o plan e a seção "Specs, plans e tasks" já adicionada ao `CLAUDE.md` *(decisão do Claude)*. As edições seguintes do `CLAUDE.md` ("Estado do projeto" e o apontamento para `ARCHITECTURE.md`) vão no commit da documentação de arquitetura.

## Outras decisões do Claude (menores)

- Adapters (controller, adapter de persistência, handler) podem usar `@Component`/`@RestController`. A regra "sem estereótipo" vale só para casos de uso.
- Features são os subpacotes diretos de `br.com.altapersonal.api`, exceto `shared`.
- O tamanho máximo do nome conta code points.
- `ARCHITECTURE.md` é escrito em português, como o resto da documentação do repositório.
