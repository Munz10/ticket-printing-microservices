# Ticket Printing Microservices

A small microservices exploration project, built around the same domain as
the [concurrent-ticket-system](https://github.com/Munz10/concurrent-ticket-system)
(toner/paper-constrained ticket printing), but split into independently
deployable services communicating over HTTP and RabbitMQ.

## Services

### `printing-service` (done)
Spring Boot REST API that owns the machine state (toner level, paper level,
tickets printed) in memory.

Endpoints:
- `POST /tickets?type=ECONOMY|BUSINESS|FIRST_CLASS|VIP_PREMIUM` — print a ticket
  (consumes toner/paper, returns `409 Conflict` if not enough resources)
- `GET /status` — current toner/paper levels and tickets printed
- `POST /refill/toner` — refill toner to full
- `POST /refill/paper` — add one pack of paper (capped at full tray)

Run it:
```bash
cd printing-service
./mvnw spring-boot:run
```

Try it:
```bash
curl http://localhost:8081/status
curl -X POST "http://localhost:8081/tickets?type=BUSINESS"
curl -X POST http://localhost:8081/refill/toner
```

### `resupply-service` (done)
Listens for "resource low" events published by `printing-service` over
RabbitMQ and calls `/refill/toner` or `/refill/paper` on it in response —
the event-driven equivalent of the technician threads in the original
project (no more polling).

Run it (with `printing-service` and RabbitMQ already running):
```bash
cd resupply-service
./mvnw spring-boot:run
```

Configuration (`src/main/resources/application.properties`):
- `printing-service.base-url` — where to find printing-service
  (overridable via `PRINTING_SERVICE_URL` env var)
- `spring.rabbitmq.*` — RabbitMQ connection settings
  (overridable via `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`,
  `RABBITMQ_PASSWORD` env vars)

### `passenger-service` (done)
Load generator that simulates passengers arriving at random and requesting a
ticket — the equivalent of the passenger threads in the original project.
On a fixed interval it calls `POST /tickets` on `printing-service` with a
random ticket type, logging the result (and tolerating `409 Conflict`
responses when resources are unavailable).

Run it (with `printing-service` already running):
```bash
cd passenger-service
./mvnw spring-boot:run
```

Configuration (`src/main/resources/application.properties`):
- `printing-service.base-url` — where to find printing-service
  (overridable via `PRINTING_SERVICE_URL` env var)
- `passenger.request-interval-ms` — how often a ticket is requested
  (overridable via `PASSENGER_REQUEST_INTERVAL_MS` env var)

## Event-driven architecture

`printing-service` and `resupply-service` communicate asynchronously via
RabbitMQ instead of HTTP polling:

- `printing-service` declares a topic exchange named `resource-events`.
  Whenever toner or paper drops to/below its minimum level (and wasn't
  already flagged), it publishes a `ResourceLowEvent` with routing key
  `resource.toner.low` or `resource.paper.low`. The notification is
  edge-triggered — it fires once per depletion and only fires again after
  the resource is refilled.
- `resupply-service` declares a queue (`resupply.resource-events`) bound to
  that exchange with the pattern `resource.*.low`, and a `@RabbitListener`
  reacts to each event by calling the corresponding refill endpoint on
  `printing-service`.
- The RabbitMQ management UI is available at http://localhost:15672
  (default credentials `guest`/`guest`) when running via Docker Compose.

## Monitoring

Every service exposes Actuator endpoints, including `/actuator/prometheus`
with Micrometer-formatted metrics
(`management.endpoints.web.exposure.include=health,info,prometheus`).
A `prometheus` service (config in [`prometheus/prometheus.yml`](prometheus/prometheus.yml))
scrapes all three services every 5 seconds when running via Docker Compose.

- `printing-service` health → http://localhost:8081/actuator/health
- `resupply-service` health → http://localhost:8082/actuator/health
- `passenger-service` health → http://localhost:8083/actuator/health
- Prometheus UI → http://localhost:9090

## Running with Docker Compose

Each service has a multi-stage `Dockerfile` (Maven build + JRE runtime).
`docker-compose.yml` wires up `rabbitmq`, `printing-service`,
`resupply-service`, `passenger-service` and `prometheus`, with the services
pointed at each other via their container hostnames.

```bash
docker compose up --build
```

- `printing-service` → http://localhost:8081
- `resupply-service` → http://localhost:8082 (no public endpoints yet, just consumes events)
- `passenger-service` → http://localhost:8083 (no public endpoints yet, just generates load)
- RabbitMQ management UI → http://localhost:15672 (`guest`/`guest`)
- Prometheus UI → http://localhost:9090

## Roadmap
1. ~~`printing-service` with REST API + in-memory state~~
2. ~~`resupply-service` polling over HTTP~~
3. ~~Dockerize both, run via `docker-compose`~~
4. ~~Replace polling with events (RabbitMQ/Kafka: "low resource" events)~~
5. ~~Add a load-generating passenger service + basic monitoring (Actuator/Prometheus)~~
