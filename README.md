# PriceWatch

PriceWatch is a service for tracking product prices and notifying users when prices decrease. This initial stage contains only the application infrastructure; business logic and REST endpoints are intentionally not implemented.

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL 17
- Lombok
- Maven
- Docker Compose

## Run

1. Create a local environment file: `copy .env.example .env`.
2. Start PostgreSQL: `docker compose up -d`.
3. Start the application: `mvnw.cmd spring-boot:run`.

The application reads database settings exclusively from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.

## Verify

Run `mvnw.cmd verify` to compile and validate the build. Check the database service with `docker compose ps`; its status should be `healthy`. Application startup confirms the PostgreSQL connection.

## Project Structure

```text
src/main/java/com/example/pricewatch
├── config/       # Application configuration
├── controller/   # Future web layer
├── dto/          # Future API contracts
├── entity/       # Future persistence models
├── exception/    # Future error handling
├── mapper/       # Future object mappings
├── repository/   # Future data access layer
└── service/      # Future business layer
```
