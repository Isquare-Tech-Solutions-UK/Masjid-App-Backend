#!/bin/sh
# Container entrypoint.
#
# Two ways to run this image:
#
# 1. Infisical (Portainer stacks): set INFISICAL_CLIENT_ID,
#    INFISICAL_CLIENT_SECRET, INFISICAL_URL, INFISICAL_PROJECT_ID and
#    INFISICAL_ENV (optionally INFISICAL_PATH, default "/"). The container
#    logs in with the machine identity and starts the app with the secrets
#    injected as environment variables.
#
# 2. Plain environment (legacy masjid-app compose stack): leave
#    INFISICAL_CLIENT_ID unset and pass the secrets as environment variables
#    directly. The app starts as-is.
set -e

if [ -z "${INFISICAL_CLIENT_ID:-}" ]; then
  echo "entrypoint: INFISICAL_CLIENT_ID not set, starting with the container environment as-is"
  exec java -jar /app/app.jar
fi

# Fail with a clear message instead of a confusing CLI error.
for var in INFISICAL_CLIENT_SECRET INFISICAL_URL INFISICAL_PROJECT_ID INFISICAL_ENV; do
  eval "value=\${$var:-}"
  if [ -z "$value" ]; then
    echo "entrypoint: INFISICAL_CLIENT_ID is set but $var is empty" >&2
    exit 1
  fi
done

# Assignment and export are separate on purpose: with `export VAR=$(...)` the
# exit status of the login is lost, and a failed login would go unnoticed.
INFISICAL_TOKEN=$(infisical login --method=universal-auth \
  --client-id="$INFISICAL_CLIENT_ID" --client-secret="$INFISICAL_CLIENT_SECRET" \
  --domain="$INFISICAL_URL" --silent --plain)
export INFISICAL_TOKEN

# The machine identity credentials are not needed past this point; keep them
# out of the application's environment.
unset INFISICAL_CLIENT_ID INFISICAL_CLIENT_SECRET

exec infisical run --domain="$INFISICAL_URL" \
  --projectId="$INFISICAL_PROJECT_ID" --env="$INFISICAL_ENV" \
  --path="${INFISICAL_PATH:-/}" -- java -jar /app/app.jar
