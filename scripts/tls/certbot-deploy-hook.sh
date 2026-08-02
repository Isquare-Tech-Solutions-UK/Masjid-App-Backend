#!/bin/sh
# Reload nginx after certbot renews a certificate.
#
# Installed to /etc/letsencrypt/renewal-hooks/deploy/ by setup-tls.sh and
# re-installed on every deploy, so it is versioned in git rather than being a
# file that only exists on the box.
#
# Why this is required: nginx reads certificate files into memory at startup.
# Certbot renewing them on disk changes nothing until nginx is reloaded — so
# without this hook, renewals succeed silently and nginx keeps serving the old
# certificate until it expires and the site starts throwing warnings.
#
# Certbot runs deploy hooks only when a certificate was actually renewed, and
# runs them as root.

set -eu

CONTAINER="masjid-nginx"
LOG_TAG="certbot-deploy-hook"

log() { logger -t "$LOG_TAG" "$1" 2>/dev/null || true; echo "$LOG_TAG: $1"; }

if ! command -v docker >/dev/null 2>&1; then
    log "docker not found; cannot reload $CONTAINER"
    exit 0
fi

# Not an error: the renewal itself succeeded. Whenever nginx next starts it
# reads the new certificate from disk anyway.
if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null || echo false)" != "true" ]; then
    log "$CONTAINER is not running; skipping reload"
    exit 0
fi

# Validate before reloading. A reload against a broken config leaves nginx
# serving the old one, but failing loudly here means the problem surfaces in
# the renewal log instead of at expiry.
if ! docker exec "$CONTAINER" nginx -t >/dev/null 2>&1; then
    log "ERROR: nginx -t failed inside $CONTAINER; NOT reloading"
    docker exec "$CONTAINER" nginx -t 2>&1 | sed "s/^/$LOG_TAG: /" || true
    exit 1
fi

docker exec "$CONTAINER" nginx -s reload
log "reloaded $CONTAINER after certificate renewal"