# design.md

## 1. System Overview

Community Rescue & Clinic Billing Hub is a role-based full-stack application for operating a local health organization entirely on an internal network. It combines:

* clinic intake and visit management
* community incident intake and routing support
* bulletin and incident discovery and ranking
* billing, settlement, refunds, and ledger exports
* quality control, anomaly checks, overrides, and corrective actions
* auditability, retention, backup, and restore-test tracking

The system serves these primary roles:

* Front Desk Clerk
* Clinician
* Billing Specialist
* Quality Reviewer
* Content Moderator
* System Administrator

The system uses:

* Frontend: Vue.js single-page web application
* Backend: Spring Boot RESTful API
* Database: MySQL
* File storage: local filesystem for incident media, exports, backups, route sheets, and generated reports
* Background execution: Spring scheduled jobs and background services
* Authentication: local username and password only
* Map and routing: locally hosted internal map layer and local resource dataset only
* Network boundary: internal HTTPS/TLS only

The platform must function entirely on an internal network with no dependence on external services. No cloud storage, no public map APIs, no third-party authentication providers, no SaaS billing platforms, and no remote monitoring services are allowed.

Primary business capabilities:

* role-based staff login and dashboard access
* patient registration, visit creation, identity verification metadata capture, and historical visit lookup
* clinician review of prior appointments and archived visit summaries
* incident report submission with photos and videos and approximate location descriptions
* permission-based map display with protected-case location masking
* locally generated printable route sheets to the nearest shelter or resource
* full-text content search and ranking for incidents and bulletins
* visit-close billing with service and package rules, discounts, taxes, and mixed tender reconciliation
* refund and void policy enforcement
* quality rules engine for cross-field checks, anomalies, duplicate detection, blocking, override notes, and second-look sampling
* immutable audit logging with before and after values
* optimistic locking, idempotency keys, nightly backups, monthly restore-test tracking, and soft-delete retention controls

---

## 2. Design Goals

* Full operation inside an internal network with zero dependency on internet services
* Clear separation between Vue UI, Spring controllers, services, repositories, jobs, and storage
* Strict backend enforcement for validation, authorization, privacy, billing correctness, and QC gates
* Deterministic workflows for visits, incidents, billing, settlement, refunds, overrides, exports, and archival
* Explicit role and object-scope behavior for sensitive data access
* Docker-first runtime that can start cleanly with one root command
* Stable contracts across frontend, backend, tests, and docs
* No silent fallbacks, mock-only flows, or UI-only enforcement
* Future-proof module boundaries so map data, backup mechanisms, or pricing policies can evolve later without rewriting core business rules

---

## 3. Scope, Deployment, and Trust Model

### 3.1 Deployment Mode

Supported deployment mode:

* one shared local deployment inside an internal network serving multiple browser users

In this mode:

* Vue frontend is served locally
* Spring Boot API runs locally over internal HTTPS/TLS
* MySQL runs locally or on-prem
* media, exports, backups, and generated documents are stored on mounted local volumes
* all users authenticate against the local system
* backend time is the source of truth for billing deadlines, refund windows, void windows, QC checks, sampling, and retention jobs

### 3.2 Trust Boundary

The UI is semi-trusted.

Trust rules:

* UI visibility is not security
* all business correctness is enforced in backend services
* all reads and writes must enforce role, object, and protected-case privacy rules
* client-supplied IDs, statuses, totals, coordinates, rankings, and permissions are never trusted without backend verification
* the database is never directly accessed by end users

### 3.3 Time Standard

* all timestamps are stored in UTC
* UI renders in configured local business timezone
* backend time is authoritative for:

  * refund within 30 days
  * void before daily close at 11:00 PM
  * QC 7-day anomaly windows
  * nightly backups
  * monthly restore-test cadence
  * 7-year patient archival thresholds

### 3.4 Offline and Internal Constraint

The system must remain fully usable with no internet access.

Therefore:

* all authentication is local
* maps and resources are locally hosted
* all uploads, exports, and backups are local
* all billing logic is internal
* all reports and route sheets are generated locally
* no core workflow may depend on a remote API

---

## 4. High-Level Architecture

### 4.1 Layered Architecture

```text
Vue.js SPA
    ↓
API client / route guards / feature modules / state management
    ↓
Spring Boot Controllers / DTO validation / security filters
    ↓
Application Services
    ↓
Repositories / transaction + optimistic locking layer
    ↓
MySQL + local filesystem
    ↓
Background jobs / rules engine / export generator / backup engine / route-sheet generator
```

### 4.2 Backend Modules

* auth
* users
* roles_permissions
* patients
* visits
* appointments
* incidents
* resources_and_shelters
* map_privacy
* bulletins
* search_ranking
* billing
* invoices
* tenders
* refunds_voids
* ledger
* quality_rules
* duplicates
* overrides
* corrective_actions
* exports
* audit
* backups
* retention
* settings
* scheduler
* storage
* crypto

### 4.3 Frontend Modules

* login and session
* role-based app shell
* front desk intake workspace
* clinician visit history workspace
* incident intake and map workspace
* discovery and search workspace
* billing and settlement workspace
* quality review workspace
* admin settings and security workspace
* shared tables, forms, filters, badges, dialogs, route-sheet print views, and audit views

### 4.4 Architecture Rules

Mandatory rules:

* controllers only parse requests, invoke services, and return structured responses
* services contain all validation, authorization, workflow, pricing, anomaly, and retention logic
* repositories contain DB access only
* file operations live in storage, export, and backup services only
* every read and write path enforces privacy and role rules
* all create, update, approve, export, reveal, and override actions are auditable
* frontend never computes authoritative invoice totals, settlement states, or QC outcomes
* protected-case location masking must be enforced in services, not only hidden in UI
* tests must target service rules, API behavior, and end-to-end flows

Forbidden:

* DB access in controllers
* billing calculations in Vue components
* authorization enforced only in the frontend
* external service dependencies for map, routing, auth, or storage
* silent downgrade from blocked QC flow to permissive completion
* partial execution of billing, refund, export, or override actions without explicit result state

---

## 5. Repository and Package Layout

### 5.1 Required Root Structure

```text
prompt.md
questions.md
docs/
  design.md
  api-spec.md
fullstack/
  README.md
  docker-compose.yml
  run_tests.sh
  frontend/
  backend/
  unit_tests/
  API_tests/
  storage/
```

### 5.2 Backend Layout

```text
backend/
  pom.xml
  src/
    main/
      java/
        .../
          config/
          controller/
          dto/
          entity/
          enums/
          exception/
          repository/
          security/
          service/
          scheduler/
          storage/
          audit/
          rules/
          search/
          backup/
          retention/
      resources/
        application.yml
        db/
          migration/
    test/
```

### 5.3 Frontend Layout

```text
frontend/
  package.json
  src/
    app/
    api/
    components/
    features/
      auth/
      patients/
      visits/
      incidents/
      map/
      search/
      billing/
      quality/
      admin/
    router/
    stores/
    utils/
    types/
```

### 5.4 Project Structure Principles

* each module has clear ownership and API boundaries
* no giant mixed-responsibility files
* DTOs, services, API tests, and docs must stay aligned
* runtime, tests, and README must use the same commands and ports
* storage directories must be explicit and Docker-mounted
* no dead endpoints or disconnected UI flows

---

## 6. Domain Model

### 6.1 Organization

Fields:

* id
* code
* name
* is_active
* created_at
* updated_at

Rules:

* all operational records belong to one organization
* inactive organizations remain queryable for history and audit but cannot receive new transactional data

### 6.2 User

Fields:

* id
* organization_id
* username
* password_hash
* display_name
* role
* is_active
* is_frozen
* password_changed_at
* created_at
* updated_at

Rules:

* usernames are unique
* passwords use bcrypt
* frozen users cannot authenticate or perform protected actions
* role changes are auditable

### 6.3 UserSession

Fields:

* id
* user_id
* session_token_hash
* csrf_token_hash
* workstation_id
* ip_address
* issued_at
* expires_at
* last_seen_at
* revoked_at_nullable

Rules:

* session expiry and revocation are enforced on backend
* mutating requests require valid CSRF token
* workstation and IP are captured for audit and rate-limit context

Workstation Identification:

- workstation_id is derived on the server
- defined as a stable hash of (IP address + User-Agent string)
- client-provided identifiers are not trusted
- used for:
  - rate limiting
  - audit logging
  - anomaly detection

Rules:

- workstation_id must be consistent for a session
- changes in IP/User-Agent result in a new workstation context

### 6.4 Patient

Fields:

* id
* organization_id
* medical_record_number
* first_name_ciphertext
* first_name_iv
* last_name_ciphertext
* last_name_iv
* date_of_birth
* sex_nullable
* phone_ciphertext_nullable
* phone_iv_nullable
* phone_last4_nullable
* address_ciphertext_nullable
* address_iv_nullable
* emergency_contact_name_nullable
* emergency_contact_phone_ciphertext_nullable
* emergency_contact_phone_iv_nullable
* is_minor
* is_protected_case
* archived_at_nullable
* legal_hold_until_nullable
* created_at
* updated_at
* version

Rules:

* patient identity is persistent across visits
* sensitive fields are encrypted at rest
* inactive patient records archive after 7 years unless legal hold applies
* patient records are soft-deleted or archived, never hard-deleted silently

### 6.5 PatientIdentityVerification

Fields:

* id
* patient_id
* verified_by_user_id
* document_type
* document_last4
* verified_at
* note_nullable

Rules:

* only minimal ID verification metadata is stored by default
* full identifier values are not stored unless explicitly required by later policy
* views showing ID data are masked by default and audited on reveal

### 6.6 Appointment

Fields:

* id
* organization_id
* patient_id
* scheduled_date
* scheduled_time_nullable
* clinician_user_id_nullable
* status
* archived_at_nullable
* created_at
* updated_at
* version

Status enum:

* scheduled
* checked_in
* completed
* canceled
* no_show
* archived

Rules:

* clinicians can search prior appointments by date
* archived appointments remain readable with clear archived badge
* invalid state transitions are rejected

### 6.7 Visit

Fields:

* id
* organization_id
* patient_id
* appointment_id_nullable
* created_by_user_id
* clinician_user_id_nullable
* opened_at
* closed_at_nullable
* status
* chief_complaint_nullable
* summary_text
* diagnosis_text_nullable
* qc_blocked
* qc_override_required
* archived_at_nullable
* created_at
* updated_at
* version

Status enum:

* open
* ready_for_close
* closed
* archived

Rules:

* bill generation occurs when visit closes
* visit cannot close when blocking QC rules remain unresolved unless Quality Reviewer override exists
* archived visits remain searchable and display archived badge clearly

### 6.8 IncidentReport

Fields:

* id
* organization_id
* submitted_by_user_id_nullable
* external_reporter_name_nullable
* is_anonymous
* subject_age_group_nullable
* involves_minor
* is_protected_case
* category
* description
* approximate_location_text
* neighborhood_nullable
* nearest_cross_streets_nullable
* exact_location_ciphertext_nullable
* exact_location_iv_nullable
* status
* favorite_count
* comment_count
* view_count
* created_at
* updated_at
* version

Status enum:

* submitted
* reviewed
* published
* hidden
* archived

Rules:

* approximate location is always allowed
* exact location is restricted for minors and protected cases
* protected-case visibility rules are enforced in service layer
* favorites and comments contribute to discovery ranking

### 6.9 IncidentMediaFile

Fields:

* id
* incident_report_id
* file_name
* file_type
* file_size_bytes
* sha256_hash
* storage_path
* uploaded_by_user_id_nullable
* created_at

Rules:

* only validated local files are accepted
* file type, size, and binary signature are checked before persist
* all media remain on local storage only

### 6.10 ShelterResource

Fields:

* id
* organization_id
* name
* category
* neighborhood
* address_text
* latitude_nullable
* longitude_nullable
* is_active
* created_at
* updated_at

Rules:

* resources are locally hosted and maintained
* routing always uses local resource data only

### 6.11 RouteSheet

Fields:

* id
* incident_report_id
* resource_id
* generated_by_user_id
* route_summary_text
* printable_file_path
* created_at

Rules:

* route sheet generation is local-only
* printed directions must reflect privacy restrictions for protected incidents
* every generated route sheet is auditable

### 6.12 Bulletin

Fields:

* id
* organization_id
* title
* body
* status
* created_by_user_id
* moderated_by_user_id_nullable
* favorite_count
* comment_count
* view_count
* created_at
* updated_at
* version

Status enum:

* draft
* published
* hidden
* archived

Rules:

* moderators can rank or promote items
* published and archived states must be explicit

### 6.13 RankedContentEntry

Fields:

* id
* organization_id
* content_type
* content_id
* weighting_snapshot_json
* score
* promoted_by_user_id_nullable
* created_at
* updated_at

Rules:

* ranked lists are derived from configurable weighting
* cold-start support must not exclude low-engagement new items entirely
* ranking adjustments are auditable

### 6.14 VisitCharge

Fields:

* id
* visit_id
* service_code
* description
* pricing_source_type
* unit_price
* quantity
* line_total
* taxable
* created_at

Rules:

* charges are generated from configured service and package rules
* line items are immutable after finalization unless authorized adjustment flow is used

### 6.15 Invoice

Fields:

* id
* organization_id
* patient_id
* visit_id
* invoice_number
* status
* subtotal_amount
* discount_amount
* tax_amount
* total_amount
* outstanding_amount
* generated_at
* daily_close_date_nullable
* voided_at_nullable
* refunded_at_nullable
* created_at
* updated_at
* version

Status enum:

* open
* partially_paid
* paid
* voided
* partially_refunded
* refunded

Rules:

* invoices auto-generate on visit close
* total discount per invoice cannot exceed $200.00
* outstanding amount is derived from invoice and tender activity
* void only allowed before daily close at 11:00 PM
* refund only allowed within 30 days

### 6.16 DailyClose

Fields:

- id
- organization_id
- business_date
- closed_by_user_id
- closed_at
- status

Status enum:

- open
- closed

Rules:

- Daily close is an explicit, auditable operation performed by an authorized Billing Specialist or Administrator
- business_date represents the operational day being closed
- Only one DailyClose record may exist per organization per business_date
- Void operations are only allowed if no DailyClose exists for the invoice’s business_date
- The 11:00 PM time represents a policy boundary but is not the sole enforcement mechanism
- Once closed, the day is locked for void operations

### 6.17 InvoiceTender

Fields:

* id
* invoice_id
* tender_type
* amount
* status
* external_reference_nullable
* created_at
* updated_at

Tender type enum:

* cash
* check
* external_card_on_file

Status enum:

* pending
* recorded
* reconciled
* voided
* refunded

Rules:

* mixed tender is tracked as multiple allocations
* external card-on-file is recorded only as an internal external tender record
* invoice reconciliation derives overall tender outcome from allocations

### 6.18 BillingRule

Fields:

* id
* organization_id
* rule_type
* code
* name
* amount_nullable
* percentage_nullable
* tax_rate_nullable
* package_definition_json_nullable
* is_active
* priority
* created_at
* updated_at

Rule type enum:

* service
* package
* discount
* tax

Rules:

* calculation order is deterministic
* discount cap is enforced at invoice level
* overlapping active rules must resolve by priority or fail validation

### 6.19 RefundRequest

Fields:

* id
* invoice_id
* requested_by_user_id
* approved_by_user_id_nullable
* refund_amount
* refund_reason
* status
* created_at
* approved_at_nullable
* executed_at_nullable

Status enum:

* pending
* approved
* rejected
* executed

Rules:

* refunds allowed only within 30 days
* refund amount cannot exceed remaining refundable balance
* refund and void actions are auditable and policy-gated

### 6.20 LedgerEntry

Fields:

* id
* organization_id
* invoice_id_nullable
* refund_request_id_nullable
* entry_type
* amount
* occurred_at
* before_json_nullable
* after_json_nullable

Entry type enum:

* invoice_generated
* payment_recorded
* refund_executed
* invoice_voided
* tax_recorded
* discount_recorded

Rules:

* ledger is append-only
* financial source of truth is derived from invoice plus ledger consistency
* exports must come from authoritative ledger-backed calculations

### 6.21 QualityRuleResult

Fields:

* id
* organization_id
* visit_id_nullable
* patient_id_nullable
* incident_report_id_nullable
* rule_code
* severity
* status
* result_details_json
* created_at
* resolved_at_nullable

Severity enum:

* info
* warning
* blocking

Status enum:

* open
* overridden
* resolved

Rules:

* blocking issues prevent visit close or other protected operations unless override exists
* all rule outputs are auditable

### 6.22 QualityOverride

Fields:

* id
* quality_rule_result_id
* overridden_by_user_id
* override_reason_code
* override_note
* created_at

Rules:

* only Quality Reviewer can override blocking or anomalous items
* override note is mandatory
* overrides do not delete original rule result

### 6.23 CorrectiveAction

Fields:

* id
* organization_id
* related_visit_id_nullable
* related_rule_result_id_nullable
* assigned_to_user_id_nullable
* status
* description
* resolution_note_nullable
* created_at
* updated_at
* closed_at_nullable

Status enum:

* open
* assigned
* in_progress
* resolved
* verified_closed

Rules:

* second-look review outcomes can spawn corrective actions
* corrective actions must close through explicit lifecycle states

### 6.24 DuplicateFingerprint

Fields:

* id
* organization_id
* fingerprint_type
* fingerprint_value
* object_type
* object_id
* created_at

Rules:

* deterministic duplicate detection uses per-domain fingerprints
* duplicate checks are performed in services, not UI
* idempotency and duplicate detection are related but distinct

### 6.25 AuditLog

Fields:

* id
* organization_id
* actor_user_id_nullable
* actor_username_snapshot
* action_code
* object_type
* object_id
* ip_address_nullable
* workstation_id_nullable
* before_json_nullable
* after_json_nullable
* created_at

Rules:

* audit log is immutable
* create, update, approve, export, reveal, override, and soft-delete actions are always logged
* logs must preserve enough context for later review

### 6.26 BackupRun

Fields:

* id
* organization_id
* backup_type
* output_path
* retention_expires_at
* status
* created_at
* completed_at_nullable

Status enum:

* running
* completed
* failed

Rules:

* backups run nightly to on-prem directory
* retention is 30 days
* failed backups are explicit and auditable

### 6.27 RestoreTestLog

Fields:

* id
* organization_id
* performed_by_user_id
* backup_run_id
* result
* note_nullable
* performed_at

Result enum:

* passed
* failed

Rules:

* restore test is documented monthly
* restore-test evidence is auditable
* this is not the same as live restore

### 6.28 RetentionPolicyHold

Fields:

* id
* patient_id
* hold_reason
* hold_until_nullable
* created_by_user_id
* created_at

Rules:

* legal or policy hold can suspend archival or deletion-related retention steps
* holds must be explicit and auditable

### 6.29 Favorite

Fields:

- id
- organization_id
- user_id
- content_type
- content_id
- created_at

Rules:

- Represents a user marking content as favorite
- favorite_count is derived from this table, not stored independently

---

### 6.30 Comment

Fields:

- id
- organization_id
- user_id
- content_type
- content_id
- body
- created_at

Rules:

- Comments are auditable
- comment_count is derived, not authoritative

---

### 6.31 ViewEvent

Fields:

- id
- organization_id
- user_id_nullable
- content_type
- content_id
- viewed_at

Rules:

- Used for tracking engagement and ranking
- view_count is derived from ViewEvent records

---

### 6.32 Payment

Fields:

- id
- organization_id
- invoice_id
- tender_type
- amount
- occurred_at
- external_reference_nullable
- created_at

Rules:

- Represents the source-of-truth financial payment event
- Ledger entries for payment_recorded must reference a Payment
- InvoiceTender acts as allocation metadata, not the payment source of truth

---

## 7. Role Model and Authorization

### 7.1 Roles

#### Front Desk Clerk

Capabilities:

* register patient visit
* capture demographics and contact details
* record ID document type and last four digits
* view masked sensitive data
* cannot author clinical summaries or approve billing and QC overrides

#### Clinician

Capabilities:

* review historical appointments by date
* reopen historical visit summaries where allowed
* author and close encounter documentation
* trigger visit-close billing indirectly through visit close
* cannot perform finance-only settlement actions unless separately authorized

#### Billing Specialist

Capabilities:

* review invoices and mixed tenders
* reconcile settlement
* process refunds and voids within policy
* generate daily settlement reports and ledger exports
* confirm high-volume export secondary confirmation flow

#### Quality Reviewer

Capabilities:

* review blocking and anomalous items
* submit override notes
* review second-look sampled visits
* track corrective actions to closure
* cannot bypass audit logging

#### Content Moderator

Capabilities:

* moderate incidents and bulletins
* adjust ranked list weighting inputs where allowed
* hide or promote content
* manage publication visibility for moderated content

#### System Administrator

Capabilities:

* manage users, roles, settings, allowlist and denylist, security policies, backups, restore-test logs, and retention settings
* review audit logs
* manage protected-case visibility policy
* cannot bypass immutable audit or retention records silently

### 7.2 Authorization Rules

* all records are organization-scoped
* all reads enforce scope, not only writes
* protected-case location visibility is object-sensitive
* patient and incident sensitive reveals require explicit permission and audit
* service layer is authoritative
* UI hiding is convenience only, never enforcement

### 7.3 Permission Strategy

Authorization decisions combine:

* authenticated user role
* organization scope
* object ownership or assignment where relevant
* protected-case sensitivity
* current entity state
* export size and elevation policy

Examples:

* Front Desk can register patient and view masked ID metadata but cannot reveal hidden protected location data
* Clinician can read archived visit summaries and close visits but cannot override QC blocks without reviewer role
* Billing Specialist can export financial records but exports above 500 rows require secondary confirmation and elevated role
* Quality Reviewer can override blocked QC issues only with required note
* Content Moderator can rank incidents and bulletins but cannot settle invoices

---

## 8. Authentication, Sessions, and Security

### 8.1 Authentication Model

* username and password only
* bcrypt password hashing
* local session-based authentication
* no OAuth, SSO, magic links, or external auth providers

### 8.2 Session Rules

* signed, time-limited session cookies or equivalent secure server-side session model
* CSRF protection on all mutating requests
* session expiry and revocation enforced on backend
* login attempts rate-limited to 10 per 10 minutes per workstation

### 8.3 Sensitive Data and Encryption at Rest

Sensitive fields include:

* patient names
* patient phone numbers
* addresses
* emergency contact fields
* exact incident locations where stored
* any other explicitly designated protected values

Rules:

* encrypted at rest
* masked by default in UI
* reveal actions require explicit authorized path
* reveal actions are audited

### 8.4 Security Protections

* CSRF protection on all mutating endpoints
* XSS-safe rendering and input sanitization
* SQL injection protection through parameterized persistence access
* allowlist and denylist policy layer
* anomalous login and repeated export attempt risk scoring
* immutable audit records for create, update, approve, export, reveal, and override actions

### 8.5 TLS and Local Certificates

* internal HTTPS/TLS is required
* certificate material is locally managed
* private key material is never returned by the application
* certificate metadata may be visible to admins where needed

---

## 9. Patient Intake, Visits, and Historical Record Design

### 9.1 Patient Registration Flow

Flow:

1. Front Desk creates or locates patient
2. captures demographics and contact details
3. records ID verification metadata
4. opens visit or links visit to scheduled appointment

Rules:

* full identifiers are masked by default
* on-screen watermarking is enabled for sensitive views
* duplicate patient detection can raise a deterministic fingerprint warning

### 9.2 Historical Visit Lookup

Clinicians can:

* search appointments by date using MM/DD/YYYY input
* reopen historical visit summaries
* see archived badges clearly on archived records

Rules:

* archived status is explicit, not inferred only by age
* reopening summary does not silently unarchive record

### 9.3 Visit Close and Bill Generation

Rules:

* visit close generates bill automatically
* visit cannot close if blocking QC anomalies remain unresolved
* Quality Reviewer override with note can unblock when policy allows
* bill generation must be idempotent

---

## 10. Incident Intake, Map Privacy, and Routing Design

### 10.1 Incident Submission

Flow:

1. community member or call-center staff submits incident
2. enters approximate location text
3. attaches photos or videos
4. report enters moderation and review flow

Rules:

* approximate location text is always allowed
* media stays local
* duplicate detection may flag repeat incident submissions

### 10.2 Map Visibility and Protected Cases

Rules:

* locally hosted map layer only
* nearby shelters and resources shown on local data only
* minors and protected cases may hide exact location details
* authorization determines whether exact location, approximate location, or only coarse neighborhood is shown

### 10.3 Route Sheet Generation

Rules:

* route sheet is printable and locally generated
* nearest resource determination uses local dataset
* turn-by-turn instructions use local route logic where available
* if exact routing is unavailable, fallback to structured local directions summary
* generated route sheets are auditable

---

## 11. Search, Discovery, and Ranking Design

### 11.1 Unified Search Surface

Search spans:

* incidents
* bulletins

Filters include:

* time
* popularity
* favorites
* comments
* content type

Rules:

* full-text indexing is server-side
* filters and ranking are backend-driven
* hidden or protected content respects permission checks

### 11.2 Ranking and Cold-Start Handling

Rules:

* ranking is based on configurable weighting
* weighting can include recency, favorites, comments, moderator boost, and cold-start base weight
* cold-start items must remain discoverable
* ranking changes are auditable

---

## 12. Billing, Settlement, Refund, and Void Design

### 12.1 Invoice Calculation Order

Apply in this order:

1. service and package rules
2. package adjustments
3. discounts
4. discount cap enforcement at $200.00 total per invoice
5. tax calculation where applicable
6. total and outstanding reconciliation

Rules:

* calculation order is deterministic
* frontend never computes authoritative totals
* invoice snapshots are audit-backed

### 12.2 Mixed Tender Model

Rules:

* mixed tender is represented as multiple tender allocations
* cash, check, and external card-on-file are tracked independently
* invoice settlement status is derived from allocations, not one opaque payment record

### 12.3 Refund Rules

* refunds allowed within 30 days
* refund amount cannot exceed refundable balance
* refund actions are auditable
* refund execution writes ledger entries atomically

### 12.4 Void Rules

* void only allowed before daily close at 11:00 PM
* void is distinct from refund
* voided invoices remain visible and auditable
* no new payments on voided invoices

### 12.5 Daily Settlement and Export Rules

Rules:

* daily settlement reports generate local files only
* ledger exports generate local files only
* exports above 500 rows require second confirmation plus elevated role
* export actions are auditable with file metadata

---

## 13. Quality Control, Rules Engine, and Corrective Action Design

### 13.1 Rules Engine

The rules engine evaluates:

* cross-field consistency such as age vs DOB
* threshold and trend anomalies such as unusually high test frequency within 7 days
* deterministic duplicate detection
* missing or anomalous required items

Rules:

* blocking results prevent protected workflows from proceeding
* non-blocking warnings remain visible and auditable
* rules run in backend services

### 13.2 Override Model

Rules:

* only Quality Reviewer can override blocking or anomalous items
* override note is mandatory
* override never erases the original result
* override action is audited

### 13.3 Sampling Review

Rules:

* 5% of closed visits are assigned for second-look review
* sampling must be deterministic and auditable
* second-look can generate corrective actions tracked to closure

### 13.4 Sampling Model

Entities:

SamplingRun:

- id
- organization_id
- period
- seed
- percentage
- created_at

SampledVisit:

- id
- sampling_run_id
- visit_id
- selection_reason

Rules:

- Sampling must be deterministic and reproducible
- Selection uses:

  hash(visit_id + seed) % 100 < percentage

- SamplingRun defines the seed and selection window
- A visit may only belong to one SamplingRun for a given period
- Re-running the job must produce identical results

---

## 14. Audit, Risk, and Observability Design

### 14.1 Audit Coverage

Audit logs must cover:

* authentication events
* patient registration and update
* ID verification metadata capture
* visit close
* incident submission and moderation
* ranking changes
* invoice generation, payment recording, refunds, voids
* export actions
* override actions
* backup and restore-test actions
* reveal of sensitive fields or locations

### 14.2 Risk Scoring

Risk scoring evaluates:

* anomalous logins
* repeated export attempts
* other configurable high-risk events

Rules:

* risk score can trigger warn-only, secondary confirmation, temporary block, or admin follow-up depending on policy
* risk outcomes are auditable

### 14.3 Logging Rules

* structured logs only
* no raw passwords or unmasked sensitive values in logs
* logs stored locally
* operational errors are explicit and diagnosable

---

## 15. Backup, Retention, and Restore-Test Design

### 15.1 Backups

Rules:

* nightly backups to on-prem directory
* 30-day retention
* backup success or failure is explicit
* backup metadata is auditable

### 15.2 Restore Test Tracking

Rules:

* monthly restore test must be documented in system
* restore test log stores operator, date, backup used, result, and note
* restore test tracking is not optional documentation-only behavior

### 15.3 Retention and Soft Delete

Rules:

* deletions are soft-delete and traceable
* inactive patient records archive after 7 years unless legal hold or longer policy applies
* archived records remain retrievable through authorized administrative paths
* no silent hard deletion of protected operational records

---

## 16. Data Integrity and Concurrency Rules

### 16.1 Core Integrity Rules

* no cross-organization references
* no orphan visit, invoice, refund, or corrective-action records
* patient, visit, and billing relationships must remain valid
* protected-case flags must remain consistent across dependent display paths
* no critical action may partially mutate business state without explicit result

### 16.2 Optimistic Locking Rules

* versioned entities use optimistic locking
* stale updates fail explicitly
* UI must refresh from authoritative backend state after conflict

### 16.3 Idempotency Rules

Idempotency keys are required for:

* visit registration submit
* incident submission
* invoice finalization on visit close
* refund or void execution
* export job creation

Rules:

* duplicate submissions return consistent idempotent result
* duplicate requests must not create duplicate business artifacts

### 16.4 No Silent Conflict Handling

If an action fails because of:

* stale version
* duplicate submission
* blocked QC issue
* out-of-scope export
* invalid refund window
* invalid void after daily close
* duplicate incident or patient fingerprint hit

the system must return an explicit structured result, not silently modify behavior

### 16.4 IdempotencyKey

Fields:

- id
- organization_id
- user_id
- key
- request_hash
- response_snapshot_json
- created_at
- expires_at

Rules:

- Unique per (organization_id, key)
- Required for:
  - visit creation
  - incident submission
  - invoice generation
  - refund and void operations
  - export job creation
- Duplicate requests must return the same response_snapshot
- Idempotency prevents duplicate financial and operational records

---

## 17. API Design Principles

### 17.1 API Style

* REST-style JSON APIs
* DTO-based request and response contracts
* standard HTTP status codes
* structured JSON errors

Error shape:

```json
{
  "code": 403,
  "message": "Forbidden",
  "details": null
}
```

### 17.2 Security and Validation

* authenticated routes require valid session
* CSRF required on mutating endpoints
* service layer validates role, organization scope, and object visibility
* request DTO validation handles shape and format
* controllers never trust client-supplied totals, statuses, ranking scores, or privacy assumptions

### 17.3 API Coverage Areas

* auth and session
* patients and visits
* appointments
* incidents and media
* map resources and route sheets
* search and ranking
* billing, invoices, tenders, refunds, voids
* quality rules, overrides, corrective actions
* exports, audit, settings, backup, and retention

### 17.4 Contract Stability

* DTOs are part of the contract
* schema drift across controllers, frontend, tests, and docs is a defect
* no silent field additions or removals without coordinated update

---

## 18. Frontend UX and State Design

### 18.1 App Shell

Must provide:

* role-aware navigation
* current session and user context
* clear module separation
* loading and error boundaries
* confirmation dialogs for sensitive actions

### 18.2 Required States

All major views must support:

* loading
* empty
* validation feedback
* success feedback
* permission denied
* recoverable error
* disabled or submitting state for critical actions

### 18.3 UI-Service Parity

Rules:

* UI may hide impossible actions when state and permissions are already known
* backend remains the final authority
* sensitive fields are masked by default
* watermarking in sensitive views is display-level only and does not replace backend controls
* frontend must re-render from authoritative backend state after mutations

### 18.4 Accessibility

* semantic form controls
* keyboard support for dialogs and menus
* visible state indicators
* clear loading and disabled states
* no dead-end flows for core intake, billing, incident, or QC work

---

## 19. Scheduler and Background Jobs

### 19.1 Job Types

* nightly backups
* monthly restore-test reminder and compliance check
* archival and retention processing
* QC anomaly trend windows and second-look sampling selection
* duplicate cleanup and idempotency-key cleanup
* export expiration or cleanup where applicable

### 19.2 Startup Reconciliation

On startup, the system should:

* reconcile unfinished scheduled jobs safely
* resume eligible background processing idempotently
* record prior failed jobs explicitly
* not duplicate invoices, exports, or archival actions

### 19.3 Job Idempotency

Jobs must not create duplicate business artifacts for the same logical window.

Examples:

* one visit-close invoice per visit close
* one restore-test compliance record per tracked run
* one archival action per eligible record state transition
* one export artifact per approved export execution key

### 19.4 Scheduler Failure Rules

* job failure must be explicit
* partial failure must not silently mark job complete
* retries must be idempotent
* admin-visible diagnostics should expose failed job state

---

## 20. Testing and QA Requirements

### 20.1 Minimum Test Categories

The implementation must include:

* backend unit tests for services and rules engine
* repository and persistence integration tests
* API tests for auth, billing, incident, and QC flows
* frontend component and interaction tests for critical workflows
* end-to-end tests for core role-based flows
* backup and retention logic tests
* idempotency and optimistic-locking conflict tests

### 20.2 Critical Flow Coverage

At minimum, tests must cover:

* patient registration and visit close
* historical appointment lookup and archived badge behavior
* incident submission with protected-case privacy enforcement
* route-sheet generation to nearest resource
* invoice generation, mixed tender reconciliation, refund, and void boundaries
* QC blocking, override, second-look sampling, and corrective action lifecycle
* export threshold confirmation and elevated-role enforcement
* backup creation and restore-test logging

### 20.3 QA Rules

* no fake success states
* no dead endpoints
* no UI-only enforcement
* no partial workflows
* all major actions must be auditable
* root runnability must work with one documented command
