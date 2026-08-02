# Post-Deploy Checklist

For the subdomain-routing release. Work top to bottom — each step gates the
next. If one fails, stop and fix or roll back before continuing.

Highest-risk items are first: nginx is the single front door, so a config
problem takes down every service at once.

---

## 1. Did nginx actually start? (do this within seconds)

```bash
docker compose -f ~/masjid-app/docker-compose.yml ps
docker logs masjid-nginx --tail 30
```

`masjid-nginx` must be **Up**, not `Restarting`. Look for `[emerg]` in the log
— the usual causes are a missing certificate file or an unresolvable
`proxy_pass` hostname.

```bash
curl -sI https://masjid-app.isquaretechsolutions.com/ | head -1   # expect 200
```

**If nginx is crash-looping the entire site is down.** Roll back now (section
10) and investigate afterwards.

---

## 2. Is the database still there?

Container names changed this release; volume names deliberately did not. Verify
the old volumes are still attached and populated.

```bash
docker volume ls | grep masjid-app
#   masjid-app_db-data-dev     <- must be this exact name
#   masjid-app_minio-data

docker exec masjid-db psql -U "$DB_USERNAME" -d masjid_app \
  -c "\dt" -c "select count(*) from campaigns;"
```

If you see an empty schema, **stop**. The old data is intact in the original
volume; the app is pointing at a new empty one. Do not let writes accumulate —
fix the volume name first.

---

## 3. Are the old containers gone?

```bash
docker ps -a --format '{{.Names}}\t{{.Status}}' | grep masjid
```

Expect `masjid-backend`, `masjid-db`, `masjid-minio`, `masjid-nginx`. Any
leftover `-dev`-suffixed container still running would be holding a port.

---

## 4. Are the internal ports actually closed?

Run this **from your laptop, not the VPS**:

```bash
nmap -Pn -p 5432,8080,9000,9001,8088,80,443 72.62.6.65
```

Only 80 and 443 should be `open`. 5432 in particular must not be — that was the
database exposed to the internet.

Confirm you can still reach them through a tunnel:

```bash
ssh -L 5432:127.0.0.1:5432 <user>@72.62.6.65
```

---

## 5. Expand the certificate

Until this runs, the new subdomains serve a cert that does not list them and
browsers show a full-page warning.

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

Keep that domain order — certbot names the directory after the first `-d`, and
`app.conf` depends on that path.

Verify all six names are on the cert:

```bash
echo | openssl s_client -connect masjid-app.isquaretechsolutions.com:443 \
  -servername masjid-app.isquaretechsolutions.com 2>/dev/null \
  | openssl x509 -noout -text | grep -o "DNS:[^,]*"
```

---

## 6. Does each subdomain reach its own service?

```bash
for h in "" api. logs. vault. s3. console.; do
  printf '%-12s %s\n' "${h:-root}" \
    "$(curl -s -o /dev/null -w '%{http_code}' https://${h}masjid-app.isquaretechsolutions.com/)"
done
```

No `-k` — the point is that certificates validate now.

Then confirm they serve *different* things. If every hostname returns the
frontend title, nginx is falling through to its default block and the new
config did not take effect:

```bash
for h in "" api. logs. vault. s3.; do
  printf '%-12s %s\n' "${h:-root}" \
    "$(curl -s https://${h}masjid-app.isquaretechsolutions.com/ \
       | grep -oiE '<title>[^<]*</title>|grafana|minio|infisical' | head -1)"
done
```

---

## 7. CORS — the most likely thing to break login

The API is a separate origin now, so CORS applies where it never did before.

```bash
curl -si -X OPTIONS https://api.masjid-app.isquaretechsolutions.com/api/v1/admin/auth/login \
  -H 'Origin: https://masjid-app.isquaretechsolutions.com' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: authorization,content-type,x-request-id' \
  | grep -i 'access-control-allow'
```

Must return `Access-Control-Allow-Origin` matching the frontend and
`Access-Control-Allow-Credentials: true`. If it does not, the admin panel
cannot log in.

---

## 8. Auth flow end to end

Not scriptable — do it in a browser:

1. Log in to the admin panel.
2. Confirm data loads (campaigns, events, prayer times).
3. **Leave the tab idle for 30+ minutes**, past the access-token expiry, then
   click something.

Step 3 is the real test. The refresh token is an HttpOnly cookie and the API is
now on a different subdomain. It *should* work — both names share the
registrable domain, so the cookie stays same-site — but this is the assumption
most worth confirming. If you get logged out, the cookie is not crossing.

---

## 9. Remaining checks

**Images** — update `MINIO_PUBLIC_URL` in Infisical to
`https://s3.masjid-app.isquaretechsolutions.com` and redeploy the backend, then
open a page with uploaded images and check the browser console. Mixed-content
blocks appear there, not as visible errors.

**Logs** — after redeploying the logging stack
(`docker compose -f docker-compose.logging.yml up -d`), open
`https://logs.masjid-app.isquaretechsolutions.com` and run:

```logql
{container=~"masjid-.*"}
```

All containers should appear. nginx access lines should now carry a `host`
field distinguishing the subdomains.

**Correlation IDs** — trigger a failure in the UI, note the `(ref: xxxxxxxx)` in
the error message, and search `{container=~"masjid-.*"} |= "xxxxxxxx"`. It
should return both the nginx access line and the backend log line.

**Certificate renewal** — the ACME webroot is newly mounted, so exercise it
before it matters:

```bash
sudo certbot renew --dry-run
```

---

## 10. Rollback

```bash
git revert <commit> && git push        # triggers redeploy
```

Or immediately on the box, if the site is down:

```bash
cd ~/masjid-app
git -C <your checkout> show HEAD~1:nginx/conf.d/app.conf > nginx/conf.d/app.conf
docker compose restart nginx
```

Nothing in DNS needs undoing — the extra records simply go unused. The expanded
certificate is harmless to keep.

**Not covered by rollback:** if the database ever came up on a fresh volume,
reverting the code will not move data back. That is why section 2 comes before
everything else.
