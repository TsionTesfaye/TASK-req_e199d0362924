-- V4: Add referential integrity constraints for key relationships.
--
-- audit_log.actor_user_id is intentionally left without a FK constraint.
-- Audit logs must survive user deletion (soft-delete or hard-delete) to preserve
-- the compliance trail. The actor is captured via actorUsernameSnapshot at write time.

ALTER TABLE visit
    ADD CONSTRAINT fk_visit_patient
        FOREIGN KEY (patient_id) REFERENCES patient (id);

ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_visit
        FOREIGN KEY (visit_id) REFERENCES visit (id);
