# Car Service Workshop API

A REST API for managing an auto-repair workshop. Vehicles arrive, mechanics open service orders, log work items and parts, and invoice the customer on completion.

## How to run

The API uses PostgreSQL. For local development, start the database with Docker (it runs on port 5433), then the API:

```bash
docker compose up -d
./mvnw spring-boot:run
```

The API will start on http://localhost:8080

Swagger UI is available at: http://localhost:8080/swagger-ui.html

Demo data is inserted on the first start, only when the database is empty.

### Configuration (environment variables)

| Variable | Default (local development) |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/carservice` |
| `DB_USER` / `DB_PASSWORD` | `carservice` / `carservice` |
| `API_KEY` | `local-dev-key` |
| `PORT` | `8080` |

Tests use an in-memory H2 database and need no running database: `./mvnw test`

## API Key

All requests must include the `X-API-Key` header with the value of `API_KEY` (locally `local-dev-key`):

```
X-API-Key: local-dev-key
```

Missing or invalid key returns 401 Unauthorized.

## Business Invariant

A service order cannot be marked COMPLETED while any of its work items is still IN_PROGRESS. All work must be finished before closing. The system returns 409 Conflict if this rule is violated. This is enforced in the service layer inside a `@Transactional` method that checks all work items before allowing the status change.

## Example curl commands

```bash
# Create a vehicle
curl -X POST http://localhost:8080/vehicles \
  -H "X-API-Key: local-dev-key" \
  -H "Content-Type: application/json" \
  -d '{"licensePlate":"ABC-123","ownerName":"Dimitar Klifov","make":"BMW","model":"320i","year":2020}'

# Get all vehicles
curl http://localhost:8080/vehicles \
  -H "X-API-Key: local-dev-key"

# Create a service order for vehicle 1
curl -X POST http://localhost:8080/vehicles/1/orders \
  -H "X-API-Key: local-dev-key"

# Add a work item to order 1
curl -X POST http://localhost:8080/orders/1/work-items \
  -H "X-API-Key: local-dev-key" \
  -H "Content-Type: application/json" \
  -d '{"description":"Replace brake pads","estimatedHours":1.5,"mechanicName":"Stefan"}'

# Finish work item 1
curl -X PATCH http://localhost:8080/work-items/1/finish \
  -H "X-API-Key: local-dev-key"

# Complete order 1
curl -X PATCH http://localhost:8080/orders/1/complete \
  -H "X-API-Key: local-dev-key"

# Get revenue by month report
curl http://localhost:8080/reports/revenue-by-month \
  -H "X-API-Key: local-dev-key"
```
