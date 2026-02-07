# Masjid App - API Contract Documentation

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Common Patterns](#common-patterns)
4. [Admin APIs](#admin-apis)
5. [Member (Mobile) APIs](#member-mobile-apis)
6. [Webhook APIs](#webhook-apis)

---

## Overview

### Base URL
```
Production: https://api.{masjid-domain}.com/api/v1
Development: http://localhost:8080/api/v1
```

### API Structure
| Path Prefix | Consumer | Authentication |
|-------------|----------|----------------|
| `/api/v1/admin/*` | Admin Web App | JWT Bearer Token |
| `/api/v1/member/*` | Mobile App | API Key |
| `/api/v1/webhooks/*` | External Services | Signature Verification |

### Content Type
All requests and responses use `application/json`

---

## Authentication

### Admin Authentication (JWT)

Admin endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Lifecycle:**
- Access Token: 15 minutes
- Refresh Token: 7 days
- Stored in HttpOnly cookies (refresh) + memory (access)

### Mobile Authentication (API Key)

Member endpoints require an API key in the header:
```
X-API-Key: ma_live_xxxxxxxxxxxxxxxxxxxx
```

---

## Common Patterns

### Standard Response Format

**Success Response:**
```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2026-01-29T08:00:00Z",
    "requestId": "req_abc123"
  }
}
```

**Success Response (List):**
```json
{
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  },
  "meta": {
    "timestamp": "2026-01-29T08:00:00Z",
    "requestId": "req_abc123"
  }
}
```

**Error Response:**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data",
    "details": [
      {
        "field": "email",
        "message": "must be a valid email address"
      }
    ]
  },
  "meta": {
    "timestamp": "2026-01-29T08:00:00Z",
    "requestId": "req_abc123"
  }
}
```

### Error Codes

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request data |
| 400 | `BAD_REQUEST` | Malformed request |
| 401 | `UNAUTHORIZED` | Missing or invalid token |
| 401 | `TOKEN_EXPIRED` | JWT token has expired |
| 401 | `INVALID_API_KEY` | Invalid or revoked API key |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 403 | `ACCOUNT_LOCKED` | Account locked due to failed attempts |
| 404 | `NOT_FOUND` | Resource not found |
| 409 | `CONFLICT` | Resource already exists |
| 429 | `RATE_LIMITED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Server error |

### Pagination Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Items per page (max: 100) |
| `sort` | string | varies | Sort field and direction (e.g., `createdAt,desc`) |

---

## Admin APIs

### Auth Module

#### POST `/api/v1/admin/auth/login`
Authenticate admin user and receive tokens.

**Request:**
```json
{
  "username": "admin",
  "password": "securePassword123"
}
```

**Response (200):**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 900,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "username": "admin",
      "email": "admin@masjid.com",
      "fullName": "Masjid Admin",
      "role": "admin"
    }
  },
  "meta": { ... }
}
```
*Note: Refresh token is set as HttpOnly cookie*

**Error Responses:**
- `401 UNAUTHORIZED` - Invalid credentials
- `403 ACCOUNT_LOCKED` - Account locked (returns `lockedUntil` timestamp)

---

#### POST `/api/v1/admin/auth/refresh`
Refresh access token using refresh token cookie.

**Request:** No body (uses HttpOnly cookie)

**Response (200):**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 900
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/auth/logout`
Invalidate refresh token and clear cookies.

**Response (200):**
```json
{
  "data": {
    "message": "Logged out successfully"
  },
  "meta": { ... }
}
```

---

#### GET `/api/v1/admin/auth/me`
Get current authenticated user details.

**Response (200):**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "admin",
    "email": "admin@masjid.com",
    "fullName": "Masjid Admin",
    "role": "admin",
    "lastLoginAt": "2026-01-29T07:30:00Z"
  },
  "meta": { ... }
}
```

---

#### PUT `/api/v1/admin/auth/change-password`
Change current user's password.

**Request:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword456"
}
```

**Response (200):**
```json
{
  "data": {
    "message": "Password changed successfully"
  },
  "meta": { ... }
}
```

---

### Prayer Times Module

#### GET `/api/v1/admin/prayer-times`
Get prayer times with pagination and filters.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `startDate` | date | Filter from date (YYYY-MM-DD) |
| `endDate` | date | Filter to date (YYYY-MM-DD) |
| `page` | integer | Page number |
| `size` | integer | Page size |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "date": "2026-01-29",
      "hijriDate": "29 Rajab 1447",
      "prayers": {
        "fajr": { "athan": "06:15", "jamah": "06:45" },
        "sunrise": { "athan": "07:45" },
        "zuhr": { "athan": "12:30", "jamah": "13:00" },
        "asr": { "athan": "15:00", "jamah": "15:30" },
        "maghrib": { "athan": "17:15", "jamah": "17:20" },
        "isha": { "athan": "18:45", "jamah": "19:15" }
      },
      "jumuahTimes": null,
      "createdAt": "2026-01-28T10:00:00Z",
      "updatedAt": "2026-01-28T10:00:00Z"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### GET `/api/v1/admin/prayer-times/{id}`
Get single prayer time entry by ID.

**Response (200):**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "date": "2026-01-29",
    "hijriDate": "29 Rajab 1447",
    "prayers": { ... },
    "jumuahTimes": null,
    "createdAt": "2026-01-28T10:00:00Z",
    "updatedAt": "2026-01-28T10:00:00Z"
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/prayer-times`
Create prayer time entry for a specific date.

**Request:**
```json
{
  "date": "2026-01-30",
  "hijriDate": "30 Rajab 1447",
  "prayers": {
    "fajr": { "athan": "06:14", "jamah": "06:44" },
    "sunrise": { "athan": "07:44" },
    "zuhr": { "athan": "12:30", "jamah": "13:00" },
    "asr": { "athan": "15:02", "jamah": "15:30" },
    "maghrib": { "athan": "17:17", "jamah": "17:22" },
    "isha": { "athan": "18:47", "jamah": "19:15" }
  },
  "jumuahTimes": null
}
```

**Response (201):**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "date": "2026-01-30",
    ...
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/prayer-times/bulk`
Create or update prayer times in bulk (for importing monthly timetables).

**Request:**
```json
{
  "prayerTimes": [
    {
      "date": "2026-02-01",
      "hijriDate": "1 Sha'ban 1447",
      "prayers": { ... }
    },
    {
      "date": "2026-02-02",
      "hijriDate": "2 Sha'ban 1447",
      "prayers": { ... }
    }
  ]
}
```

**Response (200):**
```json
{
  "data": {
    "created": 28,
    "updated": 2,
    "failed": 0
  },
  "meta": { ... }
}
```

---

#### PUT `/api/v1/admin/prayer-times/{id}`
Update prayer time entry.

**Request:**
```json
{
  "hijriDate": "29 Rajab 1447",
  "prayers": {
    "fajr": { "athan": "06:15", "jamah": "06:50" },
    ...
  }
}
```

**Response (200):** Returns updated prayer time object.

---

#### DELETE `/api/v1/admin/prayer-times/{id}`
Delete prayer time entry.

**Response (204):** No content

---

### Events Module

#### GET `/api/v1/admin/events`
Get all events with filters.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status (draft, published, cancelled, completed) |
| `category` | string | Filter by category |
| `startDate` | date | Filter events from date |
| `endDate` | date | Filter events to date |
| `search` | string | Search in title/description |
| `page` | integer | Page number |
| `size` | integer | Page size |
| `sort` | string | Sort field (default: `eventDate,asc`) |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Ramadan Preparation Workshop",
      "description": "Learn how to prepare spiritually for Ramadan",
      "category": "educational",
      "status": "published",
      "eventDate": "2026-02-15",
      "timing": {
        "startTime": "19:00",
        "endTime": "21:00",
        "doorsOpen": "18:30"
      },
      "location": {
        "venue": "Main Hall",
        "address": "123 Mosque Street, London",
        "capacity": 200,
        "coordinates": {
          "latitude": 51.5074,
          "longitude": -0.1278
        }
      },
      "speaker": {
        "name": "Sheikh Abdullah",
        "bio": "Islamic Scholar",
        "imageUrl": "https://cdn.example.com/speakers/sheikh-abdullah.jpg"
      },
      "media": {
        "featuredImageUrl": "https://cdn.example.com/events/ramadan-workshop.jpg",
        "featuredImageKey": "events/ramadan-workshop.jpg"
      },
      "registrationLink": "https://forms.google.com/...",
      "notificationSent": false,
      "notificationSentAt": null,
      "createdBy": "550e8400-e29b-41d4-a716-446655440000",
      "publishedAt": "2026-01-25T10:00:00Z",
      "createdAt": "2026-01-20T10:00:00Z",
      "updatedAt": "2026-01-25T10:00:00Z"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### GET `/api/v1/admin/events/{id}`
Get single event by ID.

---

#### POST `/api/v1/admin/events`
Create new event.

**Request:**
```json
{
  "title": "Ramadan Preparation Workshop",
  "description": "Learn how to prepare spiritually for Ramadan",
  "category": "educational",
  "eventDate": "2026-02-15",
  "timing": {
    "startTime": "19:00",
    "endTime": "21:00",
    "doorsOpen": "18:30"
  },
  "location": {
    "venue": "Main Hall",
    "address": "123 Mosque Street, London",
    "capacity": 200
  },
  "speaker": {
    "name": "Sheikh Abdullah",
    "bio": "Islamic Scholar"
  },
  "registrationLink": "https://forms.google.com/..."
}
```

**Response (201):** Returns created event.

---

#### PUT `/api/v1/admin/events/{id}`
Update event.

---

#### DELETE `/api/v1/admin/events/{id}`
Delete event (only draft events can be deleted).

**Response (204):** No content

---

#### PATCH `/api/v1/admin/events/{id}/status`
Update event status.

**Request:**
```json
{
  "status": "published"
}
```

**Response (200):**
```json
{
  "data": {
    "id": "...",
    "status": "published",
    "publishedAt": "2026-01-29T10:00:00Z"
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/events/{id}/notify`
Send push notification for event.

**Response (200):**
```json
{
  "data": {
    "message": "Notification sent successfully",
    "recipientCount": 1250
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/events/{id}/image`
Upload event featured image.

**Request:** `multipart/form-data`
| Field | Type | Description |
|-------|------|-------------|
| `image` | file | Image file (JPEG, PNG, WebP, max 5MB) |

**Response (200):**
```json
{
  "data": {
    "imageUrl": "https://cdn.example.com/events/abc123.jpg",
    "imageKey": "events/abc123.jpg"
  },
  "meta": { ... }
}
```

---

### Announcements Module

#### GET `/api/v1/admin/announcements`
Get all announcements.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status |
| `page` | integer | Page number |
| `size` | integer | Page size |
| `sort` | string | Sort field (default: `createdAt,desc`) |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Masjid Closure Notice",
      "message": "The masjid will be closed for maintenance on Friday.",
      "status": "sent",
      "isScheduled": false,
      "scheduledFor": null,
      "sentAt": "2026-01-28T15:00:00Z",
      "fcmMessageId": "fcm_msg_123456",
      "createdBy": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2026-01-28T14:55:00Z",
      "updatedAt": "2026-01-28T15:00:00Z"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/announcements`
Create and optionally send/schedule announcement.

**Request (Immediate Send):**
```json
{
  "title": "Important Notice",
  "message": "Please remember to bring your prayer mats.",
  "sendImmediately": true
}
```

**Request (Scheduled):**
```json
{
  "title": "Jumuah Reminder",
  "message": "Jumuah prayer starts at 1:30 PM today.",
  "isScheduled": true,
  "scheduledFor": "2026-01-31T12:00:00Z"
}
```

**Request (Draft):**
```json
{
  "title": "Draft Announcement",
  "message": "This is a draft message."
}
```

**Response (201):**
```json
{
  "data": {
    "id": "...",
    "title": "Important Notice",
    "status": "sent",
    "sentAt": "2026-01-29T10:00:00Z",
    ...
  },
  "meta": { ... }
}
```

---

#### GET `/api/v1/admin/announcements/{id}`
Get single announcement.

---

#### PUT `/api/v1/admin/announcements/{id}`
Update announcement (only draft/scheduled).

---

#### DELETE `/api/v1/admin/announcements/{id}`
Delete announcement (only draft).

---

#### POST `/api/v1/admin/announcements/{id}/send`
Send a draft announcement immediately.

**Response (200):**
```json
{
  "data": {
    "message": "Announcement sent successfully",
    "recipientCount": 1250
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/announcements/{id}/cancel`
Cancel a scheduled announcement.

---

### Campaigns Module

#### GET `/api/v1/admin/campaigns`
Get all campaigns.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status |
| `category` | string | Filter by category |
| `page` | integer | Page number |
| `size` | integer | Page size |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Masjid Roof Repair Fund",
      "description": "Help us repair the masjid roof before winter.",
      "category": "masjid_development",
      "status": "active",
      "goalAmount": 25000.00,
      "raisedAmount": 15750.50,
      "donorCount": 127,
      "progressPercentage": 63.0,
      "startDate": "2026-01-01",
      "endDate": "2026-03-31",
      "media": {
        "featuredImageUrl": "https://cdn.example.com/campaigns/roof-repair.jpg",
        "featuredImageKey": "campaigns/roof-repair.jpg"
      },
      "createdBy": "550e8400-e29b-41d4-a716-446655440000",
      "publishedAt": "2026-01-01T00:00:00Z",
      "endedAt": null,
      "createdAt": "2025-12-28T10:00:00Z",
      "updatedAt": "2026-01-29T08:00:00Z"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### GET `/api/v1/admin/campaigns/{id}`
Get single campaign with detailed stats.

**Response (200):**
```json
{
  "data": {
    "id": "...",
    "title": "Masjid Roof Repair Fund",
    ...
    "stats": {
      "totalDonations": 127,
      "averageDonation": 124.02,
      "largestDonation": 1000.00,
      "anonymousDonations": 23,
      "donationsToday": 5,
      "amountToday": 350.00
    }
  },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/campaigns`
Create new campaign.

**Request:**
```json
{
  "title": "Masjid Roof Repair Fund",
  "description": "Help us repair the masjid roof before winter.",
  "category": "masjid_development",
  "goalAmount": 25000.00,
  "startDate": "2026-01-01",
  "endDate": "2026-03-31"
}
```

---

#### PUT `/api/v1/admin/campaigns/{id}`
Update campaign.

---

#### PATCH `/api/v1/admin/campaigns/{id}/status`
Update campaign status.

**Request:**
```json
{
  "status": "active"
}
```

---

#### DELETE `/api/v1/admin/campaigns/{id}`
Delete campaign (only draft, no donations).

---

#### GET `/api/v1/admin/campaigns/{id}/donations`
Get donations for a campaign.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status |
| `page` | integer | Page number |
| `size` | integer | Page size |
| `sort` | string | Sort field (default: `createdAt,desc`) |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "donorName": "Ahmed Khan",
      "donorEmail": "ahmed@example.com",
      "isAnonymous": false,
      "amount": 50.00,
      "processingFee": 1.70,
      "coverFee": true,
      "totalCharged": 51.70,
      "currency": "GBP",
      "paymentMethod": "card",
      "status": "completed",
      "receiptSent": true,
      "receiptSentAt": "2026-01-29T08:05:00Z",
      "completedAt": "2026-01-29T08:00:00Z",
      "createdAt": "2026-01-29T07:59:00Z"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### POST `/api/v1/admin/campaigns/{id}/image`
Upload campaign featured image.

---

### Masjid Settings Module

#### GET `/api/v1/admin/settings`
Get masjid settings.

**Response (200):**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Al-Noor Mosque",
    "about": "A welcoming community mosque serving the local Muslim community since 1985.",
    "logoUrl": "https://cdn.example.com/logo.png",
    "logoKey": "settings/logo.png",
    "address": {
      "line1": "123 Mosque Street",
      "line2": "Unit 5",
      "city": "London",
      "postcode": "E1 6AN",
      "country": "United Kingdom"
    },
    "contact": {
      "phone": "+44 20 1234 5678",
      "email": "info@alnoor-mosque.org.uk",
      "website": "https://alnoor-mosque.org.uk"
    },
    "location": {
      "latitude": 51.5174,
      "longitude": -0.0710
    },
    "capacity": {
      "mens": 500,
      "womens": 200
    },
    "services": {
      "jumuahPrayer": true,
      "taraweehPrayer": true,
      "eidPrayer": true,
      "funeralService": true,
      "marriageService": true,
      "counseling": true,
      "hallRental": true,
      "quranClasses": true,
      "newMuslimSupport": true,
      "iftarProgram": true
    },
    "facilities": {
      "parking": true,
      "sistersArea": true,
      "wuduFacilities": true,
      "disabilityAccess": true,
      "childrensArea": false,
      "library": true,
      "shoeRacks": true
    },
    "payment": {
      "stripeAccountId": "acct_1234567890",
      "stripeAccountStatus": "active",
      "bankAccountName": "Al-Noor Mosque",
      "bankAccountNumberLast4": "1234",
      "bankSortCode": "20-00-00"
    },
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2026-01-29T08:00:00Z"
  },
  "meta": { ... }
}
```

---

#### PUT `/api/v1/admin/settings`
Update masjid settings.

**Request:**
```json
{
  "name": "Al-Noor Mosque",
  "about": "Updated description...",
  "address": {
    "line1": "123 Mosque Street",
    "city": "London",
    "postcode": "E1 6AN"
  },
  "contact": {
    "phone": "+44 20 1234 5678",
    "email": "info@alnoor-mosque.org.uk"
  },
  "capacity": {
    "mens": 500,
    "womens": 200
  },
  "services": {
    "jumuahPrayer": true,
    "taraweehPrayer": true,
    ...
  },
  "facilities": {
    "parking": true,
    ...
  }
}
```

---

#### POST `/api/v1/admin/settings/logo`
Upload masjid logo.

**Request:** `multipart/form-data`

---

#### DELETE `/api/v1/admin/settings/logo`
Remove masjid logo.

---

#### POST `/api/v1/admin/settings/stripe/connect`
Initiate Stripe Connect onboarding.

**Response (200):**
```json
{
  "data": {
    "onboardingUrl": "https://connect.stripe.com/setup/..."
  },
  "meta": { ... }
}
```

---

#### GET `/api/v1/admin/settings/stripe/status`
Get Stripe account status.

**Response (200):**
```json
{
  "data": {
    "accountId": "acct_1234567890",
    "status": "active",
    "chargesEnabled": true,
    "payoutsEnabled": true,
    "detailsSubmitted": true
  },
  "meta": { ... }
}
```

---

### Admin User Management (Super Admin Only)

#### GET `/api/v1/admin/users`
Get all admin users.

---

#### POST `/api/v1/admin/users`
Create new admin user.

**Request:**
```json
{
  "username": "newadmin",
  "email": "newadmin@masjid.com",
  "fullName": "New Admin User",
  "password": "tempPassword123",
  "role": "admin"
}
```

---

#### PUT `/api/v1/admin/users/{id}`
Update admin user.

---

#### DELETE `/api/v1/admin/users/{id}`
Deactivate admin user (soft delete).

---

#### POST `/api/v1/admin/users/{id}/reset-password`
Reset user password (generates temporary password).

---

---

## Member (Mobile) APIs

All member APIs require the `X-API-Key` header.

### Prayer Times

#### GET `/api/v1/member/prayer-times/today`
Get today's prayer times.

**Response (200):**
```json
{
  "data": {
    "date": "2026-01-29",
    "hijriDate": "29 Rajab 1447",
    "prayers": {
      "fajr": { "athan": "06:15", "jamah": "06:45" },
      "sunrise": { "athan": "07:45" },
      "zuhr": { "athan": "12:30", "jamah": "13:00" },
      "asr": { "athan": "15:00", "jamah": "15:30" },
      "maghrib": { "athan": "17:15", "jamah": "17:20" },
      "isha": { "athan": "18:45", "jamah": "19:15" }
    },
    "jumuahTimes": null,
    "nextPrayer": {
      "name": "zuhr",
      "athan": "12:30",
      "jamah": "13:00",
      "timeUntil": "2h 15m"
    }
  },
  "meta": { ... }
}
```

---

#### GET `/api/v1/member/prayer-times/week`
Get prayer times for the current week.

**Response (200):**
```json
{
  "data": [
    {
      "date": "2026-01-29",
      "dayName": "Wednesday",
      "hijriDate": "29 Rajab 1447",
      "prayers": { ... },
      "jumuahTimes": null
    },
    {
      "date": "2026-01-30",
      "dayName": "Thursday",
      "hijriDate": "30 Rajab 1447",
      "prayers": { ... },
      "jumuahTimes": null
    },
    {
      "date": "2026-01-31",
      "dayName": "Friday",
      "hijriDate": "1 Sha'ban 1447",
      "prayers": { ... },
      "jumuahTimes": [
        { "khutbah": "13:00", "jamah": "13:30" },
        { "khutbah": "14:00", "jamah": "14:30" }
      ]
    },
    ...
  ],
  "meta": { ... }
}
```

---

#### GET `/api/v1/member/prayer-times/month`
Get prayer times for the current month.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `month` | integer | Month (1-12), defaults to current |
| `year` | integer | Year, defaults to current |

---

### Events

#### GET `/api/v1/member/events`
Get published events.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `category` | string | Filter by category |
| `upcoming` | boolean | Only future events (default: true) |
| `page` | integer | Page number |
| `size` | integer | Page size |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Ramadan Preparation Workshop",
      "description": "Learn how to prepare spiritually for Ramadan",
      "category": "educational",
      "eventDate": "2026-02-15",
      "timing": {
        "startTime": "19:00",
        "endTime": "21:00",
        "doorsOpen": "18:30"
      },
      "location": {
        "venue": "Main Hall",
        "address": "123 Mosque Street, London"
      },
      "speaker": {
        "name": "Sheikh Abdullah",
        "bio": "Islamic Scholar",
        "imageUrl": "https://cdn.example.com/speakers/sheikh-abdullah.jpg"
      },
      "featuredImageUrl": "https://cdn.example.com/events/ramadan-workshop.jpg",
      "registrationLink": "https://forms.google.com/..."
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### GET `/api/v1/member/events/{id}`
Get single event details.

---

### Campaigns

#### GET `/api/v1/member/campaigns`
Get active campaigns.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `category` | string | Filter by category |
| `page` | integer | Page number |
| `size` | integer | Page size |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Masjid Roof Repair Fund",
      "description": "Help us repair the masjid roof before winter.",
      "category": "masjid_development",
      "goalAmount": 25000.00,
      "raisedAmount": 15750.50,
      "donorCount": 127,
      "progressPercentage": 63.0,
      "startDate": "2026-01-01",
      "endDate": "2026-03-31",
      "daysRemaining": 61,
      "featuredImageUrl": "https://cdn.example.com/campaigns/roof-repair.jpg"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

#### GET `/api/v1/member/campaigns/{id}`
Get single campaign details.

---

### Donations

#### POST `/api/v1/member/campaigns/{id}/donate`
Create donation and get Stripe checkout URL.

**Request:**
```json
{
  "amount": 50.00,
  "donorName": "Ahmed Khan",
  "donorEmail": "ahmed@example.com",
  "isAnonymous": false,
  "coverFee": true,
  "successUrl": "masjidapp://donation/success",
  "cancelUrl": "masjidapp://donation/cancel"
}
```

**Response (200):**
```json
{
  "data": {
    "donationId": "550e8400-e29b-41d4-a716-446655440000",
    "checkoutUrl": "https://checkout.stripe.com/pay/cs_...",
    "amount": 50.00,
    "processingFee": 1.70,
    "totalCharged": 51.70,
    "currency": "GBP"
  },
  "meta": { ... }
}
```

---

#### GET `/api/v1/member/donations/{id}/status`
Check donation status (for polling after checkout).

**Response (200):**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "completed",
    "amount": 50.00,
    "campaignTitle": "Masjid Roof Repair Fund",
    "completedAt": "2026-01-29T08:00:00Z"
  },
  "meta": { ... }
}
```

---

### Masjid Info

#### GET `/api/v1/member/masjid`
Get masjid information for mobile app.

**Response (200):**
```json
{
  "data": {
    "name": "Al-Noor Mosque",
    "about": "A welcoming community mosque...",
    "logoUrl": "https://cdn.example.com/logo.png",
    "address": {
      "line1": "123 Mosque Street",
      "line2": "Unit 5",
      "city": "London",
      "postcode": "E1 6AN",
      "country": "United Kingdom",
      "formatted": "123 Mosque Street, Unit 5, London E1 6AN"
    },
    "contact": {
      "phone": "+44 20 1234 5678",
      "email": "info@alnoor-mosque.org.uk",
      "website": "https://alnoor-mosque.org.uk"
    },
    "location": {
      "latitude": 51.5174,
      "longitude": -0.0710
    },
    "capacity": {
      "mens": 500,
      "womens": 200,
      "total": 700
    },
    "services": [
      "Jumu'ah Prayer",
      "Taraweeh Prayer",
      "Eid Prayer",
      "Funeral Service",
      "Marriage Service",
      "Counseling",
      "Hall Rental",
      "Quran Classes",
      "New Muslim Support",
      "Iftar Program"
    ],
    "facilities": [
      "Parking",
      "Sisters Area",
      "Wudu Facilities",
      "Disability Access",
      "Library",
      "Shoe Racks"
    ],
    "donationsEnabled": true
  },
  "meta": { ... }
}
```

---

### Device Registration (for Push Notifications)

#### POST `/api/v1/member/devices/register`
Register device for push notifications.

**Request:**
```json
{
  "fcmToken": "eKzF8tXqRkG...",
  "platform": "ios",
  "appVersion": "1.0.0",
  "deviceModel": "iPhone 14 Pro"
}
```

**Response (200):**
```json
{
  "data": {
    "deviceId": "550e8400-e29b-41d4-a716-446655440000",
    "registered": true
  },
  "meta": { ... }
}
```

---

#### DELETE `/api/v1/member/devices/{fcmToken}`
Unregister device from push notifications.

---

### Notification Preferences

#### GET `/api/v1/member/devices/{fcmToken}/preferences`
Get notification preferences for device.

**Response (200):**
```json
{
  "data": {
    "prayerAlerts": {
      "enabled": true,
      "minutesBefore": 15,
      "prayers": {
        "fajr": true,
        "zuhr": true,
        "asr": true,
        "maghrib": true,
        "isha": true
      }
    },
    "announcements": true,
    "events": true,
    "campaigns": false
  },
  "meta": { ... }
}
```

---

#### PUT `/api/v1/member/devices/{fcmToken}/preferences`
Update notification preferences.

**Request:**
```json
{
  "prayerAlerts": {
    "enabled": true,
    "minutesBefore": 10,
    "prayers": {
      "fajr": true,
      "zuhr": false,
      "asr": false,
      "maghrib": true,
      "isha": true
    }
  },
  "announcements": true,
  "events": true,
  "campaigns": true
}
```

---

---

## Webhook APIs

### Stripe Webhooks

#### POST `/api/v1/webhooks/stripe`
Handle Stripe webhook events.

**Headers:**
```
Stripe-Signature: t=1234567890,v1=abc123...
```

**Handled Events:**
| Event | Action |
|-------|--------|
| `checkout.session.completed` | Mark donation as completed, update campaign totals |
| `payment_intent.payment_failed` | Mark donation as failed |
| `charge.refunded` | Mark donation as refunded, update campaign totals |
| `account.updated` | Update Stripe account status |

**Response (200):**
```json
{
  "received": true
}
```

---

## Rate Limiting

| Endpoint Type | Rate Limit |
|---------------|------------|
| Admin APIs | 100 requests/minute per user |
| Member APIs | 60 requests/minute per API key |
| Auth endpoints | 10 requests/minute per IP |
| Webhook endpoints | No limit (signature verified) |

**Rate Limit Headers:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1706518800
```

**Rate Limit Exceeded Response (429):**
```json
{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Too many requests. Please try again later.",
    "retryAfter": 45
  },
  "meta": { ... }
}
```

---

## Appendix

### Category Values

**Event Categories:**
- `community`
- `educational`
- `fundraiser`
- `religious`
- `youth`
- `other`

**Campaign Categories:**
- `masjid_development`
- `education`
- `charity`
- `individual_support`
- `general`

### Status Values

**Event Status:**
- `draft` - Not visible to members
- `published` - Visible to members
- `cancelled` - Cancelled event
- `completed` - Past event

**Announcement Status:**
- `draft` - Not sent
- `scheduled` - Scheduled for future
- `sent` - Successfully sent
- `failed` - Failed to send

**Campaign Status:**
- `draft` - Not visible to members
- `active` - Accepting donations
- `paused` - Temporarily paused
- `completed` - Goal reached or ended
- `cancelled` - Cancelled

**Donation Status:**
- `pending` - Payment initiated
- `completed` - Payment successful
- `failed` - Payment failed
- `refunded` - Payment refunded
