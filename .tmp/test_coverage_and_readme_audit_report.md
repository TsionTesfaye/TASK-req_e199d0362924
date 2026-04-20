# Test Coverage Audit

## Project Type Detection
- README top declaration: **fullstack** ([README.md](README.md):3).
- Structural inference confirms fullstack: `backend/`, `frontend/`, `tests/`, `e2e/`.

## Backend Endpoint Inventory
Total unique endpoints (`METHOD + PATH`): **83**

- GET /api/admin/access-control (backend/src/main/java/com/rescuehub/controller/AccessControlController.java:36)
- POST /api/admin/access-control (backend/src/main/java/com/rescuehub/controller/AccessControlController.java:43)
- DELETE /api/admin/access-control/:id (backend/src/main/java/com/rescuehub/controller/AccessControlController.java:52)
- GET /api/admin/users (backend/src/main/java/com/rescuehub/controller/AdminController.java:37)
- POST /api/admin/users (backend/src/main/java/com/rescuehub/controller/AdminController.java:30)
- DELETE /api/admin/users/:id (backend/src/main/java/com/rescuehub/controller/AdminController.java:59)
- GET /api/admin/users/:id (backend/src/main/java/com/rescuehub/controller/AdminController.java:45)
- PUT /api/admin/users/:id (backend/src/main/java/com/rescuehub/controller/AdminController.java:51)
- GET /api/appointments (backend/src/main/java/com/rescuehub/controller/AppointmentController.java:42)
- POST /api/appointments (backend/src/main/java/com/rescuehub/controller/AppointmentController.java:34)
- GET /api/appointments/:id (backend/src/main/java/com/rescuehub/controller/AppointmentController.java:67)
- PUT /api/appointments/:id/status (backend/src/main/java/com/rescuehub/controller/AppointmentController.java:73)
- GET /api/audit (backend/src/main/java/com/rescuehub/controller/AuditController.java:24)
- POST /api/auth/login (backend/src/main/java/com/rescuehub/controller/AuthController.java:27)
- POST /api/auth/logout (backend/src/main/java/com/rescuehub/controller/AuthController.java:41)
- GET /api/auth/me (backend/src/main/java/com/rescuehub/controller/AuthController.java:47)
- GET /api/backups (backend/src/main/java/com/rescuehub/controller/BackupController.java:29)
- POST /api/backups/:id/restore-test (backend/src/main/java/com/rescuehub/controller/BackupController.java:43)
- POST /api/backups/run (backend/src/main/java/com/rescuehub/controller/BackupController.java:37)
- POST /api/bootstrap (backend/src/main/java/com/rescuehub/controller/BootstrapController.java:34)
- GET /api/bootstrap/status (backend/src/main/java/com/rescuehub/controller/BootstrapController.java:22)
- GET /api/bulletins (backend/src/main/java/com/rescuehub/controller/BulletinController.java:34)
- POST /api/bulletins (backend/src/main/java/com/rescuehub/controller/BulletinController.java:27)
- GET /api/bulletins/:id (backend/src/main/java/com/rescuehub/controller/BulletinController.java:42)
- POST /api/bulletins/:id/status (backend/src/main/java/com/rescuehub/controller/BulletinController.java:48)
- GET /api/comments (backend/src/main/java/com/rescuehub/controller/CommentController.java:31)
- POST /api/comments (backend/src/main/java/com/rescuehub/controller/CommentController.java:25)
- GET /api/daily-close (backend/src/main/java/com/rescuehub/controller/DailyCloseController.java:34)
- POST /api/daily-close (backend/src/main/java/com/rescuehub/controller/DailyCloseController.java:27)
- POST /api/exports (backend/src/main/java/com/rescuehub/controller/ExportController.java:26)
- GET /api/exports/history (backend/src/main/java/com/rescuehub/controller/ExportController.java:34)
- DELETE /api/favorites (backend/src/main/java/com/rescuehub/controller/FavoriteController.java:31)
- GET /api/favorites (backend/src/main/java/com/rescuehub/controller/FavoriteController.java:38)
- POST /api/favorites (backend/src/main/java/com/rescuehub/controller/FavoriteController.java:25)
- GET /api/health (backend/src/main/java/com/rescuehub/controller/HealthController.java:11)
- GET /api/incidents (backend/src/main/java/com/rescuehub/controller/IncidentController.java:54)
- POST /api/incidents (backend/src/main/java/com/rescuehub/controller/IncidentController.java:44)
- GET /api/incidents/:id (backend/src/main/java/com/rescuehub/controller/IncidentController.java:82)
- GET /api/incidents/:id/media (backend/src/main/java/com/rescuehub/controller/IncidentController.java:98)
- POST /api/incidents/:id/media (backend/src/main/java/com/rescuehub/controller/IncidentController.java:107)
- POST /api/incidents/:id/moderate (backend/src/main/java/com/rescuehub/controller/IncidentController.java:88)
- POST /api/incidents/:id/reclassify (backend/src/main/java/com/rescuehub/controller/IncidentController.java:65)
- GET /api/incidents/:id/reveal-location (backend/src/main/java/com/rescuehub/controller/IncidentController.java:74)
- GET /api/invoices (backend/src/main/java/com/rescuehub/controller/BillingController.java:28)
- GET /api/invoices/:id (backend/src/main/java/com/rescuehub/controller/BillingController.java:36)
- GET /api/invoices/:id/payments (backend/src/main/java/com/rescuehub/controller/BillingController.java:42)
- POST /api/invoices/:id/payments (backend/src/main/java/com/rescuehub/controller/BillingController.java:49)
- POST /api/invoices/:id/refunds (backend/src/main/java/com/rescuehub/controller/BillingController.java:66)
- POST /api/invoices/:id/void (backend/src/main/java/com/rescuehub/controller/BillingController.java:57)
- GET /api/patients (backend/src/main/java/com/rescuehub/controller/PatientController.java:66)
- POST /api/patients (backend/src/main/java/com/rescuehub/controller/PatientController.java:57)
- GET /api/patients/:id (backend/src/main/java/com/rescuehub/controller/PatientController.java:77)
- DELETE /api/patients/:id/archive (backend/src/main/java/com/rescuehub/controller/PatientController.java:83)
- GET /api/patients/:id/reveal (backend/src/main/java/com/rescuehub/controller/PatientController.java:90)
- GET /api/patients/:id/verifications (backend/src/main/java/com/rescuehub/controller/PatientController.java:109)
- POST /api/patients/:id/verify-identity (backend/src/main/java/com/rescuehub/controller/PatientController.java:99)
- GET /api/quality/corrective-actions (backend/src/main/java/com/rescuehub/controller/QualityController.java:61)
- POST /api/quality/corrective-actions (backend/src/main/java/com/rescuehub/controller/QualityController.java:54)
- GET /api/quality/corrective-actions/:id (backend/src/main/java/com/rescuehub/controller/QualityController.java:69)
- PUT /api/quality/corrective-actions/:id (backend/src/main/java/com/rescuehub/controller/QualityController.java:75)
- GET /api/quality/results (backend/src/main/java/com/rescuehub/controller/QualityController.java:32)
- GET /api/quality/results/:id (backend/src/main/java/com/rescuehub/controller/QualityController.java:40)
- POST /api/quality/results/:id/override (backend/src/main/java/com/rescuehub/controller/QualityController.java:46)
- GET /api/ranking (backend/src/main/java/com/rescuehub/controller/RankingController.java:68)
- POST /api/ranking/promote (backend/src/main/java/com/rescuehub/controller/RankingController.java:58)
- GET /api/ranking/weights (backend/src/main/java/com/rescuehub/controller/RankingController.java:33)
- PUT /api/ranking/weights (backend/src/main/java/com/rescuehub/controller/RankingController.java:45)
- POST /api/retention/archive-run (backend/src/main/java/com/rescuehub/controller/RetentionController.java:23)
- GET /api/risk-scores (backend/src/main/java/com/rescuehub/controller/RiskScoreController.java:30)
- POST /api/routesheets (backend/src/main/java/com/rescuehub/controller/RouteSheetController.java:24)
- GET /api/sampling/runs (backend/src/main/java/com/rescuehub/controller/SamplingController.java:32)
- POST /api/sampling/runs (backend/src/main/java/com/rescuehub/controller/SamplingController.java:24)
- GET /api/sampling/runs/:id/visits (backend/src/main/java/com/rescuehub/controller/SamplingController.java:40)
- GET /api/search (backend/src/main/java/com/rescuehub/controller/SearchController.java:20)
- GET /api/shelters (backend/src/main/java/com/rescuehub/controller/ShelterController.java:38)
- POST /api/shelters (backend/src/main/java/com/rescuehub/controller/ShelterController.java:30)
- GET /api/shelters/:id (backend/src/main/java/com/rescuehub/controller/ShelterController.java:46)
- PUT /api/shelters/:id (backend/src/main/java/com/rescuehub/controller/ShelterController.java:52)
- GET /api/visits (backend/src/main/java/com/rescuehub/controller/VisitController.java:38)
- POST /api/visits (backend/src/main/java/com/rescuehub/controller/VisitController.java:28)
- GET /api/visits/:id (backend/src/main/java/com/rescuehub/controller/VisitController.java:47)
- PUT /api/visits/:id (backend/src/main/java/com/rescuehub/controller/VisitController.java:53)
- POST /api/visits/:id/close (backend/src/main/java/com/rescuehub/controller/VisitController.java:61)

## API Test Mapping Table
| Endpoint | Covered | Test Type | Test Files | Evidence |
|---|---|---|---|---|
| GET /api/admin/access-control | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AccessControlController.java:36; tests/api.test.js:715 (test: GET /admin/access-control returns list) |
| POST /api/admin/access-control | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AccessControlController.java:43; tests/api.test.js:721 (test: POST /admin/access-control creates an entry) |
| DELETE /api/admin/access-control/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AccessControlController.java:52; tests/api.test.js:738 (test: DELETE /admin/access-control/{id} removes the entry) |
| GET /api/admin/users | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AdminController.java:37; tests/api.test.js:403 (test: deleted user no longer appears in user list) |
| POST /api/admin/users | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AdminController.java:30; tests/api.test.js:116 (test: admin can log in after bootstrap and response includes user+csrf) |
| DELETE /api/admin/users/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AdminController.java:59; tests/api.test.js:387 (test: non-admin cannot delete a user) |
| GET /api/admin/users/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AdminController.java:45; tests/api.test.js:690 (test: admin GET /admin/users/{id} returns a single user) |
| PUT /api/admin/users/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AdminController.java:51; tests/api.test.js:697 (test: admin PUT /admin/users/{id} updates a user's displayName) |
| GET /api/appointments | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AppointmentController.java:42; tests/api.test.js:836 (test: GET /appointments returns a paginated list) |
| POST /api/appointments | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AppointmentController.java:34; tests/api.test.js:821 (test: POST /appointments creates a new appointment (FRONT_DESK)) |
| GET /api/appointments/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AppointmentController.java:67; tests/api.test.js:849 (test: GET /appointments/{id} returns a single appointment) |
| PUT /api/appointments/:id/status | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AppointmentController.java:73; tests/api.test.js:857 (test: PUT /appointments/{id}/status?status=CONFIRMED updates status via query param) |
| GET /api/audit | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AuditController.java:24; tests/api.test.js:178 (test: patient reveal writes audit and returns decrypted fields) |
| POST /api/auth/login | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AuthController.java:27; tests/api.test.js:51 (test: login rejected before bootstrap) |
| POST /api/auth/logout | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AuthController.java:41; tests/api.test.js:665 (test: POST /logout invalidates a temporary session) |
| GET /api/auth/me | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/AuthController.java:47; tests/api.test.js:646 (test: GET /me returns current user for authenticated session) |
| GET /api/backups | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BackupController.java:29; tests/api.test.js:1346 (test: GET /backups returns paginated list of backup runs (ADMIN role)) |
| POST /api/backups/:id/restore-test | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BackupController.java:43; tests/api.test.js:345 (test: restore-test missing result is rejected with 400) |
| POST /api/backups/run | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BackupController.java:37; tests/api.test.js:338 (test: admin can trigger a backup run) |
| POST /api/bootstrap | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BootstrapController.java:34; tests/api.test.js:57 (test: bootstrap creates admin and returns session + csrf tokens) |
| GET /api/bootstrap/status | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BootstrapController.java:22; tests/api.test.js:45 (test: bootstrap status returns initialized=false on fresh system) |
| GET /api/bulletins | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BulletinController.java:34; tests/api.test.js:1017 (test: GET /bulletins returns paginated list) |
| POST /api/bulletins | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BulletinController.java:27; tests/api.test.js:1007 (test: POST /bulletins creates a bulletin (MODERATOR role)) |
| GET /api/bulletins/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BulletinController.java:42; tests/api.test.js:1024 (test: GET /bulletins/{id} returns a single bulletin) |
| POST /api/bulletins/:id/status | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BulletinController.java:48; tests/api.test.js:1032 (test: POST /bulletins/{id}/status publishes the bulletin (MODERATOR role)) |
| GET /api/comments | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/CommentController.java:31; tests/api.test.js:1110 (test: GET /comments returns comments for a given bulletin) |
| POST /api/comments | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/CommentController.java:25; tests/api.test.js:1100 (test: POST /comments creates a comment on a bulletin) |
| GET /api/daily-close | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/DailyCloseController.java:34; tests/api.test.js:492 (test: clinician cannot access daily close list (not BILLING/ADMIN)) |
| POST /api/daily-close | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/DailyCloseController.java:27; tests/api.test.js:315 (test: daily close missing businessDate is rejected with 400) |
| POST /api/exports | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/ExportController.java:26; tests/api.test.js:270 (test: export accepts canonical types (ledger/audit/patients)) |
| GET /api/exports/history | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/ExportController.java:34; tests/api.test.js:567 (test: GET /exports/history returns list of past exports for BILLING role) |
| DELETE /api/favorites | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/FavoriteController.java:31; tests/api.test.js:1149 (test: DELETE /favorites removes a favorite via query params) |
| GET /api/favorites | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/FavoriteController.java:38; tests/api.test.js:1133 (test: GET /favorites returns favorites list containing the bulletinId just added) |
| POST /api/favorites | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/FavoriteController.java:25; tests/api.test.js:1122 (test: POST /favorites favorites a bulletin) |
| GET /api/health | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/HealthController.java:11; tests/api.test.js:39 (test: health endpoint) |
| GET /api/incidents | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:54; tests/api.test.js:945 (test: GET /incidents returns paginated list) |
| POST /api/incidents | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:44; tests/api.test.js:194 (test: incident submit (for route-sheet test)) |
| GET /api/incidents/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:82; tests/api.test.js:953 (test: GET /incidents/{id} returns a single incident) |
| GET /api/incidents/:id/media | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:98; tests/api.test.js:984 (test: GET /incidents/{id}/media returns media list) |
| POST /api/incidents/:id/media | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:107; tests/api.test.js:992 (test: POST /incidents/{id}/media route exists (returns 4xx not 404/405)) |
| POST /api/incidents/:id/moderate | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:88; tests/api.test.js:592 (test: moderator can moderate an incident with { status } payload) |
| POST /api/incidents/:id/reclassify | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:65; tests/api.test.js:961 (test: POST /incidents/{id}/reclassify updates classification fields) |
| GET /api/incidents/:id/reveal-location | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/IncidentController.java:74; tests/api.test.js:975 (test: GET /incidents/{id}/reveal-location returns location or 404 (no exact location set)) |
| GET /api/invoices | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BillingController.java:28; tests/api.test.js:230 (test: billing list returns the generated invoice) |
| GET /api/invoices/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BillingController.java:36; tests/api.test.js:902 (test: GET /invoices/{id} returns a single invoice) |
| GET /api/invoices/:id/payments | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BillingController.java:42; tests/api.test.js:909 (test: GET /invoices/{id}/payments returns payment list) |
| POST /api/invoices/:id/payments | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BillingController.java:49; tests/api.test.js:238 (test: payment with tenderType succeeds) |
| POST /api/invoices/:id/refunds | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BillingController.java:66; tests/api.test.js:930 (test: POST /invoices/{id}/refunds records a refund (or 409/422 on business rule)) |
| POST /api/invoices/:id/void | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/BillingController.java:57; tests/api.test.js:918 (test: POST /invoices/{id}/void voids a fresh invoice (or 409/422 on business rule)) |
| GET /api/patients | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:66; tests/api.test.js:140 (test: unauthenticated request to protected endpoint rejected) |
| POST /api/patients | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:57; tests/api.test.js:77 (test: mutating request without CSRF token is rejected) |
| GET /api/patients/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:77; tests/api.test.js:549 (test: patient detail response contains DTO fields (medicalRecordNumber, dateOfBirth, phoneLast4)) |
| DELETE /api/patients/:id/archive | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:83; tests/api.test.js:776 (test: DELETE /patients/{id}/archive archives a NEW patient (not the shared patientId)) |
| GET /api/patients/:id/reveal | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:90; tests/api.test.js:175 (test: patient reveal writes audit and returns decrypted fields) |
| GET /api/patients/:id/verifications | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:109; tests/api.test.js:757 (test: GET /patients/{id}/verifications returns list of verifications) |
| POST /api/patients/:id/verify-identity | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/PatientController.java:99; tests/api.test.js:749 (test: POST /patients/{id}/verify-identity records a verification) |
| GET /api/quality/corrective-actions | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:61; tests/api.test.js:1217 (test: GET /quality/corrective-actions returns paginated list) |
| POST /api/quality/corrective-actions | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:54; tests/api.test.js:418 (test: quality user can create a corrective action) |
| GET /api/quality/corrective-actions/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:69; tests/api.test.js:1224 (test: GET /quality/corrective-actions/{id} returns the known corrective action) |
| PUT /api/quality/corrective-actions/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:75; tests/api.test.js:430 (test: CA transition missing status is rejected with 400) |
| GET /api/quality/results | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:32; tests/api.test.js:1189 (test: GET /quality/results returns paginated list (QUALITY role)) |
| GET /api/quality/results/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:40; tests/api.test.js:1200 (test: GET /quality/results/{id} returns result or skip if none exist) |
| POST /api/quality/results/:id/override | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/QualityController.java:46; tests/api.test.js:1207 (test: POST /quality/results/{id}/override applies override (or skip if no results)) |
| GET /api/ranking | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RankingController.java:68; tests/api.test.js:1310 (test: GET /ranking returns ranked content list with valid entry structure) |
| POST /api/ranking/promote | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RankingController.java:58; tests/api.test.js:1296 (test: POST /ranking/promote promotes a content item (MODERATOR)) |
| GET /api/ranking/weights | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RankingController.java:33; tests/api.test.js:1268 (test: GET /ranking/weights returns weight configuration with all required fields (MODERATOR)) |
| PUT /api/ranking/weights | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RankingController.java:45; tests/api.test.js:1281 (test: PUT /ranking/weights updates and persists new weights (MODERATOR)) |
| POST /api/retention/archive-run | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RetentionController.java:23; tests/api.test.js:1364 (test: POST /retention/archive-run returns 403 for non-admin (BILLING)) |
| GET /api/risk-scores | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RiskScoreController.java:30; tests/api.test.js:1326 (test: GET /risk-scores returns list (ADMIN role)) |
| POST /api/routesheets | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/RouteSheetController.java:24; tests/api.test.js:215 (test: route sheet auto-pick (resourceId=0) succeeds and includes turn-by-turn) |
| GET /api/sampling/runs | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/SamplingController.java:32; tests/api.test.js:1248 (test: GET /sampling/runs returns paginated list) |
| POST /api/sampling/runs | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/SamplingController.java:24; tests/api.test.js:1237 (test: POST /sampling/runs creates a sampling run (QUALITY role)) |
| GET /api/sampling/runs/:id/visits | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/SamplingController.java:40; tests/api.test.js:1258 (test: GET /sampling/runs/{id}/visits returns visits for the sampling run) |
| GET /api/search | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/SearchController.java:20; tests/api.test.js:1162 (test: GET /search returns results for a query with validated payload structure) |
| GET /api/shelters | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/ShelterController.java:38; tests/api.test.js:1062 (test: GET /shelters returns paginated list) |
| POST /api/shelters | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/ShelterController.java:30; tests/api.test.js:1045 (test: POST /shelters creates a shelter (ADMIN role)) |
| GET /api/shelters/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/ShelterController.java:46; tests/api.test.js:1070 (test: GET /shelters/{id} returns a single shelter) |
| PUT /api/shelters/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/ShelterController.java:52; tests/api.test.js:1079 (test: PUT /shelters/{id} updates shelter details) |
| GET /api/visits | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/VisitController.java:38; tests/api.test.js:631 (test: GET /visits with patientId filter returns only that patient's visits) |
| POST /api/visits | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/VisitController.java:28; tests/api.test.js:166 (test: clinician opens visit (with idempotency key)) |
| GET /api/visits/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/VisitController.java:47; tests/api.test.js:787 (test: GET /visits/{id} returns a single visit) |
| PUT /api/visits/:id | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/VisitController.java:53; tests/api.test.js:803 (test: PUT /visits/{id} updates summary and diagnosis text) |
| POST /api/visits/:id/close | yes | true no-mock HTTP | tests/api.test.js | backend/src/main/java/com/rescuehub/controller/VisitController.java:61; tests/api.test.js:185 (test: visit close idempotency returns same result) |

## API Test Classification
1. True No-Mock HTTP
- `tests/api.test.js` uses real HTTP (`fetch(BASE + path)`) to backend routes.
- Evidence: `tests/api.test.js:6-20` call helper; concrete endpoint calls throughout file.

2. HTTP with Mocking
- Frontend unit/component/router tests use MSW HTTP interception.
- Evidence: `frontend/src/tests/msw/server.js:1` (`setupServer`), per-test overrides in `frontend/src/tests/components.test.js` and `frontend/src/tests/router.test.js`.

3. Non-HTTP (unit/integration without HTTP)
- Backend service-centric tests call service methods directly (`*ServiceTest.java`, `HardeningTest.java`, `VisitCloseTest.java`).

## Mock Detection
- Backend API tests: no static evidence of `@MockBean`, `@Mock`, `Mockito.*`, `jest.mock`, `vi.mock`, `sinon.stub` in backend test files.
- Frontend tests: HTTP layer mocked with MSW.
  - What mocked: frontend HTTP responses.
  - Where: `frontend/src/tests/msw/handlers.js`, `frontend/src/tests/components.test.js`, `frontend/src/tests/router.test.js`.

## Coverage Summary
- Total endpoints: **83**
- Endpoints with HTTP tests: **83**
- Endpoints with TRUE no-mock tests: **83**
- HTTP coverage: **100.0%**
- True API coverage: **100.0%**

## Unit Test Summary
### Backend Unit Tests
- Test files: `backend/src/test/java/com/rescuehub/*.java`.
- Modules covered:
  - services: auth, bootstrap, patient, visit, billing, export, backup, retention, ranking, quality, search, access control, admin, incident, shelter, media, crypto.
  - auth/guards/middleware: `CsrfCoverageTest` validates CSRF behavior through HTTP; hardening tests validate role constraints.
  - repositories: covered indirectly through service/integration tests.
  - controllers: covered via HTTP tests, not isolated controller-unit tests.
- Important backend modules not directly unit-tested:
  - controller classes as isolated units (`backend/src/main/java/com/rescuehub/controller/*`).
  - repository interfaces as isolated units (`backend/src/main/java/com/rescuehub/repository/*`).

### Frontend Unit Tests (STRICT REQUIREMENT)
- Frontend test files (evidence):
  - `frontend/src/tests/components.test.js`
  - `frontend/src/tests/shared-components.test.js`
  - `frontend/src/tests/api.test.js`
  - `frontend/src/tests/stores.test.js`
  - `frontend/src/tests/router.test.js`
- Frameworks/tools detected:
  - Vitest (`frontend/package.json` scripts/devDependencies)
  - @vue/test-utils
  - MSW
- Components/modules covered:
  - feature views in `frontend/src/features/**` via dynamic imports in tests.
  - shared components in `frontend/src/components/**`.
  - stores/API/router modules: `stores/session.js`, `stores/toast.js`, `api/client.js`, `router/index.js`.
- Important frontend modules not tested:
  - No untested feature view files detected by static import mapping.
  - `frontend/src/main.js` (app bootstrap wiring not unit-targeted).

**Frontend unit tests: PRESENT**

### Cross-Layer Observation
- Backend HTTP/API coverage is comprehensive and frontend unit coverage is explicit.
- Added router guard unit tests and role-restriction E2E reduce prior imbalance risk.

## API Observability Check
- Endpoint, request input, and response content are explicit in `tests/api.test.js`.
- Evidence examples:
  - endpoint/method: `tests/api.test.js` `call("POST", "/patients", ...)`.
  - request payload assertions: payment/export/quality/validation matrix tests.
  - response contract assertions: dedicated section (`RESPONSE CONTRACT ASSERTIONS` block).
- Verdict: **strong** observability.

## Test Quality & Sufficiency
- Success paths: broad coverage across all domains.
- Failure cases: broad 400/403/409/422 checks.
- Edge cases: idempotency, pagination bounds, enum validation, malformed JSON, role restrictions.
- Auth/permissions: strong API and additional browser role checks (`e2e/tests/roles.spec.js`).
- Integration boundaries: no-mock API tests run through real HTTP layer.
- `run_tests.sh` check: Docker-based flow only (OK).

## End-to-End Expectations
- Fullstack FE↔BE E2E present (`e2e/tests/auth.spec.js`, `patients.spec.js`, `incidents.spec.js`, `admin.spec.js`, `roles.spec.js`).
- Coverage breadth is meaningful across auth, CRUD flows, and role access rules.

## Tests Check
- Static inspection only performed.
- No test/script execution was performed in this audit.

## Test Coverage Score (0-100)
**96/100**

## Score Rationale
- + 100% endpoint-to-HTTP-call mapping from controller inventory to API tests.
- + True no-mock API test path present for all endpoints.
- + Strong validation/contract/pagination/authorization assertions in API suite.
- + Frontend unit tests explicitly present and include router guard logic.
- + Dedicated role-based browser E2E coverage exists.
- - Remaining deductions: isolated controller/repository unit granularity is still mostly indirect; multipart media happy-path validation remains route-existence-oriented rather than full artifact assertions.

## Key Gaps
- Add strict schema assertions for **every** response body (including all list item shapes), not just selected representative endpoints.
- Add full happy-path artifact assertions for incident media upload/download lifecycle (currently route existence + status semantics dominate).
- Add targeted isolated tests for highest-risk repository query constraints (tenant scoping edge cases) to reduce reliance on indirect integration proof.

## Confidence & Assumptions
- Confidence: **0.92**.
- Assumptions:
  - Endpoint inventory is derived from Spring mapping annotations in controller files.
  - Coverage mapping is based on static `call(METHOD, PATH)` extraction from `tests/api.test.js`.

---

# README Audit

## README Location
- Found at required path: `repo/README.md`.

## Hard Gate Check
- Formatting/readability: **PASS**.
- Startup instructions include `docker-compose up`: **PASS**.
- Access method (URL + port): **PASS** (`https://localhost:15443` and API base).
- Verification method (API + Web): **PASS**.
- Environment rules (no runtime installs/manual DB setup): **PASS**.
- Demo credentials for auth with all roles: **PASS**.

## High Priority Issues
- None.

## Medium Priority Issues
- Credentials section still depends on bootstrap/user-creation workflow; consider adding an explicit “seed via test flow only” note to avoid operator confusion in non-test environments.

## Low Priority Issues
- Could add one concise sequence diagram for auth/token/CSRF exchange to accelerate onboarding.

## Hard Gate Failures
- None.

## README Verdict
**PASS**
