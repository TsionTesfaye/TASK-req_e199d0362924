# Questions

This file records only prompt items that need interpretation because they are unclear, incomplete, or materially ambiguous.

## 1. Deployment scope on the internal network

**Question:**
Does “entirely on an internal network” mean a single shared LAN-hosted system for many users, or multiple isolated workstation installs?

**My understanding:**
This should be one shared application stack hosted inside the organization’s internal network, with multiple browser clients connecting over HTTPS/TLS.

**Solution:**
Use one shared deployment:

* Vue.js frontend served internally
* Spring Boot backend over internal HTTPS/TLS
* one shared MySQL database
* one shared local/on-prem file store for attachments and exports

## 2. Role separation for patient intake vs. clinical editing

**Question:**
Can Front Desk edit clinical or billing details after registration, or are they limited to demographics, contact, and ID verification fields?

**My understanding:**
Front Desk should be limited to intake-oriented fields and should not edit clinician-authored clinical summaries or finalized billing content.

**Solution:**
Enforce:

* Front Desk: registration, demographics, contact details, ID metadata capture
* Clinician: encounter/visit summary and medical updates
* Billing Specialist: bill/settlement actions
* Quality Reviewer: overrides, second-look review, anomaly resolutions

## 3. Patient identity model

**Question:**
Is a “visit-patient” always an existing patient record reused across visits, or can a visit begin as a one-off registration and later merge into a persistent patient profile?

**My understanding:**
The system should support persistent patient records reused across visits, while allowing intake to begin from a new registration if no prior patient is found.

**Solution:**
Model:

* Patient
* Visit
* optional duplicate/merge workflow for patient records
* visit always linked to one canonical patient record

## 4. ID verification storage boundaries

**Question:**
The prompt says to record document type and last four digits while masking full identifiers. Should the system store only the last four and type, or also store the full identifier encrypted?

**My understanding:**
The safest baseline is to store only:

* document type
* last four digits
* verification timestamp / verifier
  and avoid storing the full identifier unless explicitly required.

**Solution:**
Persist only minimal ID verification metadata by default:

* `document_type`
* `document_last4`
* `verified_by`
* `verified_at`
* optional note
  Do not store full document number unless a later requirement explicitly demands it.

## 5. Historical appointment and archived visit semantics

**Question:**
What makes a visit or appointment “archived” for badge display?

**My understanding:**
Older records should remain searchable, but archived status should be a lifecycle/data-retention state rather than just “old by date.”

**Solution:**
Use explicit archival fields:

* `archived_at`
* `archived_reason`
  and show archived badges based on record state, not simple age alone.

## 6. Incident reporter identity model

**Question:**
Can incident reports be submitted anonymously, or must every report be tied to a known user, call-center operator, or community contact?

**My understanding:**
Reports should support both:

* staff-submitted identified reports
* community-submitted reports with optional anonymous/minimal identity

**Solution:**
Model incident submitter as one of:

* internal user
* named external reporter
* anonymous / withheld
  Audit who entered the report into the system separately from who originated it.

## 7. Approximate location privacy rules

**Question:**
How should “hide exact locations for minors or protected cases” work when approximate location descriptions and map displays are involved?

**My understanding:**
Protected incidents should still be operationally useful, but exact coordinates or precise geocoding must be hidden from unauthorized users.

**Solution:**
Store:

* approximate text location
* optional internal precise map reference
  Enforce permission-based disclosure:
* protected users see only coarse neighborhood/cross-street data
* authorized roles may see the exact mapped point where permitted

## 8. Map layer data source scope

**Question:**
What does “locally hosted map layer” imply for base maps, shelters, and routing?

**My understanding:**
The system should not depend on public map APIs at runtime and should use a locally hosted/internal map dataset and resource directory.

**Solution:**
Assume:

* local/internal tile or map layer
* local shelter/resource dataset
* local route sheet generation using internal routing logic/data where available
  Do not assume external Google/Mapbox/OpenStreetMap live calls.

## 9. Route sheet generation detail

**Question:**
How exact must “printable, turn-by-turn route sheet” be if exact GPS routing is unavailable or approximate incident locations are protected?

**My understanding:**
The route sheet should use the best available internal route generation and degrade gracefully when only approximate location data is available.

**Solution:**
Generate:

* nearest resource
* turn list where internal routing exists
* otherwise fallback to structured directions summary
  Mark approximate/protected routes clearly where exact pathing is intentionally hidden.

## 10. Content discovery domain boundaries

**Question:**
Does full-text search and ranking apply only to incidents, only to bulletins, or to both in a shared discovery surface?

**My understanding:**
Search should span both incidents and bulletin content, while allowing filters or tabs to narrow by content type.

**Solution:**
Implement unified indexed search over:

* incidents
* bulletins
  with filters for:
* content type
* time
* popularity
* favorites
* comments

## 11. Ranked list weighting behavior

**Question:**
How should configurable weighting for cold-start ranking behave?

**My understanding:**
Ranking should combine engagement and freshness, with configurable defaults that prevent zero-engagement items from disappearing completely.

**Solution:**
Use a weighted ranking formula with admin-configurable inputs such as:

* recency
* favorites
* comments
* moderator/reviewer boost
* cold-start baseline weight

## 12. Bill generation timing

**Question:**
What exactly counts as “visit close” for auto-generating bills?

**My understanding:**
A visit should generate a bill when the encounter is formally closed by the clinician or an authorized workflow step, not merely when intake begins.

**Solution:**
Define explicit visit states and generate bill on:

* clinician-completed + closed visit
  or equivalent final encounter-close state.

## 13. Service/package rule precedence

**Question:**
How are service rules, package rules, discounts, and tax applied when multiple pricing rules overlap?

**My understanding:**
Billing must use a deterministic calculation order.

**Solution:**
Apply in this order:

1. base services/packages
2. package adjustments/bundles
3. discounts
4. discount cap enforcement ($200 max)
5. taxable subtotal and tax
6. tender reconciliation

## 14. Discount cap semantics

**Question:**
Does the “capped at $200.00 per invoice” rule apply to each individual discount or to the total combined discount amount?

**My understanding:**
The safer interpretation is total discounts on one invoice cannot exceed $200.00.

**Solution:**
Enforce:

* aggregate discount cap of $200.00 per invoice
* reject or trim configurations that exceed that cap

## 15. Mixed tender settlement model

**Question:**
How should mixed tender statuses behave when part of a balance is cash/check and part is “card-on-file recorded as external”?

**My understanding:**
Each tender component should be tracked independently while the invoice maintains a reconciled overall settlement status.

**Solution:**
Model:

* invoice
* payment allocations
* tender type per allocation
* external-reference status for card-on-file
  Do not collapse mixed tender into one opaque payment record.

## 16. Refund vs. void boundary

**Question:**
How should the system distinguish between refund and void when an invoice or payment is adjusted near daily close?

**My understanding:**
Void is a pre-close cancellation of a bill/payment state; refund is a post-settlement reversal within the allowed 30-day period.

**Solution:**
Enforce:

* void allowed only before daily close at 11:00 PM
* refund allowed only within 30 days
* each action has separate rules, audit trail, and permission checks

## 17. Daily close authority

**Question:**
Who performs daily close, and is it automatic at 11:00 PM or a controlled settlement action by authorized staff?

**My understanding:**
Daily close should be an explicit, auditable process performed by authorized finance/billing roles, with 11:00 PM acting as a policy boundary.

**Solution:**
Implement:

* daily close operation
* authorized actor requirement
* close timestamp
* closed-day lock behavior for void rules

## 18. Quality override scope

**Question:**
Which blocking QC issues can be overridden by a Quality Reviewer, and are all overrides equivalent?

**My understanding:**
Blocking and anomalous items may be overridden only by a Quality Reviewer, but the system should distinguish override reasons by category.

**Solution:**
Require override record with:

* reviewer
* reason category
* note
* timestamp
* linked blocked rule/anomaly

## 19. Duplicate detection fingerprint scope

**Question:**
What fields participate in deterministic duplicate detection for patients, visits, incidents, and billing artifacts?

**My understanding:**
Different record types need different fingerprints rather than one shared dedupe rule.

**Solution:**
Define deterministic fingerprints separately for:

* patient identity candidates
* incident report duplicates
* visit duplicates
* invoice/export idempotency keys

## 20. Sampling review assignment

**Question:**
How is the 5% second-look sample selected?

**My understanding:**
Sampling should be deterministic and auditable, not manual or random in an opaque way.

**Solution:**
Use reproducible sampling logic based on:

* closed visit population
* configurable percentage (default 5%)
* audit-stored selection reason

## 21. Corrective action closure model

**Question:**
What states should corrective actions follow after a second-look review finds an issue?

**My understanding:**
Corrective actions need a formal lifecycle, not just an open text note.

**Solution:**
Use states such as:

* open
* assigned
* in_progress
* resolved
* verified_closed

## 22. Rate limiting identity source

**Question:**
For “10 login attempts per 10 minutes per workstation,” what should count as a workstation identifier on an internal web app?

**My understanding:**
Rate limiting should use a server-observed workstation or client device identifier, not only username.

**Solution:**
Track rate limits using:

* IP
* workstation/device fingerprint if available
* username combination
  Log lockout/risk events with workstation context.

## 23. Allowlist / denylist scope

**Question:**
Do allowlist/denylist controls apply to users, workstations, IP ranges, exported targets, or all of them?

**My understanding:**
At minimum, they should apply to:

* user accounts
* workstations/IP sources
* export-sensitive actions where appropriate

**Solution:**
Design allowlist/denylist as a reusable policy layer supporting:

* login restrictions
* action restrictions
* export attempt restrictions

## 24. Risk scoring response model

**Question:**
What actions should the system take when anomalous login/export risk scores are high?

**My understanding:**
Risk scoring should do more than log; it should influence authentication or approval behavior.

**Solution:**
Support configurable responses:

* warn only
* require secondary confirmation
* temporary block
* force reviewer/admin follow-up

## 25. Immutable audit log scope

**Question:**
Does “all create/update/approve/export actions” mean those are the minimum audited actions, or should reveal/access/override events also be logged?

**My understanding:**
Those are the minimum required actions, but sensitive reveals and override actions should also be logged.

**Solution:**
Audit at minimum:

* create
* update
* approve
* export
  and also:
* reveal sensitive fields
* overrides
* login anomalies
* soft deletes
* restore operations

## 26. Idempotency key scope

**Question:**
Which endpoints require idempotency keys?

**My understanding:**
Any action that can create duplicate financial or operational records should require idempotency protection.

**Solution:**
Require idempotency keys for:

* visit registration submit
* incident creation submit
* invoice close/finalize
* payment/refund/void actions
* export job creation

## 27. Backup and restore coverage

**Question:**
Does monthly restore testing need to be represented only in documentation, or also inside the product as a tracked operation?

**My understanding:**
Because the prompt requires a documented restore test performed monthly, the system should track restore-test evidence as an auditable admin operation.

**Solution:**
Add restore-test log records with:

* operator
* date
* backup used
* result
* note

## 28. Seven-year archival semantics

**Question:**
What exactly happens when inactive patient records hit the 7-year threshold?

**My understanding:**
They should be archived and hidden from routine workflows, not blindly hard-deleted, unless policy explicitly allows deletion later.

**Solution:**
Implement:

* inactive/archive state
* archived_at
* retention hold / legal hold support
* separate admin retrieval path for archived patient records
