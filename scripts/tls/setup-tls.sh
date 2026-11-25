#!/usr/bin/env bash
# Converge TLS on the VPS to what domains.conf declares.
#
# Idempotent and safe to re-run: it compares the certificate's current SANs
# against the desired list and only calls certbot when they differ. That matters
# because Let's Encrypt allows only 5 duplicate certificates per week — a script
# that reissued on every run would exhaust that and lock you out of renewals.
#
# Run on the VPS as root:
#   sudo ./scripts/tls/setup-tls.sh
#
# Renewal itself is NOT driven from here. Certbot's own systemd timer handles
# that; this script ensures the deploy hook exists so renewals reach nginx.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=domains.conf
source "$SCRIPT_DIR/domains.conf"

HOOK_DIR="/etc/letsencrypt/renewal-hooks/deploy"
HOOK_DEST="$HOOK_DIR/reload-nginx.sh"
WEBROOT="$APP_DIR/certbot/www"

info()  { printf '\033[0;34m==>\033[0m %s\n' "$1"; }
ok()    { printf '\033[0;32m  ok\033[0m %s\n' "$1"; }
warn()  { printf '\033[0;33m  !!\033[0m %s\n' "$1"; }
fail()  { printf '\033[0;31m  xx\033[0m %s\n' "$1" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || fail "must run as root (certbot writes to /etc/letsencrypt)"
command -v certbot >/dev/null 2>&1 || fail "certbot not installed: apt install certbot"

DESIRED="$(printf '%s\n' $DOMAINS | grep -v '^$' | sort -u)"
DESIRED_COUNT="$(printf '%s\n' "$DESIRED" | wc -l | tr -d ' ')"

# ── 1. ACME webroot ─────────────────────────────────────────────────────────
info "ACME webroot"
mkdir -p "$WEBROOT/.well-known/acme-challenge"
ok "$WEBROOT"

# ── 2. DNS must resolve before certbot is called ────────────────────────────
# A validation failure counts against Let's Encrypt's rate limit, so check
# first rather than letting certbot discover it.
info "DNS resolution"
UNRESOLVED=""
while read -r d; do
    [ -z "$d" ] && continue
    if [ -z "$(dig +short "$d" A 2>/dev/null | head -1)" ]; then
        UNRESOLVED="$UNRESOLVED $d"
    fi
done <<< "$DESIRED"
if [ -n "$UNRESOLVED" ]; then
    fail "these do not resolve, create the DNS records first:$UNRESOLVED"
fi
ok "all $DESIRED_COUNT hostnames resolve"

# ── 3. Deploy hook ──────────────────────────────────────────────────────────
# Installed before issuance so the very first renewal already reloads nginx.
info "certbot deploy hook"
mkdir -p "$HOOK_DIR"
install -m 0755 "$SCRIPT_DIR/certbot-deploy-hook.sh" "$HOOK_DEST"
ok "$HOOK_DEST"

# ── 4. Certificate ──────────────────────────────────────────────────────────
info "certificate for $PRIMARY"
CURRENT=""
if certbot certificates --cert-name "$PRIMARY" 2>/dev/null | grep -q "Domains:"; then
    CURRENT="$(certbot certificates --cert-name "$PRIMARY" 2>/dev/null \
        | awk '/Domains:/{ $1=""; print }' | tr ' ' '\n' | grep -v '^$' | sort -u)"
fi

if [ "$CURRENT" = "$DESIRED" ]; then
    ok "already covers exactly the desired $DESIRED_COUNT hostnames; nothing to do"
else
    if [ -z "$CURRENT" ]; then
        info "no existing certificate — issuing"
    else
        info "SAN list differs — expanding"
        printf '     have: %s\n' "$(printf '%s ' $CURRENT)"
        printf '     want: %s\n' "$(printf '%s ' $DESIRED)"
    fi

    CERTBOT_ARGS=()
    while read -r d; do
        [ -z "$d" ] && continue
        CERTBOT_ARGS+=(-d "$d")
    done <<< "$(printf '%s\n' "$PRIMARY"; printf '%s\n' "$DESIRED" | grep -vx "$PRIMARY")"

    certbot certonly \
        --webroot -w "$WEBROOT" \
        --cert-name "$PRIMARY" \
        "${CERTBOT_ARGS[@]}" \
        --expand --non-interactive --agree-tos --keep-until-expiring
    ok "certificate issued/expanded"

    # Deploy hooks fire only on renewal, not on this path, so reload explicitly.
    "$HOOK_DEST" || warn "reload failed; run: docker exec $NGINX_CONTAINER nginx -s reload"
fi

# ── 5. Verify what is actually being served ─────────────────────────────────
info "verifying served certificate"
LIVE_SANS="$(echo \
    | openssl s_client -connect "$PRIMARY:443" -servername "$PRIMARY" 2>/dev/null \
    | openssl x509 -noout -text 2>/dev/null \
    | grep -o 'DNS:[^,]*' | sed 's/DNS://' | sort -u)"
MISSING=""
while read -r d; do
    [ -z "$d" ] && continue
    printf '%s\n' "$LIVE_SANS" | grep -qx "$d" || MISSING="$MISSING $d"
done <<< "$DESIRED"
if [ -n "$MISSING" ]; then
    warn "served certificate is missing:$MISSING"
    warn "nginx may not have reloaded — try: docker exec $NGINX_CONTAINER nginx -s reload"
else
    ok "served certificate covers all $DESIRED_COUNT hostnames"
fi

# ── 6. Prove renewal works end to end ───────────────────────────────────────
# The single most valuable check here: it exercises the real webroot path
# through nginx. Without it you find out at expiry.
info "renewal dry run"
if certbot renew --cert-name "$PRIMARY" --dry-run --webroot -w "$WEBROOT" >/tmp/certbot-dryrun.log 2>&1; then
    ok "renewal path works"
else
    warn "dry run FAILED — renewals will not succeed. See /tmp/certbot-dryrun.log"
    tail -20 /tmp/certbot-dryrun.log >&2
    exit 1
fi

# ── 7. Renewal timer ────────────────────────────────────────────────────────
info "renewal timer"
if systemctl list-timers --all 2>/dev/null | grep -q certbot; then
    ok "certbot timer is present"
elif [ -f /etc/cron.d/certbot ]; then
    ok "certbot cron job is present"
else
    warn "no certbot timer or cron found — renewals will NOT run automatically"
    warn "enable with: systemctl enable --now certbot.timer"
fi

printf '\n\033[0;32mTLS converged.\033[0m %s hostnames on one certificate, hook installed, renewal verified.\n' "$DESIRED_COUNT"