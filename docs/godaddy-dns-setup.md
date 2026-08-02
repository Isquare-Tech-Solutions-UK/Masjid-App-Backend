# GoDaddy DNS Setup

Adding the subdomain records that put each service behind nginx. Do this
**before** the certbot step in [dns-and-tls.md](./dns-and-tls.md) — certbot
fails on names that do not yet resolve, and Let's Encrypt rate-limits failures.

Registered domain: **isquaretechsolutions.com**
VPS address: **72.62.6.65**

---

## ⚠️ The one thing that goes wrong

GoDaddy's **Name** field is *relative to your domain*. It appends
`.isquaretechsolutions.com` for you.

| | |
| --- | --- |
| ✅ Correct | `api.masjid-app` |
| ❌ Wrong | `api.masjid-app.isquaretechsolutions.com` |

Typing the full hostname produces
`api.masjid-app.isquaretechsolutions.com.isquaretechsolutions.com`, which
resolves to nothing. This is the single most common mistake here.

---

## Records to add

Five A records, identical except for the Name.

| # | Type | Name | Value | TTL |
| --- | --- | --- | --- | --- |
| 1 | A | `api.masjid-app` | `72.62.6.65` | 1/2 Hour |
| 2 | A | `logs.masjid-app` | `72.62.6.65` | 1/2 Hour |
| 3 | A | `vault.masjid-app` | `72.62.6.65` | 1/2 Hour |
| 4 | A | `s3.masjid-app` | `72.62.6.65` | 1/2 Hour |
| 5 | A | `console.masjid-app` | `72.62.6.65` | 1/2 Hour |

What each one is for:

| Hostname | Service |
| --- | --- |
| `api.masjid-app.isquaretechsolutions.com` | Backend API |
| `logs.masjid-app.isquaretechsolutions.com` | Grafana — log explorer |
| `vault.masjid-app.isquaretechsolutions.com` | Infisical — secrets manager |
| `s3.masjid-app.isquaretechsolutions.com` | MinIO — uploaded images |
| `console.masjid-app.isquaretechsolutions.com` | MinIO admin console |

> **Do not modify the existing `masjid-app` A record.** That is the live
> frontend. You are only adding new records alongside it.

---

## Steps

1. Sign in to GoDaddy → **My Products**
2. Find **isquaretechsolutions.com** → **DNS** (or *Manage DNS*)
3. Click **Add New Record**
4. Fill in:
   - **Type**: `A`
   - **Name**: `api.masjid-app`  ← relative, see the warning above
   - **Value**: `72.62.6.65`
   - **TTL**: `1/2 Hour`
5. **Save**
6. Repeat steps 3–5 for the remaining four names

Half-hour TTL is deliberate while setting up: a typo corrects itself in 30
minutes rather than an hour. Raise it to 1 Hour once everything works.

---

## Verify

All five must resolve before moving on:

```bash
for h in api logs vault s3 console; do
  printf '%-10s %s\n' "$h" "$(dig +short $h.masjid-app.isquaretechsolutions.com)"
done
```

Expected:

```
api        72.62.6.65
logs       72.62.6.65
vault      72.62.6.65
s3         72.62.6.65
console    72.62.6.65
```

Also confirm the existing frontend record is untouched:

```bash
dig +short masjid-app.isquaretechsolutions.com   # -> 72.62.6.65
```

---

## Troubleshooting

**A name returns nothing.** Usually just propagation — GoDaddy is typically
5–30 minutes. Query GoDaddy's own nameservers to see whether the record exists
yet, bypassing any cached answer:

```bash
dig +short api.masjid-app.isquaretechsolutions.com @ns01.domaincontrol.com
```

If that returns the IP but plain `dig` does not, it is propagation — wait. If
it returns nothing, the record is wrong or was not saved.

**A name resolves to the wrong thing, or the record list shows a doubled
domain.** You typed the full hostname in the Name field. Edit the record and
strip the `.isquaretechsolutions.com` suffix.

**Everything resolves but browsers warn about the certificate.** Expected at
this stage. The certificate does not list the new names yet — that is the
certbot `--expand` step in [dns-and-tls.md](./dns-and-tls.md). nginx is
configured to keep serving rather than refuse to start, so nothing is broken
in the meantime.

---

## Alternative: CNAME instead of A

Equally valid, and arguably better: point each new name at the existing host
rather than repeating the IP.

| Type | Name | Value |
| --- | --- | --- |
| CNAME | `api.masjid-app` | `masjid-app.isquaretechsolutions.com` |
| CNAME | `logs.masjid-app` | `masjid-app.isquaretechsolutions.com` |
| CNAME | `vault.masjid-app` | `masjid-app.isquaretechsolutions.com` |
| CNAME | `s3.masjid-app` | `masjid-app.isquaretechsolutions.com` |
| CNAME | `console.masjid-app` | `masjid-app.isquaretechsolutions.com` |

The IP then lives in exactly one record, so moving VPS later means editing one
line instead of six. Works fine with certbot HTTP-01, and all of these are
non-apex names, so CNAME is legal here.

A records are easier to scan at a glance; CNAMEs are less maintenance later.
Either is correct — just keep all five consistent.

---

## Next

Once all five resolve, continue with **step 2** of
[dns-and-tls.md](./dns-and-tls.md) — expanding the TLS certificate to cover the
new hostnames, then reloading nginx.