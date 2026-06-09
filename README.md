# Ticket Printing Microservices

A small microservices exploration project, built around the same domain as
the [concurrent-ticket-system](https://github.com/Munz10/concurrent-ticket-system)
(toner/paper-constrained ticket printing), but split into independently
deployable services communicating over HTTP.

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
mvn spring-boot:run
```

Try it:
```bash
curl http://localhost:8081/status
curl -X POST "http://localhost:8081/tickets?type=BUSINESS"
curl -X POST http://localhost:8081/refill/toner
```

### `resupply-service` (planned)
A small scheduled job that polls `printing-service`'s `/status` endpoint and
calls `/refill/toner` or `/refill/paper` when levels run low — the
microservices equivalent of the technician threads in the original project.

### `passenger-service` / load generator (future)
Simulates passengers calling `/tickets` periodically — the equivalent of the
passenger threads in the original project.

## Roadmap
1. ~~`printing-service` with REST API + in-memory state~~
2. `resupply-service` polling over HTTP
3. Dockerize both, run via `docker-compose`
4. Replace polling with events (RabbitMQ/Kafka: "low resource" events)
5. Add a load-generating passenger service + basic monitoring (Actuator/Prometheus)
