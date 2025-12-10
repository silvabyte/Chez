# TypeScript SDK Generation

Generate a TypeScript client from the running boogieloops.web example API using @hey-api/openapi-ts, then run a quick demo.

## Prerequisites

- Node.js and npm installed
- The boogieloops.web example API running (see below)

## 1) Start the API (terminal A)

```bash
make example-caskchez-api  # starts UserCrudAPI on http://localhost:8082
```

The OpenAPI spec is served at `http://localhost:8082/openapi`.

## 2) Generate the SDK (terminal B)

```bash
# Install toolchain for the TS SDK folder
make sdk-install

# Generate the client into typescript-sdk/src/client
make sdk-generate
```

Config: `typescript-sdk/openapi-ts.config.ts` points to `http://0.0.0.0:8082/openapi` and outputs to `src/client/`.

## 3) Try the demo (optional)

```bash
# By default uses BASE_URL=http://0.0.0.0:8082
make sdk-demo

# Or override the API base URL
BASE_URL=http://localhost:8082 make sdk-demo
```

The demo creates a user, lists users, fetches one by id, updates it, then deletes it.

## Troubleshooting

- 404 on generation: ensure the server is running and OpenAPI is available at `/openapi`.
- Port mismatch: if you changed ports, update `typescript-sdk/openapi-ts.config.ts` or set `BASE_URL` for `sdk-demo`.
- Node/npm issues: run `make sdk-install` to ensure dependencies are present.
