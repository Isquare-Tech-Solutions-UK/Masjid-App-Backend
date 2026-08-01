# Mobile Stripe Integration Guide

How the mobile app integrates donations with the Masjid App backend.

**Model:** the charity uses its **own** Stripe account. The **backend** creates a
PaymentIntent with the charity's secret key; the **mobile app** presents Stripe's
**Payment Sheet** to collect the card and confirm. The app never handles the secret
key and never sees raw card data (Stripe's SDK does), so it stays PCI-light.

---

## 1. Prerequisites

**Install the Stripe SDK**
- iOS: `StripePaymentSheet` (Swift Package Manager / CocoaPods)
- Android: `com.stripe:stripe-android`
- React Native: `@stripe/stripe-react-native`

**Base URL & auth**
- All endpoints below are under: `{BASE_URL}/api/v1` (dev: `http://localhost:8080/api/v1`).
- Member (mobile) endpoints require the header **`X-API-KEY: <member-api-key>`**
  (value supplied by the backend team per environment — do **not** hardcode it in source; read from secure config).
- All responses are wrapped: `{ "data": <payload>, "meta": {...} }`. Errors:
  `{ "error": { "code": "...", "message": "..." }, "meta": {...} }` with an HTTP 4xx status.

---

## 2. APIs the app consumes

| # | Method & path | Purpose |
|---|---|---|
| 1 | `GET /member/stripe/publishable-key` | Get the publishable key to initialise the Stripe SDK (once, at startup) |
| 2 | `GET /member/campaigns?page=0&size=20` | List active campaigns to donate to |
| 3 | `POST /member/campaigns/{campaignId}/donate` | Create a donation → returns the data for the Payment Sheet |
| 4 | `GET /member/donations/{donationId}/status` | Check the donation status (pending / completed / failed) |
| 5 | `POST /member/donations/{donationId}/cancel` | Cancel an unpaid donation (user abandoned / started over) |

### 1) Get publishable key
```
GET /member/stripe/publishable-key
X-API-KEY: <member-api-key>
```
```json
{ "data": { "publishableKey": "pk_live_..." } }
```
Use it to initialise the Stripe SDK once when the app starts.

### 2) List campaigns
```
GET /member/campaigns?page=0&size=20
X-API-KEY: <member-api-key>
```
```json
{ "data": { "content": [ { "id": "uuid", "title": "Mosque Renovation Fund", ... } ],
            "pagination": { "page": 0, "size": 20, "totalElements": 4, ... } } }
```

### 3) Create a donation (the key call)
```
POST /member/campaigns/{campaignId}/donate
X-API-KEY: <member-api-key>
Content-Type: application/json

{ "donorName": "Ahmed Ali", "amount": 10.00, "isAnonymous": false }
```
Response:
```json
{
  "data": {
    "donationId":     "9f1f6aa9-...",
    "paymentToken":   "pi_3Tz..._secret_xVv4...",   // ← PaymentIntent client secret (for the Payment Sheet)
    "publishableKey": "pk_live_...",
    "amount":         "10.00",   // the donation (masjid receives this)
    "processingFee":  "0.36",    // Stripe fee the donor covers
    "totalCharged":   "10.36",   // what the card is actually charged
    "currency":       "gbp"
  }
}
```
> **`paymentToken` IS the PaymentIntent client secret.** It was renamed to avoid confusion
> with the Stripe *secret key* (which never leaves the backend). Pass `paymentToken` into the
> Stripe SDK wherever it asks for a `clientSecret` / `paymentIntentClientSecret`.

**Amounts:** the card is charged **`totalCharged`** (donation + Stripe fee); the masjid
receives **`amount`**. Show the donor the breakdown before they pay.

### 4) Check donation status
```
GET /member/donations/{donationId}/status
X-API-KEY: <member-api-key>
```
```json
{ "data": { "status": "completed", "amount": 10.00, "totalCharged": 10.36, ... } }
```
`status` is one of: **`pending`**, **`completed`**, **`failed`**.

### 5) Cancel an unpaid donation
```
POST /member/donations/{donationId}/cancel
X-API-KEY: <member-api-key>
```
- `pending` → voids the PaymentIntent on Stripe and marks the donation `failed`. Returns 200.
- Already `failed` → 200 (idempotent no-op).
- Already `completed` (paid) → **400** `"This donation has already been paid and cannot be canceled"`.

---

## 3. Happy-path flow

```
App startup ─ GET /member/stripe/publishable-key ─ init Stripe SDK
                                │
User taps "Donate £10" ─ POST /member/campaigns/{id}/donate
                                │  → { donationId, paymentToken, publishableKey, amount, fee, total }
                                │  (persist { donationId, paymentToken, campaignId, amount } locally)
                                ▼
Present Payment Sheet with paymentToken (clientSecret)
                                │
                     ┌──────────┴──────────┐
                completed                canceled / failed
                    │                        │
        Poll GET .../status           (see §5 / §4)
        until "completed"             let user retry the SAME sheet
        (backend confirms via
         webhook), then show
         success + clear local state
```

**Important:** treat the payment as *done* only when `GET /member/donations/{id}/status`
returns **`completed`** — the backend confirms the payment server-side via a Stripe
**webhook**, not from the app. The Payment Sheet returning `.completed` means the charge
succeeded on Stripe; poll status (a couple of times, ~1–2s apart) to reflect the confirmed
donation in your UI.

---

## 4. Presenting the Payment Sheet

### iOS (Swift — StripePaymentSheet)
```swift
import StripePaymentSheet

// once, at startup:
STPAPIClient.shared.publishableKey = publishableKey   // from GET /member/stripe/publishable-key

// after POST /donate returns `paymentToken`:
var config = PaymentSheet.Configuration()
config.merchantDisplayName = "Masjid App"
config.allowsDelayedPaymentMethods = false

let paymentSheet = PaymentSheet(paymentIntentClientSecret: paymentToken, configuration: config)
paymentSheet.present(from: self) { result in
    switch result {
    case .completed:            // charge succeeded → poll GET /donations/{id}/status
    case .canceled:             // user dismissed → keep the donation, allow resume (§5)
    case .failed(let error):    // show error → user can retry the SAME sheet
    }
}
```

### Android (Kotlin — stripe-android)
```kotlin
// once, at startup:
PaymentConfiguration.init(context, publishableKey)

// in your Activity/Fragment:
private val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

// after POST /donate returns `paymentToken`:
paymentSheet.presentWithPaymentIntent(
    paymentIntentClientSecret = paymentToken,
    PaymentSheet.Configuration(merchantDisplayName = "Masjid App")
)

private fun onPaymentSheetResult(result: PaymentSheetResult) {
    when (result) {
        is PaymentSheetResult.Completed -> { /* poll GET /donations/{id}/status */ }
        is PaymentSheetResult.Canceled  -> { /* keep donation, allow resume (§5) */ }
        is PaymentSheetResult.Failed    -> { /* show result.error, allow retry */ }
    }
}
```

### React Native (@stripe/stripe-react-native)
```tsx
// wrap the app:
<StripeProvider publishableKey={publishableKey}>...</StripeProvider>

// after POST /donate returns `paymentToken`:
const { error: initError } = await initPaymentSheet({
  paymentIntentClientSecret: paymentToken,
  merchantDisplayName: 'Masjid App',
});
const { error } = await presentPaymentSheet();
if (!error) {
  // completed → poll GET /member/donations/{id}/status
} else if (error.code === 'Canceled') {
  // keep donation, allow resume (§5)
} else {
  // show error, allow retry
}
```

---

## 5. Handling interruptions & resume (reuse the same PaymentIntent)

**The problem:** the user starts a donation (PaymentIntent created), then closes the app,
loses connection, or backgrounds it while entering card details.

**The key fact:** a PaymentIntent and its `paymentToken` (client secret) **stay valid**. A
canceled/failed attempt returns the PaymentIntent to a payable state, so you can **re-present
the same Payment Sheet with the same `paymentToken`** to finish the *same* payment. **Do not
create a new donation on every attempt** — reuse the existing one.

### Do this
1. **When you create a donation**, persist locally (secure storage / local DB):
   `{ donationId, paymentToken, campaignId, amount }` and a flag `inProgress = true`.
2. **On Payment Sheet `canceled`/`failed`**: keep the stored donation. Let the user tap
   "Continue" to **re-present the Payment Sheet with the same `paymentToken`** — no new API call.
3. **On app relaunch / returning to the donate screen**, if a stored `inProgress` donation exists:
   - Call `GET /member/donations/{donationId}/status`:
     - **`pending`** → still resumable. Re-present the Payment Sheet with the stored `paymentToken`.
     - **`completed`** → it was actually paid. Show success, clear local state.
     - **`failed`** → the PaymentIntent was canceled (by the backend, Stripe auto-cleanup, or a
       previous "start over"). The `paymentToken` is dead — clear local state and start fresh.
4. **On "Start over" / changing the amount or campaign**: call
   `POST /member/donations/{donationId}/cancel` for the old donation, clear local state, then
   create a **new** donation. This prevents orphan `pending` donations.

### Resume logic (pseudocode)
```
onResumeDonateScreen():
  saved = localStore.get("inProgressDonation")
  if saved == null: return startFreshDonation()

  status = GET /member/donations/{saved.donationId}/status
  switch status:
    "pending":   presentPaymentSheet(saved.paymentToken)   // resume same PaymentIntent
    "completed": showSuccess(); localStore.clear()
    "failed":    localStore.clear(); startFreshDonation()

onStartOver():
  if saved != null: POST /member/donations/{saved.donationId}/cancel
  localStore.clear()
  startFreshDonation()
```

### Safety net (already handled server-side)
If the app never comes back (deleted, etc.), the donation does **not** stay pending forever:
Stripe automatically cancels abandoned PaymentIntents, and the backend's
`payment_intent.canceled` webhook flips the donation to `failed`. So server cleanup happens
even if the app does nothing — but calling `/cancel` on "start over" makes it immediate.

---

## 6. Small but important client rules

- **Create the PaymentIntent late** — only when the user commits to paying (taps "Donate £X"),
  not on screen load. Fewer intents = fewer abandoned ones.
- **Disable the Donate button after tap** until the request returns, to avoid double-submits
  (which would create two donations — the backend uses idempotency on the Stripe call, but it
  can't dedupe two separate `POST /donate` requests).
- **Show the fee breakdown** (`amount` + `processingFee` = `totalCharged`) before payment so the
  donor knows the card is charged `totalCharged` and the masjid receives `amount`.
- **Multiple payment methods** (Apple Pay / Google Pay / Link) appear automatically in the
  Payment Sheet if the charity enables them in their Stripe dashboard — no app change needed.
  (Apple Pay needs a one-time domain/merchant setup on the charity's Stripe account.)

---

## 7. Testing (test mode)

Use the charity's **test** keys (`pk_test_` / `sk_test_`). Test cards:

| Card | Result |
|---|---|
| `4242 4242 4242 4242` | Success (US card → higher intl fee) |
| `4000 0082 6000 0000` | Success, **UK** card (masjid nets the exact amount) |
| `4000 0000 0000 9995` | Declined (insufficient funds) |
| `4000 0025 0000 3155` | Requires 3D Secure authentication |

Any future expiry, any CVC, any postcode. There's also a browser test harness at
`{BASE_URL}/donate-test.html` (dev only) that exercises the same endpoints + Payment Element.

---

## 8. Quick reference — statuses

| Status | Meaning | App action |
|---|---|---|
| `pending` | Donation created, not yet paid | Resumable — present/re-present the Payment Sheet |
| `completed` | Payment confirmed by webhook | Show success, clear local state |
| `failed` | Payment failed, canceled, or abandoned | Clear local state, offer to start fresh |
