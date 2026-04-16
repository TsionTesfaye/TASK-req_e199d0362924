# Static Audit Report 

Date: 2026-04-15  
Mode: Static-only audit (no runtime execution)

## 1. Verdict
- Overall conclusion: **Partial Pass**

## 2. Scope and Static Verification Boundary
- Reviewed: backend/frontend source, config, migrations, docs, static tests.
- Not reviewed by execution: runtime flows, browser interactions, Docker, networked integrations.
- Intentionally not executed: project startup, Docker, tests.
- Manual Verification Required: all runtime-dependent behavior.

## 3. Repository / Requirement Mapping Summary
- Prompt core goal: internal-network hub for patient encounters, community incidents, billing/settlement, quality controls, and strict security/audit.
- Core mapped domains in codebase: auth/RBAC, patient/visit/appointment, incidents/media/moderation, billing/refund/void/export, quality/sampling, backups/audit.
- Primary gaps in first inspection were concentrated in: authorization consistency, frontend/backend contract drift, object integrity, ranking signal wiring, and integrity controls.

## 4. Section-by-section Review

### 4.1 Hard Gates
#### 4.1.1 Documentation and static verifiability
- Conclusion: **Pass**
- Rationale: project includes docs/config/entrypoints sufficient for static traceability.
- Evidence: `README.md:1`, `backend/src/main/resources/application.yml:1`, `frontend/package.json:1`

#### 4.1.2 Material deviation from prompt
- Conclusion: **Partial Pass**
- Rationale: major prompt domains implemented, but several material defects broke or weakened required role/flow/security behavior.
- Evidence: Section 5 issues #1-#11.

### 4.2 Delivery Completeness
#### 4.2.1 Coverage of explicit core requirements
- Conclusion: **Partial Pass**
- Rationale: broad functional surface exists, but first-inspection defects materially affected required flows.
- Evidence: Section 5 (Blocker/High/Medium issues).

#### 4.2.2 End-to-end 0→1 deliverable shape
- Conclusion: **Pass**
- Rationale: full-stack structure present with backend/frontend/migrations/tests/docs.
- Evidence: repository structure and module layout.

### 4.3 Engineering and Architecture Quality
#### 4.3.1 Structure and decomposition
- Conclusion: **Pass**
- Rationale: clear service/controller/repository split and feature-grouped frontend.
- Evidence: `backend/src/main/java/com/rescuehub/*`, `frontend/src/features/*`

#### 4.3.2 Maintainability and extensibility
- Conclusion: **Partial Pass**
- Rationale: maintainable baseline, but contract/policy mismatches indicated extension risk in critical paths.
- Evidence: Section 5 issues #1, #2, #5, #7.

### 4.4 Engineering Details and Professionalism
#### 4.4.1 Error handling/logging/validation/API shape
- Conclusion: **Partial Pass**
- Rationale: validation/logging/audit patterns existed, but critical defects remained in role enforcement and contract correctness.
- Evidence: Section 5 issues.

#### 4.4.2 Product-like vs demo-like
- Conclusion: **Pass**
- Rationale: complete product-style layout and workflows exist despite material defects.

### 4.5 Prompt Understanding and Requirement Fit
#### 4.5.1 Business objective and constraints fit
- Conclusion: **Partial Pass**
- Rationale: intent understood, but some prompt-required controls/flows were missing or ineffective at first inspection.
- Evidence: allowlist/denylist gap, ranking signal gap, integrity gaps.

### 4.6 Aesthetics (frontend/full-stack)
#### 4.6.1 Visual and interaction quality
- Conclusion: **Pass**
- Rationale: static UI structure and feature separation appear production-oriented.
- Evidence: frontend feature views/components.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker
1. Severity: **Blocker**
- Title: Server-side authorization policy is inconsistent and under-enforced
- Conclusion: **Fail**
- Evidence: `SecurityConfig.java:73 (anyRequest().permitAll())`, missing role checks in `AppointmentService.java:27-67`, `VisitService.java:35-158`, `ShelterService.java:25-71`, `BulletinService.java:28-40`, `CorrectiveActionService.java:37-74`, `PatientService.java:89-176`
- Impact: authenticated users may perform out-of-role actions.
- Minimum actionable fix: define and enforce explicit endpoint/service role matrix + object-level guards.

2. Severity: **Blocker**
- Title: Frontend/backend contract mismatches break core operational flows
- Conclusion: **Fail**
- Evidence: numeric IDs handled as strings via `.slice(...)` (`IncidentListView.vue:130`, `BillingListView.vue:57`, `SamplingView.vue:78`); invoice/payment fields mismatch (`InvoiceDetailView.vue:9,50,71` vs `Invoice.java:23,55` and `Payment.java:15,29`); admin form sends unsupported fields (`AdminUsersView.vue:64-66,200-206` vs `AdminController.java:25-28`); unknown fields rejected (`application.yml:20-21`)
- Impact: billing/admin/list workflows can fail or misrender.
- Minimum actionable fix: strict shared DTO/schema contract and contract tests.

### High
3. Severity: **High**
- Title: Object-level integrity checks missing for visit/appointment creation
- Conclusion: **Fail**
- Evidence: `VisitService.open(...)` persisted raw patientId without existence/org validation (`VisitService.java:52-60`); similar in `AppointmentService.create(...)` (`AppointmentService.java:30-37`)
- Impact: orphan/cross-org references possible.
- Minimum actionable fix: resolve patient by organization + patientId before create.

4. Severity: **High**
- Title: Incident media upload exists but retrieval/display path is missing
- Conclusion: **Partial Fail**
- Evidence: upload endpoint present (`IncidentController.java:93-101`); no media collection on incident entity (`IncidentReport.java:9-80`); UI expected `incident.mediaFiles` (`IncidentDetailView.vue:52-55`)
- Impact: media attach flow not reliably visible in detail workflow.
- Minimum actionable fix: add media list endpoint or include mapped media DTO in incident detail.

5. Severity: **High**
- Title: Search/popularity/ranking signals not wired to interactions
- Conclusion: **Partial Fail**
- Evidence: sort uses `viewCount/favoriteCount/commentCount` (`SearchService.java:37-41`), but interaction service appended records without counter increments (`FavoriteCommentService.java:35-84`)
- Impact: popularity/favorites/comments ranking is misleading.
- Minimum actionable fix: transactionally maintain counters or compute aggregate signals.

6. Severity: **High**
- Title: Prompt-required allowlist/denylist controls not evidenced
- Conclusion: **Fail**
- Evidence: no allowlist/denylist implementation found during first inspection scans.
- Impact: explicit security requirement unmet.
- Minimum actionable fix: implement auditable allowlist/denylist controls at login/export-sensitive operations.

### Medium
7. Severity: **Medium**
- Title: UI filters call unsupported backend query parameters
- Conclusion: **Fail**
- Evidence: `PatientListView.vue:169-171` sends `q/archived` but backend list supported only page/size (`PatientController.java:66-73`); `AuditView.vue:78` sends `q` but `AuditController.java:24-31` ignored.
- Impact: misleading filters and incorrect user expectations.
- Minimum actionable fix: implement backend support or remove unsupported UI filters.

8. Severity: **Medium**
- Title: Backup restore-test endpoint ignored path variable and trusted body id
- Conclusion: **Partial Fail**
- Evidence: route `/api/backups/{id}/restore-test` (`BackupController.java:43`) but handler passed `req.backupRunId()` (`BackupController.java:48-49`).
- Impact: ambiguous target and audit trace inconsistency.
- Minimum actionable fix: enforce path/body consistency or path-only identity.

9. Severity: **Medium**
- Title: Nginx upload limit conflicted with backend media limit
- Conclusion: **Partial Fail**
- Evidence: backend 50MB (`MediaService.java:24`) vs nginx 20m (`frontend/nginx.conf:33`)
- Impact: valid uploads blocked at proxy layer.
- Minimum actionable fix: align proxy and backend limits.

10. Severity: **Medium**
- Title: Schema missing FK constraints for core relational integrity
- Conclusion: **Partial Fail**
- Evidence: relation-like columns without FK constraints in `V1__init.sql:100-507`.
- Impact: orphaned/invalid references possible if service checks regress.
- Minimum actionable fix: add critical FK constraints or document deliberate no-FK strategy with compensating controls.

### Low
11. Severity: **Low**
- Title: Test/docs consistency ambiguity around encryption key strictness
- Conclusion: **Partial**
- Evidence: README says no default key (`README.md:18-20`), `run_tests.sh` sets fallback test key (`run_tests.sh:8`)
- Impact: documentation clarity issue.
- Minimum actionable fix: document runtime strictness vs test-only fallback explicitly.

## 6. Security Review Summary
- Authentication entry points: **Partial Pass** (auth exists, but policy consistency issues present in first inspection)
- Route-level authorization: **Fail** (global permit-all in first-inspection evidence)
- Object-level authorization: **Fail** (visit/appointment integrity and scope checks missing)
- Function-level authorization: **Partial Pass** (inconsistent across critical services)
- Tenant/user isolation: **Partial Pass** (cross-org reference/counter risks)
- Admin/internal/debug protection: **Cannot Confirm Statistically** (no runtime boundary verification)

## 7. Tests and Logging Review
- Unit tests: **Pass** (present statically)
- API/integration tests: **Pass** (present statically)
- Logging categories/observability: **Partial Pass** (audit logs implemented, but coverage for all defect paths uneven)
- Sensitive-data leakage risk in logs/responses: **Partial Pass** (manual verification required for runtime log payloads)

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit tests and API tests exist statically.
- Test frameworks/entry points and commands are documented.
- Evidence: backend test tree, `tests/*`, `run_tests.sh`, `README.md`.

### 8.2 Coverage Mapping Table (Risk-Focused)
- Requirement/Risk: unauthenticated access, role checks, core billing/visit flows
  - Mapped tests: present statically in backend/api suites
  - Coverage: **basically covered**
  - Gap: object-level and contract mismatch regressions not fully asserted in first inspection
  - Minimum addition: explicit negative tests for 403/object-scope/DTO mismatch
- Requirement/Risk: popularity counters and ranking signal integrity
  - Mapped tests: insufficient in first inspection
  - Coverage: **insufficient**
  - Gap: no assertion that interaction events mutate ranking counters
  - Minimum addition: service/repository tests asserting counter deltas
- Requirement/Risk: allowlist/denylist enforcement
  - Mapped tests: missing in first inspection
  - Coverage: **missing**
  - Gap: no tests for denied logins/exports by access-control policy
  - Minimum addition: auth/export policy denial tests

### 8.3 Security Coverage Audit
- authentication: **basically covered**
- route authorization: **insufficient**
- object-level authorization: **insufficient**
- tenant/data isolation: **insufficient**
- admin/internal protection: **cannot confirm**

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Covered: baseline auth and major happy-path flows.
- Not covered enough: route/object authorization consistency, DTO contract mismatch detection, ranking-signal integrity, allowlist/denylist controls.

## 9. Final Notes
- This file intentionally preserves the first-inspection snapshot perspective.
- Static-only boundaries were respected; runtime correctness is manual verification territory.
