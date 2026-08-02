# DNS, TLS and Service Routing

Every service is reached through nginx on its own subdomain. Only ports 80 and
443 are open to the internet; everything else is bound to loopback or reached
over the Docker network.

## Routing map

| Hostname | nginx sends it to | Notes |
| --- | --- | --- |
| `masjid-app.isquaretechsolutions.com` | `host.docker.internal:3000` | Frontend (separate compose project) |
| `api.masjid-app.…` | `app:8080` | Backend |
| `logs.masjid-app.…` | `masjid-grafana:3000` | Resolved at request time; tolerates the stack being down |
| `vault.masjid-app.…` | `host.docker.internal:8088` | Infisical (separate stack) |
| `s3.masjid-app.…` | `masjid-minio:9000` | Object storage, browser-facing |
| `console.masjid-app.…` | `masjid-minio:9001` | MinIO admin UI — consider an IP allowlist |

DNS carries no port information. The browser connects to 443 because the URL
says `https`, and nginx picks the destination from the `Host` header. That is
why a service not listed above cannot be reached by hostname at all.

## Step 1 — DNS records

For click-by-click GoDaddy instructions, see
[godaddy-dns-setup.md](./godaddy-dns-setup.md).

Create five A records, all pointing at the VPS:

```
api.masjid-app.isquaretechsolutions.com.      A   72.62.6.65
logs.masjid-app.isquaretechsolutions.com.     A   72.62.6.65
vault.masjid-app.isquaretechsolutions.com.    A   72.62.6.65
s3.masjid-app.isquaretechsolutions.com.       A   72.62.6.65
console.masjid-app.isquaretechsolutions.com.  A   72.62.6.65
```

Wait for propagation before step 2 — certbot fails if a name does not resolve:

```bash
for h in api logs vault s3 console; do
  echo "$h -> $(dig +short $h.masjid-app.isquaretechsolutions.com)"
done
```

## Step 2 — Converge TLS

One command, run on the VPS as root:

```bash
sudo ~/masjid-app/scripts/tls/setup-tls.sh
```

It is idempotent — safe to re-run, and does nothing when the certificate
already covers the declared hostnames. That matters: Let's Encrypt allows only
5 duplicate certificates per week, so a script that reissued on every run would
exhaust the limit and block real renewals.

What it does:

1. Creates the ACME webroot
2. **Checks DNS resolves first** — a failed validation counts against the rate
   limit, so it refuses to call certbot until every hostname resolves
3. Installs the certbot deploy hook
4. Compares the certificate's current SANs against `domains.conf` and issues or
   expands only if they differ
5. Verifies the certificate actually being *served* covers every hostname —
   catching the case where certbot succeeded but nginx never reloaded
6. Runs `certbot renew --dry-run` to prove the renewal path works end to end
7. Confirms a renewal timer or cron job exists

All nginx blocks deliberately point at the **same** certificate path, so nginx
starts fine before this runs. Until it does, the new subdomains serve a
certificate that does not list them and browsers warn. Nothing is broken; the
warning clears once this completes.

### Adding a subdomain later

1. Create the DNS A record — see [godaddy-dns-setup.md](./godaddy-dns-setup.md)
2. Add the hostname to `scripts/tls/domains.conf`
3. Add a `server` block to `nginx/conf.d/app.conf`
4. Deploy, then re-run `setup-tls.sh` — it detects the new name and expands

`PRIMARY` in `domains.conf` must not change. Certbot names the lineage directory
after it and every `ssl_certificate` path depends on that name.

### Why renewal needs a hook

nginx reads certificates into memory at startup. Certbot renewing them on disk
changes nothing until nginx reloads — so without
`/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh`, renewals succeed
silently and nginx keeps serving the old certificate until it expires.

The hook lives in the repo at `scripts/tls/certbot-deploy-hook.sh` and is
re-installed by **every deploy**, so it cannot drift or disappear if the box is
rebuilt. It validates the config with `nginx -t` before reloading, and treats a
stopped nginx as a non-error — the renewal itself still succeeded, and nginx
picks up the new certificate whenever it next starts.

Renewal scheduling is left to certbot's own systemd timer. It is deliberately
not driven from CI: a deploy should not depend on Let's Encrypt being
reachable.

## Step 3 — Update Infisical

These are read at deploy time. Change them in Infisical, not in the compose
file.

| Variable | New value | Why |
| --- | --- | --- |
| `MINIO_PUBLIC_URL` | `https://s3.masjid-app.isquaretechsolutions.com` | Builds the image URLs browsers load. If this stays `http://<ip>:9000`, an HTTPS page loads images over HTTP and browsers block them as mixed content. |
| `MINIO_ENDPOINT` | `http://minio:9000` | Leave internal. This is the SDK's own connection over the Docker network, not browser-facing. |
| `MINIO_SECURE` | `false` | Refers to `MINIO_ENDPOINT` above, which stays plain HTTP inside the network. |
| `FRONTEND_URL` | `https://masjid-app.isquaretechsolutions.com` | Used for redirects back to the app. |
| `INFISICAL_DOMAIN` (workflow) | `https://vault.masjid-app.…` | Optional. Currently the raw IP over plain HTTP. |

## Step 4 — Deploy order

The frontend image bakes `BACKEND_API_URL` in at build time, so it needs a
rebuild, not just a restart.

1. Deploy the **backend** (nginx config, CORS, loopback port binds).
2. Deploy the **frontend** — a full workflow run, so the image is rebuilt
   against `https://api.masjid-app.…`.
3. Redeploy the **logging stack** so Grafana picks up its new root URL:
   `docker compose -f docker-compose.logging.yml up -d`.

## Step 5 — Verify

```bash
for h in "" api. logs. vault. s3.; do
  printf '%-40s %s\n' "$h" \
    "$(curl -s -o /dev/null -w '%{http_code}' https://${h}masjid-app.isquaretechsolutions.com/)"
done

# The API specifically
curl -s https://api.masjid-app.isquaretechsolutions.com/api/v1/actuator/health

# CORS now applies — the frontend origin must be allowed
curl -si -X OPTIONS https://api.masjid-app.isquaretechsolutions.com/api/v1/admin/auth/login \
  -H 'Origin: https://masjid-app.isquaretechsolutions.com' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: authorization,content-type,x-request-id' \
  | grep -i 'access-control-allow'
```

The preflight must return `Access-Control-Allow-Origin` matching the frontend
and `Access-Control-Allow-Credentials: true`. If it does not, login will fail —
that is the main risk of moving the API to its own subdomain.

Then log in through the UI and leave it idle past the access-token expiry (30
minutes) to confirm the refresh cookie still works cross-subdomain. It should:
both names share the registrable domain `isquaretechsolutions.com`, so the
cookie is same-site and unaffected by third-party cookie restrictions. Worth
confirming rather than assuming.

## Ports

Only 80/443 face the internet. These are bound to loopback and reachable over
an SSH tunnel:

```bash
ssh -L 5432:127.0.0.1:5432 -L 8080:127.0.0.1:8080 -L 9001:127.0.0.1:9001 <user>@<host>
```

Postgres previously listened on `0.0.0.0:5432`, i.e. the database was reachable
from the public internet. Note that **Docker's published ports bypass `ufw`** —
Docker inserts its own iptables rules ahead of the ufw chain, so a firewall
rule blocking 5432 would not have helped. Check from off the box:

```bash
nmap -Pn -p 5432,8080,9000,9001,8088 72.62.6.65
```

The frontend's port 3000 stays on `0.0.0.0` because nginx reaches it through
the Docker bridge gateway, which cannot see a loopback-only bind. To close it,
put the frontend on `masjid-network` and proxy to `masjid-frontend:3000` —
using the same request-time resolver pattern as the Grafana block, or nginx
will refuse to start whenever the frontend is down.

## Rollback

`git revert` the commit and redeploy. The old routing was path-based on a
single hostname, so nothing in DNS needs undoing — the extra records simply go
unused.