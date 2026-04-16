# Revalidation Audit (All Previously Reported Issues) — Static-Only — 2026-04-15

## Scope
- Method: static code review only.
- Constraints respected: no project startup, no Docker, no test execution, no code modification.
- Objective: re-check all previously reported 9 issues from scratch and provide updated status.

## Overall Result
- Total revalidated issues: 9
- Fixed: 9
- Partially Fixed: 0
- Not Fixed: 0

---

## Issue 1
- Severity: Blocker
- Title: Frontend-backend contract mismatch breaks core visit open flow
- Previous status: Fail
- Current status: **Fixed**
- Evidence:
  - `frontend/src/features/visits/VisitListView.vue:134`
  - `frontend/src/features/visits/VisitListView.vue:173`
  - `backend/src/main/java/com/rescuehub/controller/VisitController.java:25`
  - `backend/src/main/resources/application.yml:20-21`
- Revalidation note: UI sends `chiefComplaint`; backend expects `chiefComplaint`; unknown fields still rejected.

## Issue 2
- Severity: Blocker
- Title: Incident moderation UI payload does not match API contract
- Previous status: Fail
- Current status: **Fixed**
- Evidence:
  - `frontend/src/features/incidents/IncidentDetailView.vue:346`
  - `frontend/src/features/incidents/IncidentDetailView.vue:352-353`
  - `backend/src/main/java/com/rescuehub/controller/IncidentController.java:42`
  - `backend/src/main/java/com/rescuehub/controller/IncidentController.java:88-93`
  - `backend/src/main/resources/application.yml:20-21`
- Revalidation note: frontend now maps action->status and posts `{status}` as backend requires.

## Issue 3
- Severity: High
- Title: Cross-tenant integrity risk in favorite/comment/view counter updates
- Previous status: Fail
- Current status: **Fixed**
- Evidence:
  - `backend/src/main/java/com/rescuehub/service/FavoriteCommentService.java:49-56`
  - `backend/src/main/java/com/rescuehub/service/FavoriteCommentService.java:61-68`
  - `backend/src/main/java/com/rescuehub/service/FavoriteCommentService.java:73-80`
  - `backend/src/main/java/com/rescuehub/repository/IncidentReportRepository.java:25-34`
  - `backend/src/main/java/com/rescuehub/repository/BulletinRepository.java:25-34`
- Revalidation note: object ownership checks and org-scoped update predicates are both present.

## Issue 4
- Severity: High
- Title: Admin user APIs expose password hashes in responses
- Previous status: Fail
- Current status: **Fixed**
- Evidence:
  - `backend/src/main/java/com/rescuehub/entity/User.java:33-34`
  - `backend/src/main/java/com/rescuehub/controller/AdminController.java:30-57`
- Revalidation note: `@JsonIgnore` is applied on password hash getter, preventing serialization leakage from User-returning admin endpoints.

## Issue 5
- Severity: High
- Title: Required package billing rules are defined but not implemented in invoice computation
- Previous status: Fail
- Current status: **Fixed**
- Evidence:
  - `backend/src/main/java/com/rescuehub/enums/BillingRuleType.java:4`
  - `backend/src/main/java/com/rescuehub/service/BillingService.java:93-110`
  - `backend/src/main/java/com/rescuehub/service/BillingService.java:372-379`
- Revalidation note: invoice generation now includes package-rule application stage.

## Issue 6
- Severity: High
- Title: Patient detail “visits for this patient” query is not enforced server-side
- Previous status: Fail
- Current status: **Fixed**
- Evidence:
  - `frontend/src/features/patients/PatientDetailView.vue:215`
  - `backend/src/main/java/com/rescuehub/controller/VisitController.java:39-43`
  - `backend/src/main/java/com/rescuehub/service/VisitService.java:171-176`
  - `backend/src/main/java/com/rescuehub/repository/VisitRepository.java:14`
- Revalidation note: backend now applies patient filter (`orgId + patientId`) when query parameter is supplied.

## Issue 7
- Severity: Medium
- Title: FK hardening/documentation drift leaves key relationships unenforced
- Previous status: Partial Fail
- Current status: **Fixed**
- Evidence:
  - `backend/src/main/resources/db/migration/V1__init.sql:4-6`
  - `backend/src/main/resources/db/migration/V5__fk_additional.sql:19-25`
  - `backend/src/main/resources/db/migration/V6__add_missing_foreign_keys.sql:15-25`
- Revalidation note: referenced FK migrations now exist and include previously missing relationships.

## Issue 8
- Severity: Medium
- Title: Void policy can be bypassed after business date if daily close not present
- Previous status: Partial Fail
- Current status: **Fixed**
- Evidence:
  - `backend/src/main/java/com/rescuehub/service/BillingService.java:249-259`
  - `backend/src/main/java/com/rescuehub/service/BillingService.java:260-262`
- Revalidation note: hard 11:00 PM business-date cutoff now enforced independently of daily-close row presence.

## Issue 9
- Severity: Medium
- Title: Quality/sampling/export frontend views contain contract-level degradation
- Previous status: Partial Fail
- Current status: **Fixed**
- Evidence:
  - Quality status rendering uses `row.status` in status slot:
    - `frontend/src/features/quality/QualityView.vue:25-29`
    - `frontend/src/features/quality/QualityView.vue:159-163`
  - Sampling run detail fetch by run id is implemented:
    - `frontend/src/features/quality/SamplingView.vue:128-129`
    - `backend/src/main/java/com/rescuehub/controller/SamplingController.java:40-45`
  - Export history endpoint now exists and frontend consumes it:
    - `backend/src/main/java/com/rescuehub/controller/ExportController.java:34-40`
    - `backend/src/main/java/com/rescuehub/service/ExportService.java:101-117`
    - `frontend/src/features/exports/ExportView.vue:146-152`
    - `frontend/src/features/exports/ExportView.vue:60`
- Revalidation note: all three previously degraded parts now have matching backend/frontend paths.

---

## Static Boundary Reminder
- This report confirms code-level remediation evidence only.
- Runtime correctness and operational behavior remain **Manual Verification Required** due static-only review constraints.
