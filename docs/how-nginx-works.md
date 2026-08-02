# How nginx Works Here

A plain-language walkthrough of `nginx/conf.d/app.conf` — what it does, why it
is shaped this way, and how certificates are handled. Written for someone
touching this for the first time.

For the reference tables (routing map, DNS records, certbot steps) see
[dns-and-tls.md](./dns-and-tls.md). This document explains the *why*.

---

## The core idea

The VPS is one machine with one IP address. There are six services, each
listening on a different port internally. But the internet can only reach it on
**port 443** for `https://` — that is fixed by the protocol, not configuration.

nginx is the **one program listening on 443**. Everything from the internet
arrives there, and nginx decides which internal service answers.

Nothing else is reachable from outside. The database, backend and file storage
are bound to `127.0.0.1` — visible only from inside the machine.

```
        INTERNET
            │
            │  only ports 80 + 443 are open
            ▼
    ┌───────────────┐
    │     nginx     │  ← the only thing exposed
    └───────┬───────┘
            │  private, inside the machine
   ┌────────┼────────┬─────────┬──────────┐
   ▼        ▼        ▼         ▼          ▼
frontend  backend  grafana   minio    infisical
 :3000     :8080    :3000   :9000/1     :8088
```

It is a receptionist in a building lobby. Every visitor comes through the one
front door; the receptionist reads who they are asking for and directs them to
the right office. Nobody wanders the building directly.

---

## How it decides where to send things

Two pieces of information, in order.

### 1. Which hostname was asked for

Visiting `https://logs.masjid-app.isquaretechsolutions.com` involves two
separate things:

- **DNS** turns the name into an IP address (`72.62.6.65`). DNS knows nothing
  about ports or paths — it answers only "which machine?"
- The browser then sends **the name itself** along with the request, in a header
  called `Host`.

That `Host` header is what nginx routes on. The config has seven `server { }`
blocks, each declaring a `server_name`:

```nginx
server {
    listen 443 ssl;
    server_name logs.masjid-app.isquaretechsolutions.com;
    ...
    proxy_pass http://masjid-grafana:3000;
}
```

nginx compares the incoming `Host` against each `server_name` and picks the
match. **This is why six subdomains share one IP and one port** — they are
distinguished purely by the name the browser asked for.

### 2. Which path

Inside the matched server block, `location` blocks match on URL path. The root
domain has two:

```nginx
server_name masjid-app.isquaretechsolutions.com;

location /api/ { proxy_pass http://app:8080; }                   # backend
location /     { proxy_pass http://host.docker.internal:3000; }  # frontend
```

nginx picks the **longest matching prefix**. `/api/v1/campaigns` matches both
`/api/` and `/`, but `/api/` is longer, so the backend wins. `/dashboard` only
matches `/`, so it goes to the frontend.

### The full routing table

| Hostname asked for | nginx forwards to | Which is |
| --- | --- | --- |
| `masjid-app.…` + `/api/…` | `app:8080` | backend |
| `masjid-app.…` + anything else | `host.docker.internal:3000` | frontend |
| `api.masjid-app.…` | `app:8080` | backend |
| `logs.masjid-app.…` | `masjid-grafana:3000` | Grafana |
| `vault.masjid-app.…` | `host.docker.internal:8088` | Infisical |
| `s3.masjid-app.…` | `masjid-minio:9000` | file storage |
| `console.masjid-app.…` | `masjid-minio:9001` | storage admin |

Two address styles appear in that column, and the difference matters.

**`app:8080`, `masjid-minio:9000`** are *container names*. Docker runs a private
DNS service for containers on the same network, so nginx can say "the container
called `app`" and Docker resolves it. This traffic never leaves the machine.

**`host.docker.internal:3000`** means "the machine I am running on". The
frontend and Infisical live in *separate* Docker Compose projects, so nginx
cannot address them by container name — it goes out to the host and back in via
their published ports. This requires the `extra_hosts` entry on the nginx
service in `docker-compose.yml`.

### Why the root domain still has an `/api/` route

`api.masjid-app.…` exists, so the `/api/` route on the root domain looks
redundant. It is not, and removing it once took production login down.

The frontend calls `/api/v1/…` on its own origin. A frontend image built before
the subdomain move still does. With that location gone, those requests fell
through to `location /` → Next.js → whose internal rewrite targets the backend's
published port → which had just been rebound to loopback. Every login returned
500.

Keeping both routes means the frontend can migrate to the subdomain on its own
schedule, with no window where neither resolves. Same-origin calls also skip the
CORS preflight, so they are marginally faster.

---

## How the certificates work

### What HTTPS needs

Before any data moves, the browser demands proof the server is who it claims to
be. That proof is a **certificate** — a file signed by an authority browsers
already trust (here, Let's Encrypt, which is free).

The certificate lists which hostnames it is valid for. If the browser asked for
`vault.masjid-app.…` and the certificate does not name it, the browser refuses
and shows a full-page warning. That is what `ERR_CERT_COMMON_NAME_INVALID`
means — the certificate is real and valid, it simply does not *claim* that
hostname.

### One certificate, six names

Rather than six certificates, there is one listing all six as
Subject Alternative Names:

```
DNS:masjid-app.isquaretechsolutions.com
DNS:api.masjid-app.isquaretechsolutions.com
DNS:logs.masjid-app.isquaretechsolutions.com
DNS:vault.masjid-app.isquaretechsolutions.com
DNS:s3.masjid-app.isquaretechsolutions.com
DNS:console.masjid-app.isquaretechsolutions.com
```

Which is why every server block points at the *same* two files:

```nginx
ssl_certificate     /etc/letsencrypt/live/masjid-app.isquaretechsolutions.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/masjid-app.isquaretechsolutions.com/privkey.pem;
```

One file to renew, one path to configure. It also means nginx starts even before
a new hostname has been added to the certificate — the new name warns in a
browser rather than nginx refusing to boot.

### How Let's Encrypt proves domain ownership

Certificates are free but not given away; you must prove you control the domain.

1. `certbot` requests a certificate covering the six names
2. Let's Encrypt replies with a secret string per name: *"serve this at
   `/.well-known/acme-challenge/<random>`"*
3. certbot writes those files into a directory shared with nginx
4. Let's Encrypt fetches `http://<each-name>/.well-known/acme-challenge/<random>`
5. Correct content comes back → you demonstrably control the DNS and the
   server → certificate issued

This is what the first block in the config exists for:

```nginx
server {
    listen 80 default_server;
    server_name _;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;                    # serve the proof files
    }

    location / {
        return 301 https://$host$request_uri;     # everything else → HTTPS
    }
}
```

`server_name _;` means "any hostname", deliberately: a **new** subdomain can be
validated without editing this file first. Any plain-HTTP visitor is redirected
to HTTPS.

### Renewal, and the trap hiding in it

Let's Encrypt certificates expire after **90 days**. certbot has a scheduled
timer that renews around day 60.

Here is the subtle part. **nginx reads the certificate into memory when it
starts.** certbot replacing the file on disk changes nothing — nginx keeps
serving the old certificate from memory until told to re-read it.

Without intervention, renewals would succeed, nginx would keep serving an
expiring certificate, and the site would start throwing browser warnings on day
90 with nothing obviously broken and no failure to point at.

The fix is a **deploy hook** — a script certbot runs after a successful
renewal, at `scripts/tls/certbot-deploy-hook.sh`:

```sh
docker exec masjid-nginx nginx -t && docker exec masjid-nginx nginx -s reload
```

`nginx -t` checks the config is valid; `-s reload` makes nginx re-read its files
without dropping connections. The hook is re-installed by every deploy, so it
cannot drift or vanish if the box is rebuilt.

Confirmation it works, from a real run:

```
Hook 'deploy-hook' ran with output:
 certbot-deploy-hook: reloaded masjid-nginx after certificate renewal
```

---

## Two journeys through the system

### Loading the dashboard

```
1. Browser opens https://masjid-app.isquaretechsolutions.com/dashboard
2. DNS → 72.62.6.65, connect to port 443
3. nginx proves identity with the certificate      (TLS handshake)
4. Host: masjid-app.…            → frontend server block
5. Path /dashboard               → location /   (not /api/)
6. No X-Request-Id from browser  → nginx mints one
7. Forward to host.docker.internal:3000 → Next.js renders
8. nginx writes a JSON log line with the id, status and timing
9. HTML returns to the browser
```

### That page fetching data

```
1. Page JavaScript generates an id, calls /api/v1/campaigns
2. Same IP, same port 443
3. Host: masjid-app.…            → same server block
4. Path /api/v1/campaigns        → location /api/  (longer match wins)
5. An X-Request-Id WAS supplied  → nginx reuses it rather than minting
6. Forward to app:8080 over Docker's private network
7. Spring's RequestIdFilter puts that id in every log line it writes
8. Response returns, nginx logs it with the same id
```

Steps 5–7 are why one id appears in the browser console, the nginx log and the
backend logs. That is what makes this work in Grafana:

```logql
{container=~"masjid-.*"} |= "4f2a1c9d"
```

One search, the complete story of a single click.

---

## The parts that look odd, and what they prevent

### `map` — the request-ID rule

```nginx
map $http_x_request_id $req_id {
    default $http_x_request_id;   # caller sent one → keep it
    ""      $request_id;          # they did not → generate one
}
```

A small lookup table. Reusing an incoming id keeps a trace unbroken across
hops; generating one otherwise means nothing is ever untraceable.

### `resolver` — why a stopped container cannot kill the site

The most important lines in the file.

nginx normally resolves `proxy_pass` hostnames **once, at startup**. Write
`proxy_pass http://masjid-grafana:3000;` while Grafana is stopped and nginx
**refuses to start at all**. Since nginx is the only front door, the entire site
— donations included — would go down because a *logging* container was off.

So the Grafana block defers the lookup to request time:

```nginx
resolver 127.0.0.11 valid=10s ipv6=off;        # Docker's internal DNS
set $grafana_upstream http://masjid-grafana:3000;
proxy_pass $grafana_upstream;                   # a variable → resolved per request
```

Verified on the live box: with Grafana fully stopped, nginx started normally,
`/api/` kept returning 200, and only `logs.` returned 502. When Grafana came
back it recovered with no nginx restart.

Any future upstream in a *different* Compose project should use this pattern.

### `Host $host` on the S3 block

```nginx
proxy_set_header Host $host;
```

MinIO computes upload signatures *over the hostname*. Change it in transit and
every upload fails with `SignatureDoesNotMatch`. That header is passed through
untouched for this reason.

### `$connection_upgrade`

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}
```

WebSockets need `Connection: upgrade`, but sending it on ordinary requests
breaks connection reuse. This sends it only when a client actually asked to
upgrade. Grafana's live log tailing needs it.

---

## The logging line

```nginx
log_format json_combined escape=json
    '{ "time":…, "host":…, "status":…, "request_time":…, "requestId":… }';
access_log /dev/stdout json_combined;
```

nginx writes each request as **JSON to stdout**, not to a file. Docker captures
stdout, Alloy reads it from Docker, Loki stores it. No log files to rotate, and
because it is JSON, Grafana filters on `status` or `host` as real fields instead
of grepping text.

The `host` field is what makes "how much traffic is `s3.` getting?" answerable.

---

## Inspecting it yourself

```bash
# Full effective config, all includes resolved
docker exec masjid-nginx nginx -T

# Just the routing map
docker exec masjid-nginx nginx -T | grep -E 'server_name|proxy_pass'

# Check config validity WITHOUT applying it (always safe)
docker exec masjid-nginx nginx -t

# Apply changes with no downtime
docker exec masjid-nginx nginx -s reload

# Which names is the live certificate valid for?
echo | openssl s_client -connect masjid-app.isquaretechsolutions.com:443 \
  -servername masjid-app.isquaretechsolutions.com 2>/dev/null \
  | openssl x509 -noout -text | grep -o 'DNS:[^,]*'
```

The habit worth building: **always `nginx -t` before `nginx -s reload`**. A
failed test changes nothing — nginx carries on with the previous config. That is
why editing this file on a live server is safe even when a paste arrives
mangled.

---

## Adding a service

1. Create the DNS A record — [godaddy-dns-setup.md](./godaddy-dns-setup.md)
2. Add the hostname to `scripts/tls/domains.conf`
3. Add a `server` block to `nginx/conf.d/app.conf`, copying the closest
   existing one. Use the `resolver` pattern if the upstream is in a different
   Compose project.
4. Deploy, then run `sudo ~/masjid-app/scripts/tls/setup-tls.sh` — it notices
   the new name and expands the certificate

Never point a new block at a certificate path that does not exist yet. nginx
would refuse to start and take every other service down with it.
