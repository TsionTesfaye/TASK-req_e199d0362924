# RescueHub API Reference

All endpoints are prefixed with `/api`. The server listens on port `8080` behind the nginx reverse proxy (HTTPS on port `15443`).

---

## Authentication

Every request (except the four public endpoints below) must include a session token.

| Header | Value |
|---|---|
| `X-Session-Token` | Session token returned by `/auth/login` or `/bootstrap` |
| `X-CSRF-Token` | CSRF token returned by `/auth/login` or `/bootstrap` — **required on all mutating requests** (POST, PUT, PATCH, DELETE) |
| `Idempotency-Key` | Caller-supplied UUID — required or optional depending on endpoint |

**Public endpoints** (no auth or CSRF required):
- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/bootstrap`
- `GET /api/bootstrap/status`

**Roles** (from least to most privileged): `FRONT_DESK`, `CLINICIAN`, `BILLING`, `QUALITY`, `MODERATOR`, `ADMIN`

---

## Response envelope

All responses use a consistent JSON envelope.

**Single item:**
```json
{ "data": { ... } }
```

**List:**
```json
{
  "data": [ ... ],
  "meta": { "page": 0, "size": 20, "total": 142 }
}
```

**Error:**
```json
{ "code": 422, "message": "...", "details": null }
```

**Pagination limits:** `page >= 0`, `1 <= size <= 200`.

---

## Health

### `GET /api/health`
Public. Returns server liveness.

**Response**
```json
{ "data": { "status": "ok" } }
```

---

## Bootstrap

One-time system initialization. Fails with `422` if the system is already initialized.

### `GET /api/bootstrap/status`
Public.

**Response**
```json
{ "data": { "initialized": false } }
```

### `POST /api/bootstrap`
Public. Creates the first organization and admin user. Returns session and CSRF tokens.

**Body**
| Field | Type | Required |
|---|---|---|
| `username` | string | yes |
| `password` | string | yes |
| `confirmPassword` | string | yes |
| `displayName` | string | no |
| `organizationName` | string | no |

**Response**
```json
{
  "data": {
    "userId": 1,
    "organizationId": 1,
    "sessionToken": "...",
    "csrfToken": "...",
    "user": {
      "id": 1,
      "username": "admin",
      "displayName": "System Admin",
      "role": "ADMIN",
      "organizationId": 1
    }
  }
}
```

---

## Auth

### `POST /api/auth/login`
Public. Authenticates a user and returns session + CSRF tokens.

**Body**
| Field | Type | Required |
|---|---|---|
| `username` | string | yes |
| `password` | string | yes |

**Response**
```json
{
  "data": {
    "sessionToken": "...",
    "csrfToken": "...",
    "user": {
      "id": 5,
      "username": "clinician",
      "displayName": "Dr. Smith",
      "role": "CLINICIAN",
      "organizationId": 1
    }
  }
}
```

### `POST /api/auth/logout`
Requires auth + CSRF. Invalidates the session token.

**Response**
```json
{ "data": { "status": "logged out" } }
```

### `GET /api/auth/me`
Requires auth. Returns the currently authenticated user.

**Response** — same `user` shape as login.

---

## Admin — Users

All `/api/admin/*` endpoints require **ADMIN** role.

### `POST /api/admin/users`
Create a new user. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `username` | string | yes |
| `password` | string | yes |
| `displayName` | string | yes |
| `role` | `Role` | yes |

**`Role` values:** `FRONT_DESK`, `CLINICIAN`, `BILLING`, `QUALITY`, `MODERATOR`, `ADMIN`

**Response** — user object.

### `GET /api/admin/users`
List users with pagination.

**Query params:** `page` (default 0), `size` (default 20)

**Response** — paginated list of user objects.

### `GET /api/admin/users/{id}`
Get a single user by ID.

### `PUT /api/admin/users/{id}`
Update user details. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `displayName` | string | yes |
| `role` | `Role` | yes |
| `isActive` | boolean | yes |
| `isFrozen` | boolean | yes |

### `DELETE /api/admin/users/{id}`
Soft-delete a user. CSRF required. Returns `{ "data": null }`.

---

## Admin — Access Control

Manages the organization's allowlist and denylist. Requires **ADMIN** role.

### `GET /api/admin/access-control`
List all access list entries.

### `POST /api/admin/access-control`
Add an entry. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `listType` | string | yes — `"ALLOW"` or `"DENY"` |
| `subjectType` | string | yes — e.g. `"IP"`, `"USER"` |
| `subjectValue` | string | yes |
| `reason` | string | no |
| `expiresAt` | ISO-8601 instant | no |

### `DELETE /api/admin/access-control/{id}`
Remove an entry. CSRF required.

---

## Audit

### `GET /api/audit`
Requires **ADMIN** role. Returns a paginated audit log.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `page` | int | default 0 |
| `size` | int | default 20 |
| `q` | string | optional keyword search |

---

## Patients

### `POST /api/patients`
Register a new patient. CSRF required. Requires **FRONT_DESK** or **ADMIN**.

**Body**
| Field | Type | Required |
|---|---|---|
| `firstName` | string | yes (encrypted at rest) |
| `lastName` | string | yes (encrypted at rest) |
| `dateOfBirth` | `YYYY-MM-DD` | yes |
| `sex` | string | no |
| `phone` | string | no (encrypted at rest) |
| `address` | string | no (encrypted at rest) |
| `emergencyContactName` | string | no |
| `emergencyContactPhone` | string | no |
| `isMinor` | boolean | yes |
| `isProtectedCase` | boolean | yes |

**Response** — `PatientResponse` DTO (no ciphertext, no plaintext PII):
```json
{
  "data": {
    "id": 42,
    "medicalRecordNumber": "MRN-00042",
    "dateOfBirth": "1990-05-10",
    "sex": "F",
    "phoneLast4": "4567",
    "emergencyContactName": null,
    "isMinor": false,
    "isProtectedCase": false,
    "createdAt": "2026-04-16T10:00:00Z",
    "archivedAt": null
  }
}
```

### `GET /api/patients`
List patients. Requires auth.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `page` | int | default 0 |
| `size` | int | default 20 |
| `q` | string | optional search (MRN, last-4, etc.) |
| `archived` | string | optional — `"true"` to include archived |

**Response** — paginated list of `PatientResponse` DTOs.

### `GET /api/patients/{id}`
Get a single patient. Returns `PatientResponse` DTO (no PII).

### `DELETE /api/patients/{id}/archive`
Archive a patient. CSRF required. Returns `PatientResponse`.

### `GET /api/patients/{id}/reveal`
Decrypt and return full PII. Requires **CLINICIAN** or **ADMIN**. Writes an audit log entry (`PATIENT_PII_REVEAL`).

**Response**
```json
{
  "data": {
    "firstName": "Alice",
    "lastName": "Smith",
    "phone": "5551234567",
    "address": "100 Main St",
    "dateOfBirth": "1990-05-10"
  }
}
```

### `POST /api/patients/{id}/verify-identity`
Record an identity verification event. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `documentType` | string | yes — e.g. `"DRIVER_LICENSE"` |
| `documentLast4` | string | yes |
| `note` | string | no |

### `GET /api/patients/{id}/verifications`
List identity verifications for a patient.

---

## Visits

### `POST /api/visits`
Open a new visit. CSRF required. Requires **CLINICIAN** or **ADMIN**.

**Headers:** `Idempotency-Key` (optional — if provided, replays return the same visit)

**Body**
| Field | Type | Required |
|---|---|---|
| `patientId` | long | yes |
| `appointmentId` | long | no |
| `chiefComplaint` | string | no |

**Response** — Visit object with fields: `id`, `patientId`, `appointmentId`, `status`, `chiefComplaint`, `summaryText`, `diagnosisText`, `qcBlocked`, `createdAt`, `closedAt`.

**`VisitStatus` values:** `OPEN`, `READY_FOR_CLOSE`, `CLOSED`, `ARCHIVED`

### `GET /api/visits`
List visits. Requires **CLINICIAN** or **ADMIN**.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `page` | int | default 0 |
| `size` | int | default 20 |
| `patientId` | long | optional — filter to a single patient |

### `GET /api/visits/{id}`
Get a single visit.

### `PUT /api/visits/{id}`
Update visit summary and diagnosis. CSRF required. Visit must be `OPEN`.

**Body**
| Field | Type | Required |
|---|---|---|
| `summaryText` | string | no |
| `diagnosisText` | string | no |

### `POST /api/visits/{id}/close`
Close a visit. CSRF required. Generates an invoice. Visit must be `OPEN` or `READY_FOR_CLOSE`.

**Headers:** `Idempotency-Key` (required)

**Response** — Visit object with `status: "CLOSED"`.

---

## Appointments

### `POST /api/appointments`
Schedule an appointment. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `patientId` | long | yes |
| `scheduledDate` | `YYYY-MM-DD` | yes |
| `scheduledTime` | `HH:mm:ss` | no |
| `clinicianUserId` | long | no |

### `GET /api/appointments`
List appointments.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `date` | string | optional — `MM/DD/YYYY` or `YYYY-MM-DD` |
| `page` | int | default 0 |
| `size` | int | default 20 |

### `GET /api/appointments/{id}`
Get a single appointment.

### `PUT /api/appointments/{id}/status`
Update appointment status. CSRF required.

**Query param:** `status` — one of `SCHEDULED`, `CHECKED_IN`, `COMPLETED`, `CANCELED`, `NO_SHOW`, `ARCHIVED`

---

## Invoices

All invoice endpoints require **BILLING** or **ADMIN** role.

### `GET /api/invoices`
List invoices.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/invoices/{id}`
Get a single invoice. Fields include: `id`, `visitId`, `status`, `totalAmount`, `paidAmount`, `dailyCloseDate`, `createdAt`, `voidedAt`.

**`InvoiceStatus` values:** `OPEN`, `PARTIALLY_PAID`, `PAID`, `VOIDED`, `PARTIALLY_REFUNDED`, `REFUNDED`

### `GET /api/invoices/{id}/payments`
List payments on an invoice.

### `POST /api/invoices/{id}/payments`
Record a payment. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `tenderType` | `TenderType` | yes |
| `amount` | decimal | yes |
| `externalReference` | string | no |

**`TenderType` values:** `CASH`, `CHECK`, `EXTERNAL_CARD_ON_FILE`

### `POST /api/invoices/{id}/void`
Void an invoice. CSRF required. Cannot void after 23:00 on the daily close date.

**Headers:** `Idempotency-Key` (required)

### `POST /api/invoices/{id}/refunds`
Refund an invoice. CSRF required. Amount must not exceed the remaining balance and must have at most 2 decimal places.

**Headers:** `Idempotency-Key` (required)

**Body**
| Field | Type | Required |
|---|---|---|
| `amount` | decimal | yes |
| `reason` | string | yes |

---

## Daily Close

Requires **BILLING** or **ADMIN** role.

### `POST /api/daily-close`
Close the books for a business date. CSRF required. Returns `422` if already closed.

**Body**
| Field | Type | Required |
|---|---|---|
| `businessDate` | `YYYY-MM-DD` | yes |

### `GET /api/daily-close`
List daily close records.

**Query params:** `page` (default 0), `size` (default 20)

---

## Exports

Requires **BILLING** or **ADMIN** role.

### `POST /api/exports`
Trigger a data export. CSRF required. Idempotent — replaying the same `idempotencyKey` returns the cached result.

**Body**
| Field | Type | Required |
|---|---|---|
| `exportType` | string | yes — `"ledger"`, `"audit"`, or `"patients"` |
| `idempotencyKey` | string | yes |
| `elevated` | boolean | yes |
| `secondConfirmation` | boolean | yes |

Any other `exportType` value returns `422`.

**Response**
```json
{ "data": { "result": "exports/ledger/2026-04-16_ledger.csv" } }
```

### `GET /api/exports/history`
List past export runs.

**Query params:** `page` (default 0), `size` (default 20)

**Response** — list of export history entries, each with: `id`, `type`, `status`, `filePath`, `createdAt`.

---

## Backups

Requires **ADMIN** role.

### `GET /api/backups`
List backup runs.

**Query params:** `page` (default 0), `size` (default 20)

### `POST /api/backups/run`
Trigger a backup immediately. CSRF required. Returns the backup run object.

**Backup run fields:** `id`, `status` (`RUNNING`/`COMPLETED`/`FAILED`), `outputPath`, `rowCount`, `createdAt`.

### `POST /api/backups/{id}/restore-test`
Record the result of a restore test against a backup. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `result` | `RestoreTestResult` | yes |
| `note` | string | no |

**`RestoreTestResult` values:** `PASSED`, `FAILED`

---

## Incidents

### `POST /api/incidents`
Submit an incident report. CSRF required. Idempotent via `idempotencyKey` in the body.

**Body**
| Field | Type | Required |
|---|---|---|
| `idempotencyKey` | string | yes |
| `category` | string | yes — e.g. `"welfare"`, `"medical"` |
| `description` | string | yes |
| `approximateLocationText` | string | yes |
| `neighborhood` | string | no |
| `nearestCrossStreets` | string | no |
| `exactLocation` | string | no |
| `isAnonymous` | boolean | yes |
| `involvesMinor` | boolean | yes |
| `isProtectedCase` | boolean | yes |
| `subjectAgeGroup` | string | no — e.g. `"adult"`, `"minor"` |

**Response** — Incident report object. `exactLocation` is redacted unless revealed via `/reveal-location`.

**`IncidentStatus` values:** `SUBMITTED`, `REVIEWED`, `PUBLISHED`, `HIDDEN`, `ARCHIVED`

### `GET /api/incidents`
List incidents for the organization.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/incidents/{id}`
Get a single incident report.

### `POST /api/incidents/{id}/moderate`
Update incident status. CSRF required. Requires **MODERATOR** or **ADMIN**.

**Body**
| Field | Type | Required |
|---|---|---|
| `status` | `IncidentStatus` | yes |

### `POST /api/incidents/{id}/reclassify`
Reclassify an incident's sensitivity flags. CSRF required. Requires **ADMIN**.

**Body**
| Field | Type | Required |
|---|---|---|
| `isProtectedCase` | boolean | yes |
| `involvesMinor` | boolean | yes |
| `exactLocation` | string | no |
| `reason` | string | no |

### `GET /api/incidents/{id}/reveal-location`
Decrypt and return the exact location. Writes an audit log entry.

**Response**
```json
{ "data": { "exactLocation": "123 Oak St, Unit 4B" } }
```

### `GET /api/incidents/{id}/media`
List media files attached to an incident.

### `POST /api/incidents/{id}/media`
Upload a media file. CSRF required. Multipart form data.

**Form field:** `file` — the file to upload.

---

## Route Sheets

### `POST /api/routesheets`
Generate a route sheet for an incident. CSRF required. Requires **ADMIN** or **FRONT_DESK**.

Pass `resourceId: 0` to trigger auto-selection of the nearest shelter using haversine distance.

**Body**
| Field | Type | Required |
|---|---|---|
| `incidentId` | long | yes |
| `resourceId` | long | yes — use `0` for auto-pick |

**Response** — Route sheet object with fields: `id`, `incidentId`, `resourceId`, `routeSummaryText` (includes turn-by-turn directions), `createdAt`.

---

## Shelters

### `POST /api/shelters`
Create a shelter. CSRF required. Requires **ADMIN**.

**Body**
| Field | Type | Required |
|---|---|---|
| `name` | string | yes |
| `category` | string | yes — e.g. `"emergency"`, `"transitional"` |
| `neighborhood` | string | no |
| `addressText` | string | yes |
| `latitude` | decimal | no |
| `longitude` | decimal | no |

### `GET /api/shelters`
List shelters.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/shelters/{id}`
Get a single shelter.

### `PUT /api/shelters/{id}`
Update a shelter. CSRF required. Requires **ADMIN**.

**Body** — same as create plus `isActive: boolean`.

---

## Bulletins

### `POST /api/bulletins`
Create a bulletin. CSRF required. Requires **ADMIN** or **MODERATOR**.

**Body**
| Field | Type | Required |
|---|---|---|
| `title` | string | yes |
| `body` | string | yes |

**`BulletinStatus` values:** `DRAFT`, `PUBLISHED`, `HIDDEN`, `ARCHIVED`

### `GET /api/bulletins`
List bulletins.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/bulletins/{id}`
Get a single bulletin.

### `POST /api/bulletins/{id}/status`
Change bulletin status. CSRF required. Requires **ADMIN** or **MODERATOR**.

**Body**
| Field | Type | Required |
|---|---|---|
| `status` | `BulletinStatus` | yes |

---

## Favorites

### `POST /api/favorites`
Favorite a piece of content. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `contentType` | string | yes — `"incident"` or `"bulletin"` |
| `contentId` | long | yes |

### `DELETE /api/favorites`
Remove a favorite. CSRF required.

**Query params:** `contentType`, `contentId`

### `GET /api/favorites`
List the caller's favorites.

**Query params:** `page` (default 0), `size` (default 20)

---

## Comments

### `POST /api/comments`
Post a comment on a piece of content. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `contentType` | string | yes — `"incident"` or `"bulletin"` |
| `contentId` | long | yes |
| `body` | string | yes |

### `GET /api/comments`
List comments on a piece of content.

**Query params:**
| Param | Type | Required |
|---|---|---|
| `contentType` | string | yes |
| `contentId` | long | yes |
| `page` | int | default 0 |
| `size` | int | default 20 |

---

## Search

### `GET /api/search`
Full-text search across incidents and bulletins.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `q` | string | Search query (empty string returns all) |
| `type` | string | optional — `"incident"` or `"bulletin"` |
| `sort` | string | optional — `"recent"`, `"popular"`, `"favorites"`, `"comments"` |
| `page` | int | default 0 |
| `size` | int | default 20 |

**Response** — list of `SearchResult` items, each with: `contentType`, `item` (the full incident or bulletin object).

---

## Quality

### `GET /api/quality/results`
List quality rule results. Requires **QUALITY** or **ADMIN**.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/quality/results/{id}`
Get a single quality result.

**Quality result fields:** `id`, `visitId`, `ruleCode`, `severity` (`INFO`/`WARNING`/`BLOCKING`), `status` (`OPEN`/`OVERRIDDEN`/`RESOLVED`), `message`, `createdAt`.

### `POST /api/quality/results/{id}/override`
Override a quality result. CSRF required. Requires **QUALITY** or **ADMIN**.

**Body**
| Field | Type | Required |
|---|---|---|
| `reasonCode` | string | yes |
| `note` | string | yes |

### `POST /api/quality/corrective-actions`
Create a corrective action. CSRF required. Requires **QUALITY** or **ADMIN**.

**Body**
| Field | Type | Required |
|---|---|---|
| `description` | string | yes |
| `relatedVisitId` | long | no |
| `relatedRuleResultId` | long | no |

**`CorrectiveActionStatus` values and valid transitions:**
```
OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → VERIFIED_CLOSED
```

### `GET /api/quality/corrective-actions`
List corrective actions.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/quality/corrective-actions/{id}`
Get a single corrective action.

### `PUT /api/quality/corrective-actions/{id}`
Transition a corrective action to a new status. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `status` | `CorrectiveActionStatus` | yes |
| `resolutionNote` | string | no |
| `assignedTo` | long | no — user ID to assign |

Returns `422` if the transition is not valid (e.g., `OPEN → IN_PROGRESS` is not allowed; must go through `ASSIGNED` first).

---

## Sampling

Requires **QUALITY** or **ADMIN** role.

### `POST /api/sampling/runs`
Create a sampling run. CSRF required.

**Body**
| Field | Type | Required |
|---|---|---|
| `period` | string | yes — e.g. `"2026-04"` (must be unique per org) |
| `percentage` | int | yes — 1–100 |

Returns `422` if period is blank, percentage is 0 or > 100, or the period already has a run.

### `GET /api/sampling/runs`
List sampling runs.

**Query params:** `page` (default 0), `size` (default 20)

### `GET /api/sampling/runs/{id}/visits`
Get the visits sampled in a run. Returns a flat list.

---

## Ranking

### `GET /api/ranking/weights`
Get current ranking weights. Requires **ADMIN**.

**Response**
```json
{
  "data": {
    "recency": 1.5,
    "favorites": 2.0,
    "comments": 1.0,
    "moderatorBoost": 3.0,
    "coldStartBase": 0.5
  }
}
```

### `PUT /api/ranking/weights`
Update ranking weights. CSRF required. Requires **ADMIN**.

**Body** — same shape as the response above (all fields required).

### `POST /api/ranking/promote`
Manually promote a content item in the ranking. CSRF required. Requires **ADMIN** or **MODERATOR**.

**Body**
| Field | Type | Required |
|---|---|---|
| `contentType` | string | yes — `"incident"` or `"bulletin"` |
| `contentId` | long | yes |
| `favoriteCount` | int | no |
| `commentCount` | int | no |
| `ageHours` | long | no |

### `GET /api/ranking`
List ranked content entries.

**Query params:** `page` (default 0), `size` (default 20)

---

## Risk Scores

### `GET /api/risk-scores`
Requires **ADMIN**. Returns ephemeral session-level risk scores (informational only — not used for authorization).

**Response**
```json
{
  "data": {
    "ephemeral": true,
    "scores": { "192.168.1.1": 0.0, "10.0.0.5": 2.5 }
  }
}
```

---

## Retention

### `POST /api/retention/archive-run`
Trigger retention archiving of eligible records. CSRF required. Requires **ADMIN**.

**Response**
```json
{ "data": { "archivedCount": 14 } }
```

---

## Error Codes

| HTTP Status | Meaning |
|---|---|
| `400` | Validation error — missing required field, malformed value |
| `401` | Not authenticated — missing or invalid `X-Session-Token` |
| `403` | Forbidden — CSRF token missing/invalid, or insufficient role |
| `404` | Resource not found |
| `409` | Conflict — duplicate username, duplicate period, etc. |
| `422` | Business rule violation — invalid state transition, already initialized, time-window constraint, etc. |
| `500` | Internal server error |
