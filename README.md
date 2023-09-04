# banking-api

Banking api for creating accounts and executing monetary transactions between them.

## API:
- Default port `8080`, can be configured in `docker-compose.yml`
- `POST /accounts`
  - Creates new account. Balance must be positive or zero.
  - In case of validation failures, returns `400 BAD_REQUEST`
  - Request: `{"balance": 100.0}`
  - Response: `{"id": 1, "balance": 100.0}`
- `GET /accounts/{id}/balance`
  - Fetches the balance from existing account.
  - In case of validation failures, returns `400 BAD_REQUEST`
  - Response: `100.0`
- `POST /transactions`
  - Creates and executes transaction between two existing accounts. 
    - Amount should be positive
    - Recipient should have enough funds
  - In case of validation failures, returns `400 BAD_REQUEST`
  - In case of concurrent data access and version conflicts, returns `409 CONFLICT`
  - Request: `{"amount": 1.0, "from": 1, "to": 2}`
  - Response: `{"id": 1, "amount": 1.0,  "from": 1, "to": 2}`

## Used libraries and technologies
- Kotlin `1.9.10` `(JVM 17.0.8 Liberica)`
- Spring `6.0.11`, Spring Boot `3.1.3`
- Spring WebFlux
- Spring Data R2DBC 
- Postgres 15
- Testcontainers
- Flyway
- Docker, Docker Compose

## How to build
`./mvnw clean install docker:build`

## Prerequisites to run:
- Docker
- Docker compose plugin

## How to run
`docker compose up -d`

## How to stop
`docker compose down -v`

## Main challenges
For the data consistency during concurrent transactions 
I decided to use optimistic locking via versioning (@Version Spring annotation).
This creates negative response on concurrent request for the same account 
but provides consistency without too much performance overhead on transactions.
Service returns response with `HTTP 409 CONFLICT` response code in case of such request.
If needed, this request can be retried either on client or on server side.