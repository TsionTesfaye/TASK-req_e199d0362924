-- V5: Add FK constraints for core NOT-NULL relations not covered by V4.
--
-- Intentional non-FK decisions (documented here for auditability):
--
--   audit_log.actor_user_id      — no FK by design: audit rows must survive user
--                                  deletion to preserve the compliance trail; the actor
--                                  identity is captured in actor_username_snapshot at
--                                  write time and is therefore durable without the FK.
--
--   appointment.clinician_user_id — nullable; clinician assignment is optional and
--                                   may reference a user who is later deactivated.
--
--   visit.appointment_id          — nullable; walk-in visits have no appointment.
--
--   quality_rule_result.*,
--   corrective_action.*           — loose analytical references; rows may outlive the
--                                   content they reference (e.g. archived visits).

ALTER TABLE appointment
    ADD CONSTRAINT fk_appointment_patient
        FOREIGN KEY (patient_id) REFERENCES patient (id);

ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_patient
        FOREIGN KEY (patient_id) REFERENCES patient (id);
