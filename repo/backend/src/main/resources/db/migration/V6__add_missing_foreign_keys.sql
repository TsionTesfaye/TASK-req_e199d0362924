-- V6: Add FK constraints for core relationships not covered by V4 or V5.
--
-- V4 covered: visit.patient_id, invoice.visit_id
-- V5 covered: appointment.patient_id, invoice.patient_id
--
-- Assumption: database contains no orphaned rows for the columns below.
-- If orphaned rows exist, clean them before applying this migration:
--   DELETE FROM visit_charge WHERE visit_id NOT IN (SELECT id FROM visit);
--   DELETE FROM payment        WHERE invoice_id NOT IN (SELECT id FROM invoice);
--   DELETE FROM user_session   WHERE user_id NOT IN (SELECT id FROM app_user);
--
-- Intentional non-FK columns (unchanged):
--   audit_log.actor_user_id — no FK by design (see V5 rationale)

ALTER TABLE visit_charge
    ADD CONSTRAINT fk_visit_charge_visit
        FOREIGN KEY (visit_id) REFERENCES visit (id);

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoice (id);

ALTER TABLE user_session
    ADD CONSTRAINT fk_user_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id);
