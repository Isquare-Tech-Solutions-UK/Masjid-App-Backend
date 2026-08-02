# Centralised Logging

All container logs on the VPS — backend, frontend, Postgres, MinIO, nginx — are
collected into one searchable place.

| Component | Role |
| --- | --- |
| **Alloy** | Reads the Docker socket and tails every container. No per-service config. |
| **Loki** | Stores the logs. Indexes labels only, so it stays small. |
| **Grafana** | Query UI at `https://logs.masjid-app.isquaretechsolutions.com` |

Total footprint is roughly 300–400 MB RAM. Promtail — which most tutorials still
reference — is deprecated (EOL March 2026); Alloy is its replacement.

## Deployment

The CI workflow deploys this alongside the app — there is no manual setup step.
It runs from `~/masjid-app` as a **separate compose project**:

```bash
docker compose -p masjid-logging -f docker-compose.logging.yml up -d
```

The explicit `-p` is load-bearing. The app deploy runs `--remove-orphans`, which
removes containers labelled with the `masjid-app` project; these carry
`masjid-logging`, so they are untouched. **Never run this file without `-p`** —
the next app deploy would delete the entire stack.

It is also brought up *without* `--force-recreate`, so Compose only touches
services whose definition changed. Routine app deploys leave Loki, Alloy and
Grafana running and ingesting.

### Required secret

Add to Infisical:

| Variable | Value |
| --- | --- |
| `GRAFANA_ADMIN_PASSWORD` | a strong password |

The deploy step fails with a clear message if it is missing, rather than
starting Grafana with a blank admin password and leaving the dashboard open.

`GRAFANA_ADMIN_USER`, `GRAFANA_ROOT_URL` and `MASJID_NETWORK` all have working
defaults in `docker-compose.logging.yml`; override them in Infisical only if
they need to change.

### If the deploy fails on the network check

The stack joins the app's network as `external` so nginx can reach Grafana by
container name. That network is named after the app's compose project, which
Compose derives from its directory — `~/masjid-app` gives
`masjid-app_masjid-network`. Confirm with:

```bash
docker network ls | grep masjid
```

If it differs, set `MASJID_NETWORK` in Infisical to the real name.

## Querying

Grafana → **Explore** → Loki datasource.

```logql
{container=~"masjid-.*"}                       # everything, all containers
{container="masjid-backend"}                   # backend only
{container="masjid-backend", level="ERROR"}    # errors only
{container=~"masjid-.*"} |= "stripe"           # full-text across all containers
{container="masjid-nginx", status="500"}       # nginx 5xx

# One request end-to-end, across nginx and backend
{container=~"masjid-.*"} | json | requestId="0f1c..."

# Everything a specific logger emitted
{container="masjid-backend"} | json | logger_name=~"com.masjidapp.service.*"
```

Labels available for filtering: `container`, `project`, `service`, `stream`,
`host`, plus `level` (backend) and `status` (nginx). Everything else —
`logger_name`, `thread_name`, `requestId`, `request_uri` — is parsed at query
time with `| json`, which keeps index cardinality low.

### Nothing is truncated

Three separate limits normally cut off "show me everything" queries. All are
already raised in this setup:

- Loki `max_entries_limit_per_query`: 5000 → 100000
- Loki `max_query_length`: 721h → unlimited
- Grafana `maxLines`: 1000 → 5000 (Explore's *Line limit* box also overrides
  this per query)

Retention is unlimited (`retention_period: 0s`, compactor retention disabled).
To cap it later, set a duration in `logging/loki-config.yml` and flip
`compactor.retention_enabled: true`.

## Correlation IDs

The chain is wired end-to-end:

1. The web API client (`src/lib/api/client.ts`) generates an id per request and
   sends it as `X-Request-Id`. It appears in the browser console alongside each
   `[API Request]` / `[API Response]` line.
2. nginx reuses that inbound id — or mints its own via `$request_id` if a call
   arrives without one — and logs it in its JSON access log.
3. `RequestIdFilter` puts it in the SLF4J MDC, so every backend log line for
   that request carries `requestId`. It is echoed back on the response header.

So one id ties the browser console, the nginx access log and the backend logs
together:

```logql
{container=~"masjid-.*"} | json | requestId="<id>"
```

nginx and the backend deliberately use the same `requestId` field name in their
JSON so this single query spans both. A substring match — `|= "<id>"` — also
works and is cheaper to type.

Failed calls throw an `ApiError` carrying `.requestId`. Surfacing that in error
toasts is worthwhile — a user can then quote the id and you can retrieve the
exact server-side trace.

## Log levels

Set in `application.yml`, overridable via environment (and therefore Infisical)
without rebuilding:

| Variable | Default | Notes |
| --- | --- | --- |
| `LOG_LEVEL_ROOT` | `INFO` | |
| `LOG_LEVEL_APP` | `INFO` | `com.masjidapp` |
| `LOG_LEVEL_SECURITY` | `WARN` | Was `DEBUG`, which logs auth internals including token and header detail. Raise only while debugging an auth issue, then put it back. |
| `LOG_STRUCTURED_FORMAT` | `logstash` | Set empty for readable console output when running locally. |

## Disk safety

Every service caps its `json-file` buffer at 10 MB × 3 files. Without this the
buffers grow unbounded and eventually fill the VPS disk. Rotation is safe
because Loki holds the full history independently — once Alloy has shipped a
line, it survives whatever Docker rotates away.

## Backfilling logs from before this was installed

Alloy starts tailing from the current position, so anything written earlier
lives only in Docker's buffer. To keep it:

```bash
for c in $(docker ps --format '{{.Names}}'); do
  docker logs "$c" --timestamps > ~/log-archive-$c.log 2>&1
done
```

## Don't log secrets

This service handles the charity's Stripe secret key, webhook secret, and
`ENCRYPTION_MASTER_KEY`. Anything logged is retained indefinitely and readable
by anyone with Grafana access — never log a request/DTO that might carry them.