# Ticket Printing Platform

[![CI](https://github.com/Munz10/ticket-printing-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/Munz10/ticket-printing-microservices/actions/workflows/ci.yml)

A small platform of independently deployable Spring Boot services that model
a resource-constrained ticket printer: a printing machine with finite
toner/paper, a resupply system that reacts to low-resource alerts, and a
load generator that keeps the machine busy. Services communicate over REST
and RabbitMQ, ship as Docker images, and expose Prometheus metrics.

## Architecture

```
passenger-service ---HTTP--> printing-service ---RabbitMQ--> resupply-service
   (load generator)         (machine state)      (resource     |
                                  ^                low events)  |
                                  |__________________HTTP_______|
                                       (refill toner/paper)
```

## Services

### `printing-service`
Spring Boot REST API that owns the machine state (toner level, paper level,
tickets printed) in memory.

Endpoints:
- `POST /tickets?type=ECONOMY|BUSINESS|FIRST_CLASS|VIP_PREMIUM` — print a ticket
  (consumes toner/paper, returns `409 Conflict` if not enough resources,
  `400 Bad Request` for an unknown `type`)
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

### `resupply-service`
Listens for "resource low" events published by `printing-service` over
RabbitMQ and calls `/refill/toner` or `/refill/paper` on it in response,
keeping the printer stocked without any polling.

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

### `passenger-service`
Load generator that simulates passengers arriving at random and requesting a
ticket. On a fixed interval it calls `POST /tickets` on `printing-service`
with a random ticket type, logging the result (and tolerating `409 Conflict`
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
- If the broker is briefly unavailable when `printing-service` tries to
  publish a low-resource event, the print request still succeeds — the
  failure is only logged and recorded as a metric (see below).

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
- Grafana → http://localhost:3000 (default credentials `admin`/`admin`)

In addition to the standard JVM/HTTP metrics, each service publishes a few
domain-specific metrics:

- `printing-service`:
  - `printing_toner_level` / `printing_paper_level` — current resource gauges
  - `printing_tickets_printed_total{type}` — tickets printed, by ticket type
  - `printing_resource_unavailable_total` — print requests rejected with `409`
  - `printing_resource_low_events_total{resource,outcome}` — low-resource
    events published to RabbitMQ (`outcome=published|failed`)
- `resupply-service`:
  - `resupply_refills_total{resource,outcome}` — refill calls triggered by
    incoming events (`outcome=success|failed`)
- `passenger-service`:
  - `passenger_tickets_requested_total{type,outcome}` — simulated ticket
    requests (`outcome=printed|rejected|error`)

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
- Grafana → http://localhost:3000 (`admin`/`admin`)

Grafana is pre-provisioned (see [`grafana/provisioning`](grafana/provisioning)) with
a Prometheus datasource and a "Ticket Printing" dashboard
([`grafana/dashboards/ticket-printing.json`](grafana/dashboards/ticket-printing.json))
covering toner/paper levels, tickets printed by type, resource-low events,
resupply refills and passenger request outcomes — no manual setup required.

## Continuous Integration

A [GitHub Actions workflow](.github/workflows/ci.yml) builds and tests each
service with Maven on every push and pull request to `main`, then builds
each service's Docker image to make sure it still produces a working
container.

## Possible extensions

- Persist machine state in Postgres/Redis instead of in-memory
- Publish Docker images to a registry from CI
