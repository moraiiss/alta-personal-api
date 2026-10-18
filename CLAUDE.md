# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

Projeto recém-gerado (Spring Initializr): só existe a classe `AltaPersonalApiApplication` e o teste `contextLoads`. Ainda não há domínio, controllers, entidades nem configuração de datasource. Atualize este arquivo quando a arquitetura tomar forma.

## Stack

- Java 25 (toolchain do Gradle), Spring Boot 4.1.1, Gradle 9.8.1 via wrapper (`./gradlew`)
- Spring Web MVC + Spring Data JPA, driver PostgreSQL (runtime)
- springdoc-openapi 3.1.0: Swagger UI em `/swagger-ui.html` e spec em `/v3/api-docs` quando a app está rodando
- Testes: JUnit 5 (Jupiter) com os starters `data-jpa-test` e `webmvc-test`
- Dependências e plugins centralizados no Version Catalog `gradle/libs.versions.toml` (referenciados como `libs.*` no `build.gradle`). Bibliotecas gerenciadas pelo BOM do Spring Boot ficam sem versão no catálogo; só declare versão para o que o BOM não cobre.
- Pacote base: `br.com.altapersonal.api`. Novos componentes precisam ficar abaixo dele para o component scan do `@SpringBootApplication`.

## Comandos

```bash
./gradlew build                      # compila + testes
./gradlew bootRun                    # sobe a API
./gradlew test                       # todos os testes
./gradlew test --tests 'br.com.altapersonal.api.AltaPersonalApiApplicationTests'            # uma classe
./gradlew test --tests 'br.com.altapersonal.api.AltaPersonalApiApplicationTests.contextLoads' # um método
./gradlew clean
```

Não há lint/formatter configurado.

## Atenção: banco de dados

`application.yaml` só define `spring.application.name`. Como JPA e o driver PostgreSQL estão no classpath, o `bootRun` e o `@SpringBootTest` (`contextLoads`) falham ao subir o contexto até existir um datasource configurado (`spring.datasource.*` ou variáveis de ambiente `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`). Para testes de repositório, prefira o slice `@DataJpaTest` em vez do `@SpringBootTest` completo.

## Git

Mensagens de commit, títulos de PR e nomes de branch seguem o [Conventional Commits](https://www.conventionalcommits.org/) e são escritos em inglês (ex.: commit/título `feat: add student endpoint`, branch `feat/add-student-endpoint`). Descrições de PR podem ser em português.

Não incluir nenhuma co-autoria ou atribuição: sem `Co-Authored-By` em commits, sem "Generated with Claude Code" ou equivalente em PRs, issues, comentários ou qualquer outro artefato.

## Contexto do workspace

Este repositório fica em `../` junto com `alta-personal` (frontend Vite/TypeScript com Supabase, que tem `AGENTS.md`, `PRODUCT.md` e `CONTEXT.md`) e `docs` (documentos comerciais). Consulte esses arquivos do frontend quando precisar de contexto de produto ou de domínio para a API.
