# Masjid-App-Backend - Isquare

Spring Boot 3 (Java 21, Maven) API for the Masjid Guide apps. The API is served
under `/api/v1`; health is at `/api/v1/actuator/health`.

## Running locally

The app has no built-in secrets: it will not start until the database, JWT,
MinIO, encryption and member API settings are supplied. For local development
put them in a gitignored `application-local.yml`:

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# edit the REPLACE_WITH_... values, then:
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Alternatively, export the environment variables listed below instead of using
the file. Flyway creates and migrates the schema on startup.

## Configuration

Every environment supplies these as environment variables (from Infisical on
the servers):

| Variable | Required | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | yes | PostgreSQL connection |
| `JWT_SECRET` | yes | Base64, at least 32 bytes |
| `ENCRYPTION_MASTER_KEY` | yes | Base64 of a 128/192/256-bit AES key; unique per environment |
| `MEMBER_API_KEY` | yes | Key the member app sends |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` | yes | Object storage |
| `MINIO_BUCKET`, `MINIO_SECURE`, `MINIO_PUBLIC_URL` | no | Defaults: `masjid-app`, `false`, `MINIO_ENDPOINT` |
| `SPRING_PROFILES_ACTIVE` | no | `dev` or `prod`; see `application-dev.yml` / `application-prod.yml` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | no | Default `validate`; Flyway owns the schema |
| `CORS_ALLOWED_ORIGINS` | no | Comma-separated origins or patterns; defaults to the masjid-app and Vercel origins |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | no | Push notifications are disabled without it |
| `LOG_LEVEL_ROOT`, `LOG_LEVEL_APP`, `LOG_LEVEL_SECURITY` | no | Log levels |
| `LOG_STRUCTURED_FORMAT` | no | Default `logstash` (JSON); set empty for plain text |
| `STRIPE_FEE_PERCENT`, `STRIPE_FEE_FIXED` | no | Stripe fee gross-up |

## Docker image

`Dockerfile` builds one image for all environments. At startup `entrypoint.sh`
does one of two things:

- With `INFISICAL_CLIENT_ID` set, it logs in to Infisical with that machine
  identity and runs the app with the secrets injected. It also needs
  `INFISICAL_CLIENT_SECRET`, `INFISICAL_URL`, `INFISICAL_PROJECT_ID` and
  `INFISICAL_ENV`, and optionally `INFISICAL_PATH` (default `/`).
- Without it, it runs the app with the container's environment as-is (handy
  for `docker run -e ...` locally).

## CI/CD

- `pr-check.yml` builds every pull request.
- `dev-build.yml` builds every merge to `dev` and pushes
  `ghcr.io/isquare-tech-solutions-uk/masjidguide-backend:dev`.
- `release.yml` (run by hand from `dev` or `main` with a `version` such as
  `1.4.0`) tags the commit `backend-v<version>` and pushes
  `ghcr.io/isquare-tech-solutions-uk/masjidguide-backend:<version>`.
- `cleanup.yml` (weekly) deletes old untagged dev builds, then keeps the 30
  most recent image versions in GHCR.
- Nothing deploys on push. The dev stack runs `:dev`; masjid stacks run a
  released version, chosen per stack in Portainer.

### Testing on dev

Merge to `dev`, wait for the **Dev build** workflow to go green, then in
Portainer open `masjid-guide-dev` and **Update the stack** with
**Re-pull image** on.
