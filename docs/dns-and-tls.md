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

## Step 2 — Expand the certificate

All nginx blocks deliberately point at the **existing** certificate path, so
nginx starts fine before this runs. Until you expand, the new subdomains serve
a certificate that does not list them and browsers will warn. Nothing is
broken; the warning goes away after this.

```bash
sudo certbot certonly --webroot -w ~/masjid-app/certbot/www \
  -d masjid-app.isquaretechsolutions.com \
  -d api.masjid-app.isquaretechsolutions.com \
  -d logs.masjid-app.isquaretechsolutions.com \
  -d vault.masjid-app.isquaretechsolutions.com \
  -d s3.masjid-app.isquaretechsolutions.com \
  -d console.masjid-app.isquaretechsolutions.com \
  --expand

docker compose -f ~/masjid-app/docker-compose.yml exec nginx nginx -s reload
```

One certificate carries every hostname as a SAN, which is why all the server
blocks can share a path. Keep the domain list in this order — certbot names the
directory after the first `-d`, and the paths in `app.conf` depend on it.

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