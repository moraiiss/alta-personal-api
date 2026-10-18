.DEFAULT_GOAL := help

COMPOSE := docker compose
GRADLE  := ./gradlew --project-cache-dir /gradle-project-cache
MIGRATIONS_DIR := src/main/resources/db/migration

-include .env

.PHONY: help up down logs test psql db-reset migration clean

help: ## Lista os comandos disponíveis
	@grep -E '^[a-zA-Z_-]+:.*## ' $(firstword $(MAKEFILE_LIST)) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

.env:
	cp .env.example .env

up: .env ## Sobe API e Postgres com hot reload (cria o .env na primeira vez)
	$(COMPOSE) up --build -d
	@echo "API: http://localhost:$(or $(API_PORT),8080)/swagger-ui.html (acompanhe com 'make logs')"

down: ## Para e remove os containers (mantém os dados)
	$(COMPOSE) down

logs: ## Acompanha os logs da API
	$(COMPOSE) logs -f api

test: .env ## Roda os testes dentro do container da API, contra o Postgres
	@# exec no container que já roda: um segundo container disputaria os locks do cache do Gradle
	$(COMPOSE) up -d
	$(COMPOSE) exec api $(GRADLE) test

psql: ## Abre um psql no banco
	$(COMPOSE) exec db psql -U $(or $(POSTGRES_USER),altapersonal) -d $(or $(POSTGRES_DB),altapersonal)

db-reset: ## APAGA os dados do banco; as migrations rodam de novo no próximo 'make up'
	@read -p "Isso apaga todos os dados do banco local. Continuar? [y/N] " ans && [ "$$ans" = y ]
	$(COMPOSE) down
	docker volume rm alta-personal-api_db-data

migration: ## Cria uma migration vazia: make migration name=create_students
	@test -n "$(name)" || (echo "Informe o nome: make migration name=<descricao>" && exit 1)
	@file=$(MIGRATIONS_DIR)/V$$(date +%Y%m%d%H%M%S)__$(name).sql; touch $$file; echo "Criada $$file"

clean: ## APAGA containers e todos os volumes (banco e caches do Gradle)
	@read -p "Isso apaga o banco local e os caches do Gradle. Continuar? [y/N] " ans && [ "$$ans" = y ]
	$(COMPOSE) down -v --remove-orphans
