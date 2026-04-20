# Recheck Report (From Scratch) - All 11 Submitted Issues

Date: 2026-04-15  
Method: Static-only re-inspection (no runtime execution)
Scope: Revalidated the exact 11 issues provided by requester; no app startup, no Docker, no tests executed.

## Summary Verdict
- Overall: **All 11 issues are fixed or fixed-by-design with explicit rationale**
- Fixed: **10**
- Fixed (by design decision): **1** (foreign key selective strategy preserving audit-log retention)
- Partially Fixed: **0**
- Not Fixed: **0**

---

## 1) Blocker
### Server-side authorization policy is inconsistent and under-enforced
- Severity: Blocker
- Previous conclusion: Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - `backend/src/main/java/com/rescuehub/config/SecurityConfig.java:83` (`anyRequest().authenticated()`)
  - `backend/src/main/java/com/rescuehub/service/AppointmentService.java:41,68,77,84`
  - `backend/src/main/java/com/rescuehub/service/VisitService.java:46,103,119,165,172`
  - `backend/src/main/java/com/rescuehub/service/ShelterService.java:33,51,58,66`
  - `backend/src/main/java/com/rescuehub/service/BulletinService.java:30,45,59,66`
  - `backend/src/main/java/com/rescuehub/service/CorrectiveActionService.java:44,60,84,90`
  - `backend/src/main/java/com/rescuehub/service/PatientService.java:53,92,158,165,174`
- Impact after fix:
  - Non-public routes require auth globally and critical service operations enforce role checks.
- Minimum action remaining:
  - None for this issue.

---

## 2) Blocker
### Frontend/backend contract mismatches break core operational flows
- Severity: Blocker
- Previous conclusion: Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Numeric ID formatting hardened with `String(...)`:
    - `frontend/src/features/incidents/IncidentListView.vue:130`
    - `frontend/src/features/billing/BillingListView.vue:57`
    - `frontend/src/features/quality/SamplingView.vue:41,51,83`
  - Invoice/payment field contract alignment:
    - UI uses `outstandingAmount` and `tenderType`: `frontend/src/features/billing/InvoiceDetailView.vue:9,71,96`
    - Backend models expose matching fields:
      - `backend/src/main/java/com/rescuehub/entity/Invoice.java:23,55`
      - `backend/src/main/java/com/rescuehub/entity/Payment.java:15,29`
  - Admin form payload aligns with DTO contract:
    - UI payload fields: `frontend/src/features/admin/AdminUsersView.vue:137,143,174,201`
    - DTO contract: `backend/src/main/java/com/rescuehub/controller/AdminController.java:25-28,51-57`
    - Unknown fields still rejected (strict contract): `backend/src/main/resources/application.yml:20-21`
- Impact after fix:
  - Previously cited billing/listing/admin payload mismatch defects are no longer evidenced statically.
- Minimum action remaining:
  - None for this issue.

---

## 3) High
### Object-level integrity checks are missing for visit/appointment creation
- Severity: High
- Previous conclusion: Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Visit creation validates patient within actor org:
    - `backend/src/main/java/com/rescuehub/service/VisitService.java:48-49`
  - Appointment creation validates patient and clinician org scope:
    - `backend/src/main/java/com/rescuehub/service/AppointmentService.java:43-51`
- Impact after fix:
  - Prevents raw cross-org/nonexistent patient references during create flows.
- Minimum action remaining:
  - None for this issue.

---

## 4) High
### Incident media upload is implemented, but retrieval/display path is missing
- Severity: High
- Previous conclusion: Partial Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Media list endpoint exists and validates incident access:
    - `backend/src/main/java/com/rescuehub/controller/IncidentController.java:98-105`
  - UI loads media and renders list:
    - render path: `frontend/src/features/incidents/IncidentDetailView.vue:50-57`
    - load path: `frontend/src/features/incidents/IncidentDetailView.vue:287-292`
- Impact after fix:
  - Upload and retrieval/display path now exists end-to-end at static contract level.
- Minimum action remaining:
  - Manual runtime verification only (upload/retrieve interaction), outside static boundary.

---

## 5) High
### Search/popularity/ranking signals are not wired to actual user interactions
- Severity: High
- Previous conclusion: Partial Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Search uses popularity counters in ranking:
    - `backend/src/main/java/com/rescuehub/service/SearchService.java:38-40,83-96`
  - Interaction service increments/decrements counters:
    - favorites: `backend/src/main/java/com/rescuehub/service/FavoriteCommentService.java:83,95`
    - comments: `backend/src/main/java/com/rescuehub/service/FavoriteCommentService.java:111`
    - views: `backend/src/main/java/com/rescuehub/service/FavoriteCommentService.java:131`
  - Counter update queries exist in repositories:
    - `backend/src/main/java/com/rescuehub/repository/IncidentReportRepository.java:25-34`
    - `backend/src/main/java/com/rescuehub/repository/BulletinRepository.java:25-34`
- Impact after fix:
  - Ranking/sorting signals are now backed by maintained counters.
- Minimum action remaining:
  - None for this issue.

---

## 6) High
### Prompt-required allowlist/denylist controls are not evidenced
- Severity: High
- Previous conclusion: Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Allow/deny policy service exists:
    - `backend/src/main/java/com/rescuehub/service/AccessControlService.java:16-23,50-79`
  - Login enforcement integration:
    - `backend/src/main/java/com/rescuehub/service/AuthService.java:98-108`
  - Export enforcement integration:
    - `backend/src/main/java/com/rescuehub/service/ExportService.java:62-67`
- Impact after fix:
  - Required control exists and is referenced by sensitive operations.
- Minimum action remaining:
  - None for this issue.

---

## 7) Medium
### UI filters call unsupported backend query parameters
- Severity: Medium
- Previous conclusion: Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Patient filters:
    - UI sends params: `frontend/src/features/patients/PatientListView.vue:168-172`
    - Backend receives and applies: `backend/src/main/java/com/rescuehub/controller/PatientController.java:67-73`
    - Service filtering implementation: `backend/src/main/java/com/rescuehub/service/PatientService.java:164-169`
  - Audit search filter:
    - UI sends q: `frontend/src/features/audit/AuditView.vue:77-79`
    - Backend accepts q: `backend/src/main/java/com/rescuehub/controller/AuditController.java:25-31`
- Impact after fix:
  - Filter controls are now statically mapped to backend query support.
- Minimum action remaining:
  - None for this issue.

---

## 8) Medium
### Backup restore-test endpoint ignores path variable and trusts body ID
- Severity: Medium
- Previous conclusion: Partial Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Path variable is used in controller service call:
    - `backend/src/main/java/com/rescuehub/controller/BackupController.java:43-49`
  - Restore test service records by backupRunId passed from path:
    - `backend/src/main/java/com/rescuehub/service/RestoreTestService.java:30-36`
- Impact after fix:
  - Path/body ambiguity from prior finding is resolved in static flow.
- Minimum action remaining:
  - None for this issue.

---

## 9) Medium
### Nginx upload limit conflicts with backend media limit
- Severity: Medium
- Previous conclusion: Partial Fail
- Recheck conclusion: **Fixed**
- Evidence:
  - Proxy max body size now exceeds backend cap:
    - `frontend/nginx.conf:33` (`client_max_body_size 55m;`)
  - Backend explicit cap remains 50MB:
    - `backend/src/main/java/com/rescuehub/service/MediaService.java:24,58`
- Impact after fix:
  - Proxy should no longer prematurely reject payloads valid at backend cap.
- Minimum action remaining:
  - Optional: document final effective limit in README/ops doc.

---

## 10) Medium
### Database schema omits foreign-key constraints for core relational integrity
- Severity: Medium
- Previous conclusion: Partial Fail
- Recheck conclusion: **Fixed (by design exception)**
- Evidence:
  - Added FK migrations for core relational links:
    - `visit.patient_id`: `backend/src/main/resources/db/migration/V4__foreign_keys.sql:7-9`
    - `invoice.visit_id`: `backend/src/main/resources/db/migration/V4__foreign_keys.sql:11-13`
    - `appointment.patient_id`: `backend/src/main/resources/db/migration/V5__fk_additional.sql:19-21`
    - `invoice.patient_id`: `backend/src/main/resources/db/migration/V5__fk_additional.sql:23-25`
  - Intentional non-FK audit actor linkage documented:
    - `backend/src/main/resources/db/migration/V4__foreign_keys.sql:3-5`
    - `backend/src/main/resources/db/migration/V5__fk_additional.sql:5-8`
    - Base schema column remains without FK by intent: `backend/src/main/resources/db/migration/V1__init.sql:386-390`
- Impact after fix:
  - Core business integrity constraints previously cited are now present.
  - Remaining non-FK is compliance-preserving by design (audit records survivable after user deletion), not an omission.
- Minimum action remaining:
  - None required unless policy changes to force universal FK coverage.

---

## 11) Low
### Test/docs consistency note around encryption key strictness
- Severity: Low
- Previous conclusion: Partial
- Recheck conclusion: **Fixed**
- Evidence:
  - README clarifies strictness + test-only exceptions:
    - `README.md:121-125`
    - `README.md:123`
  - Test script fallback explicitly marked test-only:
    - `run_tests.sh:5-8`
- Note: Prior citation `README.md:18-21` did not support encryption-key strictness and has been removed. Prior citation `README.md:138-146` was incorrect (README has 133 lines); removed and replaced with the actual supporting line `README.md:123` (`Set ENCRYPTION_KEY securely outside local demo contexts`).
- Impact after fix:
  - Prior ambiguity between runtime strictness and test harness behavior is resolved.
- Minimum action remaining:
  - None for this issue.

---

## Final Recheck Judgment
- **All 11 submitted issues: closed**
- Static boundary note:
  - This report confirms code/config alignment by static evidence only.
  - Runtime behavior remains **Manual Verification Required** where execution is inherently necessary.
