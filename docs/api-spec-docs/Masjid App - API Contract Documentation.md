# 

# 

#  Masjid App

*API Contract Documentation*

**Isquaretechsolutions.co.uk**

**Document Information**

| Document Title | API Contract Documentation |
| :---- | :---- |
| **Version** | 1.0 |
| **Date** | January 29, 2026 |
| **Status** | Final Draft |
| **Prepared By** | Syed Fardeen (Product Team) |

**Document Revision History**

| Version | Date | Author | Description of Changes |
| ----- | ----- | ----- | ----- |
| 1.0 | January 29, 2026 | Syed Fardeen | Initial API contract documentation created |

# 

# 

# Index

[**API OVERVIEW**](#api-overview)

[Base URL & Structure](#base-url--structure)

[Authentication](#authentication)

[Response Format](#response-format)

[Error Codes](#error-codes)

[**ADMIN APIs**](#admin-apis)

[Auth Module](#auth-module)

[Prayer Times Module](#prayer-times-module)

[Events Module](#events-module)

[Announcements Module](#announcements-module)

[Campaigns Module](#campaigns-module)

[Masjid Settings Module](#masjid-settings-module)

[Admin User Management](#admin-user-management)

[**MEMBER (MOBILE) APIs**](#member-mobile-apis)

[Prayer Times](#member-prayer-times)

[Events](#member-events)

[Campaigns & Donations](#member-campaigns--donations)

[Masjid Information](#member-masjid-information)

[Device Registration & Notifications](#device-registration--notifications)

[**WEBHOOK APIs**](#webhook-apis)

[Stripe Webhooks](#stripe-webhooks)

[**APPENDIX**](#appendix)

[Category Values](#category-values)

[Status Values](#status-values)

---

# **API OVERVIEW** {#api-overview}

## **Base URL & Structure** {#base-url--structure}

**Base URLs:**
```
Production: https://api.{masjid-domain}.com/api/v1
Development: http://localhost:8080/api/v1
```

**API Structure:**

| Path Prefix | Consumer | Authentication | Description |
| :---- | :---- | :---- | :---- |
| `/api/v1/admin/*` | Admin Web App | JWT Bearer Token | Full CRUD operations |
| `/api/v1/member/*` | Mobile App | API Key | Read-only + Donations |
| `/api/v1/webhooks/*` | External Services | Signature Verification | Stripe events |

**Content Type:** All requests and responses use `application/json`

---

## **Authentication** {#authentication}

### **Admin Authentication (JWT)**

Admin endpoints require a JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Lifecycle:**

| Token Type | Duration | Storage |
| :---- | :---- | :---- |
| Access Token | 15 minutes | Memory (Frontend) |
| Refresh Token | 7 days | HttpOnly Cookie |

### **Mobile Authentication (API Key)**

Member endpoints require an API key in the header:

```
X-API-Key: ma_live_xxxxxxxxxxxxxxxxxxxx
```

**Why API Key for Mobile:**
- No user login required for mobile app
- Identifies legitimate app requests
- Simple to implement and rotate
- Rate limiting prevents abuse
- Stripe handles payment security for donations

---

## **Response Format** {#response-format}

### **Success Response (Single Object):**

```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2026-01-29T08:00:00Z",
    "requestId": "req_abc123"
  }
}
```

### **Success Response (List with Pagination):**

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

### **Error Response:**

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

---

## **Error Codes** {#error-codes}

| HTTP Status | Code | Description |
| :---- | :---- | :---- |
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

### **Rate Limiting:**

| Endpoint Type | Rate Limit |
| :---- | :---- |
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

---

# **ADMIN APIs** {#admin-apis}

All admin endpoints require JWT Bearer Token authentication.

---

## **Auth Module** {#auth-module}

**Purpose:**  
Provide secure JWT-based authentication for admin portal access with rate limiting and account lockout protection.

**Why This Matters:**
- All admin APIs require authentication
- Frontend team needs login functionality first
- Security implemented from the beginning
- Rate limiting prevents brute force attacks

---

### **POST** `/api/v1/admin/auth/login`

Authenticate admin user and receive JWT tokens.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `username` | string | Yes | Admin username (3-50 chars) |
| `password` | string | Yes | Admin password (min 8 chars) |

**Request Example:**
```json
{
  "username": "admin",
  "password": "securePassword123"
}
```

**Response (200 OK):**
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

### **POST** `/api/v1/admin/auth/refresh`

Refresh access token using refresh token cookie.

**Request:** No body (uses HttpOnly cookie)

**Response (200 OK):**
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

### **POST** `/api/v1/admin/auth/logout`

Invalidate refresh token and clear cookies.

**Response (200 OK):**
```json
{
  "data": {
    "message": "Logged out successfully"
  },
  "meta": { ... }
}
```

---

### **GET** `/api/v1/admin/auth/me`

Get current authenticated user details.

**Response (200 OK):**
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

### **PUT** `/api/v1/admin/auth/change-password`

Change current user's password.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `currentPassword` | string | Yes | Current password |
| `newPassword` | string | Yes | New password (min 8 chars) |

**Response (200 OK):**
```json
{
  "data": {
    "message": "Password changed successfully"
  },
  "meta": { ... }
}
```

---

## **Prayer Times Module** {#prayer-times-module}

**Purpose:**  
Build complete prayer time management allowing admins to upload annual schedules via CSV or edit manually.

**Why This Matters:**
- Prayer times are the primary reason users open the app daily
- CSV upload eliminates 90% of manual data entry work
- Supports multiple Friday Jumuah times for larger masjids
- Hijri calendar dates maintain cultural relevance

---

### **GET** `/api/v1/admin/prayer-times`

Get prayer times with pagination and date filters.

**Query Parameters:**

| Parameter | Type | Default | Description |
| :---- | :---- | :---- | :---- |
| `startDate` | date | - | Filter from date (YYYY-MM-DD) |
| `endDate` | date | - | Filter to date (YYYY-MM-DD) |
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Items per page (max 100) |
| `sort` | string | `date,asc` | Sort field and direction |

**Response (200 OK):**
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

### **GET** `/api/v1/admin/prayer-times/{id}`

Get single prayer time entry by ID.

**Path Parameters:**

| Parameter | Type | Description |
| :---- | :---- | :---- |
| `id` | UUID | Prayer time record ID |

**Response (200 OK):** Returns single prayer time object.

---

### **POST** `/api/v1/admin/prayer-times`

Create prayer time entry for a specific date.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `date` | date | Yes | Date (YYYY-MM-DD) |
| `hijriDate` | string | No | Hijri calendar date |
| `prayers` | object | Yes | Prayer times object |
| `jumuahTimes` | array | No | Friday Jumuah times |

**Request Example:**
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

**Response (201 Created):** Returns created prayer time object.

---

### **POST** `/api/v1/admin/prayer-times/bulk`

Create or update prayer times in bulk (for importing monthly timetables).

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `prayerTimes` | array | Yes | Array of prayer time objects |

**Response (200 OK):**
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

### **PUT** `/api/v1/admin/prayer-times/{id}`

Update prayer time entry.

**Response (200 OK):** Returns updated prayer time object.

---

### **DELETE** `/api/v1/admin/prayer-times/{id}`

Delete prayer time entry.

**Response (204 No Content)**

---

## **Events Module** {#events-module}

**Purpose:**  
Enable masjid administrators to create and manage community events with images, details, and draft/publish workflow.

**Why This Matters:**
- Events drive community engagement and attendance
- Image upload makes events more appealing
- Draft workflow allows review before publishing
- Calendar view helps avoid scheduling conflicts

---

### **GET** `/api/v1/admin/events`

Get all events with filters and pagination.

**Query Parameters:**

| Parameter | Type | Default | Description |
| :---- | :---- | :---- | :---- |
| `status` | string | - | Filter by status |
| `category` | string | - | Filter by category |
| `startDate` | date | - | Filter events from date |
| `endDate` | date | - | Filter events to date |
| `search` | string | - | Search in title/description |
| `page` | integer | 0 | Page number |
| `size` | integer | 20 | Page size |
| `sort` | string | `eventDate,asc` | Sort field |

**Response (200 OK):**
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
        "imageUrl": "https://cdn.example.com/speakers/sheikh.jpg"
      },
      "media": {
        "featuredImageUrl": "https://cdn.example.com/events/workshop.jpg",
        "featuredImageKey": "events/workshop.jpg"
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

### **GET** `/api/v1/admin/events/{id}`

Get single event by ID.

---

### **POST** `/api/v1/admin/events`

Create new event.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `title` | string | Yes | Event title (max 255 chars) |
| `description` | string | Yes | Event description |
| `category` | string | Yes | Event category |
| `eventDate` | date | Yes | Event date (YYYY-MM-DD) |
| `timing` | object | Yes | Timing details |
| `location` | object | Yes | Location details |
| `speaker` | object | No | Speaker information |
| `registrationLink` | string | No | External registration URL |

**Response (201 Created):** Returns created event.

---

### **PUT** `/api/v1/admin/events/{id}`

Update event details.

**Response (200 OK):** Returns updated event.

---

### **DELETE** `/api/v1/admin/events/{id}`

Delete event (only draft events can be deleted).

**Response (204 No Content)**

**Error:** `400 BAD_REQUEST` - Cannot delete non-draft event

---

### **PATCH** `/api/v1/admin/events/{id}/status`

Update event status (publish, cancel, complete).

**Request Body:**
```json
{
  "status": "published"
}
```

**Response (200 OK):**
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

### **POST** `/api/v1/admin/events/{id}/notify`

Send push notification for event to all registered devices.

**Response (200 OK):**
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

### **POST** `/api/v1/admin/events/{id}/image`

Upload event featured image.

**Request:** `multipart/form-data`

| Field | Type | Description |
| :---- | :---- | :---- |
| `image` | file | Image file (JPEG, PNG, WebP, max 5MB) |

**Response (200 OK):**
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

## **Announcements Module** {#announcements-module}

**Purpose:**  
Build announcement system with Firebase push notifications for immediate and scheduled community communications.

**Why This Matters:**
- Instant communication with entire community
- Scheduled announcements for planned messages
- Push notifications ensure high visibility
- Critical for emergency closures and urgent updates

---

### **GET** `/api/v1/admin/announcements`

Get all announcements with filters and pagination.

**Query Parameters:**

| Parameter | Type | Default | Description |
| :---- | :---- | :---- | :---- |
| `status` | string | - | Filter by status |
| `page` | integer | 0 | Page number |
| `size` | integer | 20 | Page size |
| `sort` | string | `createdAt,desc` | Sort field |

**Response (200 OK):**
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

### **POST** `/api/v1/admin/announcements`

Create and optionally send/schedule announcement.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `title` | string | Yes | Title (max 100 chars) |
| `message` | string | Yes | Message content |
| `sendImmediately` | boolean | No | Send now (default: false) |
| `isScheduled` | boolean | No | Schedule for later |
| `scheduledFor` | datetime | No | When to send |

**Immediate Send Example:**
```json
{
  "title": "Important Notice",
  "message": "Please remember to bring your prayer mats.",
  "sendImmediately": true
}
```

**Scheduled Example:**
```json
{
  "title": "Jumuah Reminder",
  "message": "Jumuah prayer starts at 1:30 PM today.",
  "isScheduled": true,
  "scheduledFor": "2026-01-31T12:00:00Z"
}
```

**Response (201 Created):** Returns created announcement.

---

### **POST** `/api/v1/admin/announcements/{id}/send`

Send a draft announcement immediately.

**Response (200 OK):**
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

### **POST** `/api/v1/admin/announcements/{id}/cancel`

Cancel a scheduled announcement.

**Response (200 OK):** Returns updated announcement with status changed.

---

## **Campaigns Module** {#campaigns-module}

**Purpose:**  
Build transparent fundraising system with Stripe payment integration enabling online donations.

**Why This Matters:**
- Online donations increase contribution convenience
- Real-time progress tracking builds donor confidence
- Stripe handles payment security and compliance
- Anonymous donation option respects privacy
- Processing fee coverage maximizes masjid funds

---

### **GET** `/api/v1/admin/campaigns`

Get all campaigns with filters and pagination.

**Response (200 OK):**
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
        "featuredImageUrl": "https://cdn.example.com/campaigns/roof.jpg",
        "featuredImageKey": "campaigns/roof.jpg"
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

### **GET** `/api/v1/admin/campaigns/{id}`

Get single campaign with detailed stats.

**Response (200 OK):**
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

### **POST** `/api/v1/admin/campaigns`

Create new fundraising campaign.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `title` | string | Yes | Campaign title |
| `description` | string | Yes | Campaign description |
| `category` | string | Yes | Campaign category |
| `goalAmount` | decimal | Yes | Target amount |
| `startDate` | date | Yes | Campaign start date |
| `endDate` | date | No | Campaign end date |

**Response (201 Created):** Returns created campaign.

---

### **GET** `/api/v1/admin/campaigns/{id}/donations`

Get donations for a specific campaign.

**Response (200 OK):**
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

## **Masjid Settings Module** {#masjid-settings-module}

**Purpose:**  
Enable masjid profile configuration including contact information, bank details, and branding.

**Why This Matters:**
- Masjid information displays in mobile app
- Bank details required for Stripe payouts
- Logo provides brand identity
- Services and facilities inform community

---

### **GET** `/api/v1/admin/settings`

Get all masjid settings including payment configuration.

**Response (200 OK):**
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

### **PUT** `/api/v1/admin/settings`

Update masjid profile, services, and facilities.

**Response (200 OK):** Returns updated settings.

---

### **POST** `/api/v1/admin/settings/stripe/connect`

Start Stripe Connect onboarding process.

**Response (200 OK):**
```json
{
  "data": {
    "onboardingUrl": "https://connect.stripe.com/setup/..."
  },
  "meta": { ... }
}
```

---

### **GET** `/api/v1/admin/settings/stripe/status`

Get current Stripe Connect account status.

**Response (200 OK):**
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

## **Admin User Management** {#admin-user-management}

**Note:** These endpoints are only accessible by Super Admin role.

---

### **GET** `/api/v1/admin/users`

Get all admin users.

---

### **POST** `/api/v1/admin/users`

Create new admin user.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `username` | string | Yes | Username (3-50 chars) |
| `email` | string | Yes | Email address |
| `fullName` | string | Yes | Full name |
| `password` | string | Yes | Temporary password |
| `role` | string | No | `admin` or `super_admin` |

**Response (201 Created):** Returns created user.

---

### **POST** `/api/v1/admin/users/{id}/reset-password`

Reset user password (generates temporary password).

**Response (200 OK):**
```json
{
  "data": {
    "message": "Password reset successfully",
    "temporaryPassword": "TempPass123!"
  },
  "meta": { ... }
}
```

---

# **MEMBER (MOBILE) APIs** {#member-mobile-apis}

All member APIs require the `X-API-Key` header.

---

## **Prayer Times** {#member-prayer-times}

---

### **GET** `/api/v1/member/prayer-times/today`

Get today's prayer times with next prayer info.

**Response (200 OK):**
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

### **GET** `/api/v1/member/prayer-times/week`

Get prayer times for the current week.

**Response (200 OK):**
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
      "date": "2026-01-31",
      "dayName": "Friday",
      "hijriDate": "1 Sha'ban 1447",
      "prayers": { ... },
      "jumuahTimes": [
        { "khutbah": "13:00", "jamah": "13:30" },
        { "khutbah": "14:00", "jamah": "14:30" }
      ]
    }
  ],
  "meta": { ... }
}
```

---

### **GET** `/api/v1/member/prayer-times/month`

Get prayer times for specified month.

**Query Parameters:**

| Parameter | Type | Default | Description |
| :---- | :---- | :---- | :---- |
| `month` | integer | current | Month (1-12) |
| `year` | integer | current | Year |

---

## **Events** {#member-events}

---

### **GET** `/api/v1/member/events`

Get published events for mobile app.

**Query Parameters:**

| Parameter | Type | Default | Description |
| :---- | :---- | :---- | :---- |
| `category` | string | - | Filter by category |
| `upcoming` | boolean | true | Only future events |
| `page` | integer | 0 | Page number |
| `size` | integer | 20 | Page size |

**Response (200 OK):**
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
        "imageUrl": "https://cdn.example.com/speakers/sheikh.jpg"
      },
      "featuredImageUrl": "https://cdn.example.com/events/workshop.jpg",
      "registrationLink": "https://forms.google.com/..."
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

### **GET** `/api/v1/member/events/{id}`

Get single event details.

---

## **Campaigns & Donations** {#member-campaigns--donations}

---

### **GET** `/api/v1/member/campaigns`

Get active campaigns for mobile app.

**Response (200 OK):**
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
      "featuredImageUrl": "https://cdn.example.com/campaigns/roof.jpg"
    }
  ],
  "pagination": { ... },
  "meta": { ... }
}
```

---

### **POST** `/api/v1/member/campaigns/{id}/donate`

Create donation and get Stripe checkout URL.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `amount` | decimal | Yes | Donation amount (min £1) |
| `donorName` | string | No | Donor's name |
| `donorEmail` | string | No | Donor's email for receipt |
| `isAnonymous` | boolean | No | Hide donor name |
| `coverFee` | boolean | No | Cover processing fee |
| `successUrl` | string | Yes | Redirect URL on success |
| `cancelUrl` | string | Yes | Redirect URL on cancel |

**Request Example:**
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

**Response (200 OK):**
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

### **GET** `/api/v1/member/donations/{id}/status`

Check donation status (for polling after checkout).

**Response (200 OK):**
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

## **Masjid Information** {#member-masjid-information}

---

### **GET** `/api/v1/member/masjid`

Get masjid information for mobile app.

**Response (200 OK):**
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

## **Device Registration & Notifications** {#device-registration--notifications}

---

### **POST** `/api/v1/member/devices/register`

Register device for push notifications.

**Request Body:**

| Field | Type | Required | Description |
| :---- | :---- | :---- | :---- |
| `fcmToken` | string | Yes | Firebase Cloud Messaging token |
| `platform` | string | Yes | `ios` or `android` |
| `appVersion` | string | No | App version number |
| `deviceModel` | string | No | Device model name |

**Response (200 OK):**
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

### **DELETE** `/api/v1/member/devices/{fcmToken}`

Unregister device from push notifications.

**Response (204 No Content)**

---

### **GET** `/api/v1/member/devices/{fcmToken}/preferences`

Get notification preferences for device.

**Response (200 OK):**
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

### **PUT** `/api/v1/member/devices/{fcmToken}/preferences`

Update notification preferences.

**Request Body:**
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

**Response (200 OK):** Returns updated preferences.

---

# **WEBHOOK APIs** {#webhook-apis}

## **Stripe Webhooks** {#stripe-webhooks}

---

### **POST** `/api/v1/webhooks/stripe`

Handle Stripe webhook events.

**Headers:**
```
Stripe-Signature: t=1234567890,v1=abc123...
```

**Handled Events:**

| Event | Action |
| :---- | :---- |
| `checkout.session.completed` | Mark donation as completed, update campaign totals |
| `payment_intent.payment_failed` | Mark donation as failed |
| `charge.refunded` | Mark donation as refunded, update campaign totals |
| `account.updated` | Update Stripe account status |

**Response (200 OK):**
```json
{
  "received": true
}
```

---

# **APPENDIX** {#appendix}

## **Category Values** {#category-values}

### **Event Categories:**

| Value | Display Name |
| :---- | :---- |
| `community` | Community |
| `educational` | Educational |
| `fundraiser` | Fundraiser |
| `religious` | Religious |
| `youth` | Youth |
| `other` | Other |

### **Campaign Categories:**

| Value | Display Name |
| :---- | :---- |
| `masjid_development` | Masjid Development |
| `education` | Education |
| `charity` | Charity |
| `individual_support` | Individual Support |
| `general` | General |

---

## **Status Values** {#status-values}

### **Event Status:**

| Value | Description |
| :---- | :---- |
| `draft` | Not visible to members |
| `published` | Visible to members |
| `cancelled` | Cancelled event |
| `completed` | Past event |

### **Announcement Status:**

| Value | Description |
| :---- | :---- |
| `draft` | Not sent |
| `scheduled` | Scheduled for future |
| `sent` | Successfully sent |
| `failed` | Failed to send |

### **Campaign Status:**

| Value | Description |
| :---- | :---- |
| `draft` | Not visible to members |
| `active` | Accepting donations |
| `paused` | Temporarily paused |
| `completed` | Goal reached or ended |
| `cancelled` | Cancelled |

### **Donation Status:**

| Value | Description |
| :---- | :---- |
| `pending` | Payment initiated |
| `completed` | Payment successful |
| `failed` | Payment failed |
| `refunded` | Payment refunded |

---

# **API SUMMARY TABLE**

| Module | Admin Endpoints | Member Endpoints | Total |
| :---- | :---- | :---- | :---- |
| **Auth** | 5 | - | 5 |
| **Prayer Times** | 5 | 3 | 8 |
| **Events** | 7 | 2 | 9 |
| **Announcements** | 6 | - | 6 |
| **Campaigns** | 7 | 4 | 11 |
| **Settings** | 6 | 1 | 7 |
| **Users** | 5 | - | 5 |
| **Devices** | - | 4 | 4 |
| **Webhooks** | - | 1 | 1 |
| **Total** | **41** | **15** | **56** |

---

**End of Document**
