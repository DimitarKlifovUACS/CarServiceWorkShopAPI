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

## Errors

All errors are returned as JSON in the standard "problem details" format (RFC 9457), content type `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Cannot complete order while work items are in progress",
  "instance": "/orders/4/complete"
}
```

Validation errors (400) also list every invalid field:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/vehicles",
  "fieldErrors": { "licensePlate": "must not be blank", "year": "must be greater than or equal to 1886" }
}
```

| Status | When |
|---|---|
| 400 | Validation failed, malformed JSON, or a wrong parameter type (e.g. `/orders/abc`) |
| 401 | Missing or invalid API key |
| 404 | Record or URL not found |
| 409 | Business rule violated (completing an order with work in progress, duplicate license plate) |
| 500 | Unexpected error (details are only logged on the server) |

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/vehicles?search=&page=0&size=20&sort=id,asc` | Paged list of vehicles; `search` matches plate, owner, make or model (case-insensitive). Returns `{content, page, size, totalElements, totalPages}` |
| GET | `/vehicles/{id}` | One vehicle |
| POST | `/vehicles` | Create a vehicle |
| DELETE | `/vehicles/{id}` | Delete a vehicle (soft delete) |
| GET | `/vehicles/{vehicleId}/orders` | Orders of a vehicle, newest first |
| POST | `/vehicles/{vehicleId}/orders` | Open a new order for a vehicle |
| GET | `/orders?status=OPEN` | All orders, newest first; `status` (OPEN or COMPLETED) is optional |
| GET | `/orders/{id}` | One order (includes `vehicleId` and `licensePlate`) |
| PATCH | `/orders/{id}/complete` | Complete an order (see business rules) |
| DELETE | `/orders/{id}` | Delete an order (soft delete) |
| GET | `/work-items?orderStatus=OPEN` | Work items across all orders, for the workshop board; each includes `orderId` and `licensePlate`. `orderStatus` (OPEN or COMPLETED) is optional |
| GET | `/orders/{orderId}/work-items` | Work items of an order |
| POST | `/orders/{orderId}/work-items` | Add a work item |
| PATCH | `/work-items/{id}/status` | Move a work item to another status. Body: `{"status": "TODO" \| "IN_PROGRESS" \| "DONE"}` |
| PATCH | `/work-items/{id}/finish` | Shortcut: mark a work item as DONE |
| DELETE | `/work-items/{id}` | Delete a work item (soft delete) |
| GET | `/orders/{orderId}/parts` | Parts of an order |
| POST | `/orders/{orderId}/parts` | Add a part |
| DELETE | `/parts/{id}` | Delete a part (soft delete) |
| GET | `/reports/revenue-by-month` | Revenue (sum of parts of completed orders) per month, oldest first |

## Business Rules

All violations return 409 Conflict.

1. **Completion rule:** a service order cannot be marked COMPLETED while any of its (non-deleted) work items is not DONE (work items move TODO → IN_PROGRESS → DONE, and new ones start as TODO). All work must be finished before closing. This is enforced in the service layer inside a `@Transactional` method that checks all work items before allowing the status change.
2. **Completed orders are locked:** once an order is COMPLETED, its work items and parts can no longer be added, finished or deleted, and the order cannot be completed again. This keeps the invoice and the revenue report final. A completed order can still be read, and deleted as a whole.
3. **Unique license plates:** two active vehicles cannot share a license plate. The plate of a deleted vehicle can be used again.

Deleted records (soft delete: `deletedAt` is set) behave as if they do not exist: they return 404 and are not counted anywhere.

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
