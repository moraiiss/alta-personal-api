# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

Projeto no início: só existe a classe `AltaPersonalApiApplication` e o teste `contextLoads`. Ainda não há domínio, controllers, entidades nem migrations. O ambiente local (Docker Compose + Makefile) e o Flyway já estão configurados. Atualize este arquivo quando a arquitetura tomar forma.

## Stack

- Java 25 (toolchain do Gradle), Spring Boot 4.1.1, Gradle 9.8.1 via wrapper (`./gradlew`)
- Spring Web MVC + Spring Data JPA, driver PostgreSQL (runtime)
- Flyway para o schema; Spring Boot DevTools (`developmentOnly`) para hot reload
- springdoc-openapi 3.1.0: Swagger UI em `/swagger-ui.html` e spec em `/v3/api-docs` quando a app está rodando
- Testes: JUnit 5 (Jupiter) com os starters `data-jpa-test` e `webmvc-test`
- Dependências e plugins centralizados no Version Catalog `gradle/libs.versions.toml` (referenciados como `libs.*` no `build.gradle`). Bibliotecas gerenciadas pelo BOM do Spring Boot ficam sem versão no catálogo; só declare versão para o que o BOM não cobre.
- Pacote base: `br.com.altapersonal.api`. Novos componentes precisam ficar abaixo dele para o component scan do `@SpringBootApplication`.

## Ambiente local (Docker)

O fluxo padrão é via `make` (rode `make` para listar os alvos). `make up` cria o `.env` a partir do `.env.example` se ele não existir e sobe dois containers, definidos em `compose.yaml`:

- `db`: Postgres 18, com dados no volume `alta-personal-api_db-data`, exposto na porta `DB_PORT` (5432)
- `api`: imagem só de desenvolvimento (`docker/dev/`), com o repositório montado em `/app` e exposta na porta `API_PORT` (8080). O `docker/dev/start.sh` roda um `./gradlew --continuous classes` em background e o `bootRun`. O DevTools reinicia o contexto a cada recompilação: editar em `src/main` no host basta.

```bash
make up                          # sobe API + Postgres com hot reload
make logs                        # logs da API
make test                        # testes dentro do container da API, contra o Postgres
make psql                        # psql no banco
make migration name=create_x     # cria db/migration/V<timestamp>__create_x.sql vazio
make down                        # para os containers (mantém os dados)
make db-reset                    # apaga o volume do banco (pede confirmação)
make clean                       # apaga containers e todos os volumes (pede confirmação)
```

Armadilhas:

- Dentro do container, o Gradle usa `--project-cache-dir /gradle-project-cache` e o build vai para `/gradle-build` (via `GRADLE_BUILD_DIR` no `build.gradle`), ambos em volumes. Assim o container não cria arquivos de root no `build/` e no `.gradle/` do host. Para rodar o Gradle no container, use `docker compose exec api ./gradlew --project-cache-dir /gradle-project-cache ...`.
- Não use `docker compose run api ...` para tarefas Gradle com a API rodando: os dois containers disputam os locks do cache do Gradle (volume compartilhado) e o build trava por timeout. Use `exec` no container que já está rodando, como faz o `make test`.

## Comandos Gradle (host)

```bash
./gradlew build                      # compila + testes
./gradlew bootRun                    # sobe a API
./gradlew test                       # todos os testes
./gradlew test --tests 'br.com.altapersonal.api.AltaPersonalApiApplicationTests'            # uma classe
./gradlew test --tests 'br.com.altapersonal.api.AltaPersonalApiApplicationTests.contextLoads' # um método
./gradlew clean
```

Não há lint/formatter configurado.

## Banco de dados e migrations

O banco é próprio da API e independente do Supabase do frontend. O datasource vem só de variáveis de ambiente (`SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`), que o `compose.yaml` injeta no container. No host, `./gradlew bootRun`, `build` e `test` (por causa do `@SpringBootTest` `contextLoads`) falham sem essas variáveis. Por isso o caminho padrão é `make up`/`make test`.

O schema é responsabilidade do Flyway: as migrations ficam em `src/main/resources/db/migration` e rodam na subida da app, inclusive a cada restart do DevTools. O Hibernate usa `ddl-auto: validate`, então toda mudança de entidade precisa de uma migration. Não altere nem renomeie uma migration já aplicada. Se você apagar uma migration aplicada localmente, o Flyway falha na validação: rode `make db-reset`.

Para testes de repositório, prefira o slice `@DataJpaTest` em vez do `@SpringBootTest` completo.

## Specs, plans e tasks

Specs, plans e tasks ficam sempre direto em `docs/`: `docs/specs/`, `docs/plans/` e `docs/tasks/`, com o nome `YYYY-MM-DD-<tema>.md`. Isso vale também quando uma skill ou ferramenta sugere outro caminho (ex.: `docs/superpowers/...`).

## Git

Mensagens de commit, títulos de PR e nomes de branch seguem o [Conventional Commits](https://www.conventionalcommits.org/) e são escritos em inglês (ex.: commit/título `feat: add student endpoint`, branch `feat/add-student-endpoint`). Descrições de PR podem ser em português.

Não incluir nenhuma co-autoria ou atribuição: sem `Co-Authored-By` em commits, sem "Generated with Claude Code" ou equivalente em PRs, issues, comentários ou qualquer outro artefato.

## Contexto do workspace

Este repositório fica em `../` junto com `alta-personal` (frontend Vite/TypeScript com Supabase, que tem `AGENTS.md`, `PRODUCT.md` e `CONTEXT.md`) e `docs` (documentos comerciais). Consulte esses arquivos do frontend quando precisar de contexto de produto ou de domínio para a API.
