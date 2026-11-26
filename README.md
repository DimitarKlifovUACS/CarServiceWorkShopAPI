# Car Service Workshop API

A REST API for managing an auto-repair workshop. Vehicles arrive, mechanics open service orders, log work items and parts, and invoice the customer on completion.

It was first built for the Computer Interface Programming course and then extended as the backend for **Workshop Desk**, an Angular frontend built for the Internet Programming course (PostgreSQL, Auth0 login, JSON errors, extra endpoints for the frontend and its workshop board).

**Live:** https://workshop-desk-api.onrender.com/swagger-ui.html (free hosting: after ~15 minutes without traffic the first request takes up to ~2 minutes while the server wakes up).

**Tech:** Java 17, Spring Boot 3, Spring Data JPA, Spring Security (OAuth2 resource server), PostgreSQL, springdoc-openapi (Swagger), Docker.

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
| `AUTH0_ISSUER` | `https://dev-8cqhzs6kt6ocflxl.us.auth0.com/` (must end with `/`) |
| `AUTH0_AUDIENCE` | `https://workshop-desk-api` |
| `ALLOWED_ORIGINS` | `http://localhost:4200` (comma-separated browser origins allowed by CORS) |
| `PORT` | `8080` |

## Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database and need no running database or Auth0 (requests carry test tokens). They cover the error format, soft deletes, the completed-order lock, the read endpoints, work-item statuses and the security rules (`src/test/java`).

## Deployment

- **API:** [Render](https://render.com) web service, built from the `Dockerfile` on every push to `master`. Health check path: `/v3/api-docs`.
- **Database:** [Neon](https://neon.tech) PostgreSQL (EU Central).
- **Environment variables on Render:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `ALLOWED_ORIGINS` (the frontend's URL). `PORT` is set by Render; the Auth0 values use the defaults above.
- Tables are created/updated by Hibernate (`ddl-auto=update`); `src/main/resources/schema.sql` adds the partial unique index on license plates on every start.

## Authentication

Users sign in with Google through Auth0 (OpenID Connect). The frontend receives an **access token** (a signed JWT) and sends it with every request:

```
Authorization: Bearer <access token>
```

The API is an OAuth2 **resource server**: for every request it checks the token's signature (with Auth0's public keys), issuer, audience (`https://workshop-desk-api`) and expiry. A missing or invalid token returns **401** (as JSON, like all errors). Swagger UI and `/v3/api-docs` are public; in Swagger, click **Authorize** and paste a token to try the endpoints.

To get a token for testing without the frontend: Auth0 dashboard → Applications → APIs → Workshop Desk API → **Test** tab.

Browsers can only call the API from the origins in `ALLOWED_ORIGINS` (CORS).

## Errors

All errors are returned as JSON in the standard "problem details" format (RFC 9457), content type `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Cannot complete order while work items are not done",
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
| 401 | Missing or invalid access token |
| 404 | Record or URL not found |
| 409 | Business rule violated (completing an order with unfinished work, changing a completed order, duplicate license plate) |
| 500 | Unexpected error (details are only logged on the server) |

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/vehicles?search=&page=0&size=20&sort=id,asc` | Paged list of vehicles; `search` matches plate, owner, make or model (case-insensitive). Returns `{content, page, size, totalElements, totalPages}` |
| GET | `/vehicles/{id}` | One vehicle (`old` is true for vehicles built before 2020) |
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

A full flow against a local API (with the demo data loaded). IDs come from the responses, so adjust them if yours differ.

```bash
# Put an access token in TOKEN first (see Authentication)
TOKEN=<access token>
API=http://localhost:8080

# Create a vehicle -> note the "id" in the response
curl -X POST $API/vehicles -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d '{"licensePlate":"XYZ-999","ownerName":"Ana Petrova","make":"Toyota","model":"Corolla","year":2018}'

# Search vehicles (paged)
curl "$API/vehicles?search=xyz&page=0&size=20" -H "Authorization: Bearer $TOKEN"

# Open a service order for the new vehicle (here id 4) -> note the order "id"
curl -X POST $API/vehicles/4/orders -H "Authorization: Bearer $TOKEN"

# Add a work item to the order (here id 5); it starts as TODO -> note its "id"
curl -X POST $API/orders/5/work-items -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d '{"description":"Replace brake pads","estimatedHours":1.5,"mechanicName":"Stefan"}'

# Add a part
curl -X POST $API/orders/5/parts -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d '{"name":"Brake pads","quantity":2,"unitPrice":45}'

# Completing now fails with 409: the work item is not DONE yet
curl -X PATCH $API/orders/5/complete -H "Authorization: Bearer $TOKEN"

# Move the work item (here id 7) across the board: TODO -> IN_PROGRESS -> DONE
curl -X PATCH $API/work-items/7/status -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"status":"IN_PROGRESS"}'
curl -X PATCH $API/work-items/7/status -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"status":"DONE"}'

# Now the order can be completed (after that it is locked)
curl -X PATCH $API/orders/5/complete -H "Authorization: Bearer $TOKEN"

# Workshop board: all work items of open orders
curl "$API/work-items?orderStatus=OPEN" -H "Authorization: Bearer $TOKEN"

# Revenue by month (sum of parts of completed orders)
curl $API/reports/revenue-by-month -H "Authorization: Bearer $TOKEN"
```
