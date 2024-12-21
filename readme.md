# Core Banking System

Enterprise-grade modular banking system leveraging Spring Boot 3.4.1, Spring Modulith, and GraphQL for robust financial operations.

## Architecture Overview

### Core Modules
- **Accounts Module**
  - Account creation and management
  - Balance operations
  - Status lifecycle management
  - Event publishing for account changes

- **Transfers Module**
  - Money transfer operations
  - Transaction status tracking
  - Integration with account balances
  - Event-driven transfer processing

- **Shared Module**
  - Common utilities
  - Cross-cutting concerns
  - Shared domain models

### Technical Architecture
- Event-driven communication between modules
- GraphQL API using Netflix DGS
- PostgreSQL persistence with JDBC
- Flyway database migrations
- Virtual Thread execution model
- Native compilation support

## Technology Stack

### Core Framework
- Java 21
- Spring Boot 3.4.1
- Spring Modulith 1.3.0
- Netflix DGS 9.2.2

### Data Layer
- PostgreSQL 16
- Spring Data JDBC
- Flyway Migrations

### API Layer
- GraphQL
- Netflix DGS Framework
- Spring WebFlux (test only)

### Monitoring & Observability
- Spring Actuator
- Prometheus metrics
- OpenTelemetry integration
- Health check probes

## Setup & Installation

### Prerequisites
- Java 21 or higher
- Docker and Docker Compose
- Maven 3.9+
- PostgreSQL 16+

### Database Setup

docker run -d \
  -e POSTGRES_DB=corebanking \
  -e POSTGRES_USER=corebanking_app \
  -e POSTGRES_PASSWORD=c0r3b4nk1ng \
  -p 5432:5432 \
  postgres:16

### Application Setup

./mvnw clean install
./mvnw spring-boot:run

## Configuration Reference

## API Reference

### GraphQL Endpoints
- Playground: http://localhost:8090/graphiql
- API Endpoint: http://localhost:8090/graphql

### Account Operations
- Query account by ID
- Query account by number
- Query accounts by document
- Create new account
- Activate account
- Update balance

### Transfer Operations
- Initiate transfer
- Query transfer status
- List transfers by account
- Query transfer by ID

## Monitoring & Operations

### Health Checks
- Application: /actuator/health
- Liveness: /actuator/health/liveness
- Readiness: /actuator/health/readiness

### Metrics
- Prometheus: /actuator/prometheus
- Metrics Browser: /actuator/metrics
- Flyway Status: /actuator/flyway

### Observability
- OpenTelemetry integration
- Distributed tracing
- Metrics aggregation
- Log correlation

## Development

### Build Commands
- Full Build: ./mvnw clean install
- Run Tests: ./mvnw test
- Native Build: ./mvnw -Pnative spring-boot:build-image
- Generate Docs: ./mvnw spring-boot:run test

### Module Documentation
- Generated Docs: target/spring-modulith-docs/
- API Schema: src/main/resources/graphql/
- Test Reports: target/surefire-reports/

### Testing
- Unit Tests
- Integration Tests
- Module Boundary Tests
- GraphQL API Tests

## Security

### Database Security
- Connection encryption
- Credential management
- Connection pooling limits

### API Security
- GraphQL depth limiting
- Query complexity analysis
- Rate limiting capabilities

### Application Security
- Virtual thread isolation
- Event publication security
- Module boundary enforcement

## Performance

### Optimizations
- Virtual Thread execution
- Connection pool tuning
- Native compilation support
- Event-driven architecture

### Scalability
- Stateless design
- Database connection management
- Event publication batching
- Graceful shutdown support

## License & Legal
Apache License 2.0