-- V1__init.sql — Full schema for Community Rescue & Clinic Billing Hub
-- NOTE: FK constraints (FOREIGN KEY / REFERENCES) are added in later migrations,
-- not here, so that V1 remains a clean base schema independent of table creation order.
--   V4__foreign_keys.sql  — visit.patient_id, invoice.visit_id
--   V5__fk_additional.sql — appointment.patient_id, invoice.patient_id
-- Intentional non-FK columns: audit_log.actor_user_id (see V5 for rationale).

CREATE TABLE organization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    is_frozen TINYINT(1) NOT NULL DEFAULT 0,
    password_changed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_user_org (organization_id),
    INDEX idx_user_username (username)
);

CREATE TABLE user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_token_hash VARCHAR(255) NOT NULL UNIQUE,
    csrf_token_hash VARCHAR(255),
    workstation_id VARCHAR(255),
    ip_address VARCHAR(255),
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6),
    revoked_at DATETIME(6),
    INDEX idx_session_user (user_id),
    INDEX idx_session_token (session_token_hash)
);

CREATE TABLE patient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    medical_record_number VARCHAR(255) NOT NULL,
    first_name_ciphertext VARBINARY(512),
    first_name_iv VARBINARY(512),
    last_name_ciphertext VARBINARY(512),
    last_name_iv VARBINARY(512),
    date_of_birth DATE NOT NULL,
    sex VARCHAR(50),
    phone_ciphertext VARBINARY(512),
    phone_iv VARBINARY(512),
    phone_last4 VARCHAR(4),
    address_ciphertext VARBINARY(512),
    address_iv VARBINARY(512),
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone_ciphertext VARBINARY(512),
    emergency_contact_phone_iv VARBINARY(512),
    is_minor TINYINT(1) NOT NULL DEFAULT 0,
    is_protected_case TINYINT(1) NOT NULL DEFAULT 0,
    archived_at DATETIME(6),
    legal_hold_until DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_patient_org (organization_id),
    INDEX idx_patient_mrn (medical_record_number)
);

CREATE TABLE patient_identity_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    verified_by_user_id BIGINT NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    document_last4 VARCHAR(4) NOT NULL,
    verified_at DATETIME(6) NOT NULL,
    note TEXT,
    INDEX idx_piv_patient (patient_id)
);

CREATE TABLE appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME,
    clinician_user_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'scheduled',
    archived_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_appt_org (organization_id),
    INDEX idx_appt_patient (patient_id),
    INDEX idx_appt_date (scheduled_date)
);

CREATE TABLE visit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    created_by_user_id BIGINT NOT NULL,
    clinician_user_id BIGINT,
    opened_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6),
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    chief_complaint TEXT,
    summary_text LONGTEXT,
    diagnosis_text TEXT,
    qc_blocked TINYINT(1) NOT NULL DEFAULT 0,
    qc_override_required TINYINT(1) NOT NULL DEFAULT 0,
    archived_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_visit_org (organization_id),
    INDEX idx_visit_patient (patient_id),
    INDEX idx_visit_status (status)
);

CREATE TABLE incident_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    submitted_by_user_id BIGINT,
    external_reporter_name VARCHAR(255),
    is_anonymous TINYINT(1) NOT NULL DEFAULT 0,
    subject_age_group VARCHAR(50),
    involves_minor TINYINT(1) NOT NULL DEFAULT 0,
    is_protected_case TINYINT(1) NOT NULL DEFAULT 0,
    category VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    approximate_location_text TEXT NOT NULL,
    neighborhood VARCHAR(255),
    nearest_cross_streets VARCHAR(255),
    exact_location_ciphertext VARBINARY(512),
    exact_location_iv VARBINARY(512),
    status VARCHAR(50) NOT NULL DEFAULT 'submitted',
    favorite_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_incident_org (organization_id),
    INDEX idx_incident_status (status)
);

CREATE TABLE incident_media_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_report_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    uploaded_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_media_incident (incident_report_id)
);

CREATE TABLE shelter_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(255),
    address_text TEXT NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_shelter_org (organization_id)
);

CREATE TABLE route_sheet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_report_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    generated_by_user_id BIGINT NOT NULL,
    route_summary_text LONGTEXT NOT NULL,
    printable_file_path VARCHAR(1024),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_route_incident (incident_report_id)
);

CREATE TABLE bulletin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'draft',
    created_by_user_id BIGINT NOT NULL,
    moderated_by_user_id BIGINT,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_bulletin_org (organization_id),
    INDEX idx_bulletin_status (status)
);

CREATE TABLE ranked_content_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content_id BIGINT NOT NULL,
    weighting_snapshot_json JSON,
    score DECIMAL(12,4) NOT NULL DEFAULT 0,
    promoted_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ranked_org (organization_id),
    INDEX idx_ranked_content (content_type, content_id)
);

CREATE TABLE visit_charge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    service_code VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    pricing_source_type VARCHAR(50) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    line_total DECIMAL(12,2) NOT NULL,
    taxable TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_charge_visit (visit_id)
);

CREATE TABLE invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT NOT NULL,
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    subtotal_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    outstanding_amount DECIMAL(12,2) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    daily_close_date DATE,
    voided_at DATETIME(6),
    refunded_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_invoice_org (organization_id),
    INDEX idx_invoice_visit (visit_id),
    INDEX idx_invoice_patient (patient_id)
);

CREATE TABLE daily_close (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    business_date DATE NOT NULL,
    closed_by_user_id BIGINT NOT NULL,
    closed_at DATETIME(6) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'closed',
    UNIQUE KEY uq_daily_close (organization_id, business_date),
    INDEX idx_dc_org (organization_id)
);

CREATE TABLE invoice_tender (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    tender_type VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    external_reference VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_tender_invoice (invoice_id)
);

CREATE TABLE billing_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2),
    percentage DECIMAL(5,2),
    tax_rate DECIMAL(5,2),
    package_definition_json JSON,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 100,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_billing_rule_org (organization_id),
    INDEX idx_billing_rule_code (code)
);

CREATE TABLE refund_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    approved_by_user_id BIGINT,
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    approved_at DATETIME(6),
    executed_at DATETIME(6),
    INDEX idx_refund_invoice (invoice_id)
);

CREATE TABLE ledger_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    invoice_id BIGINT,
    refund_request_id BIGINT,
    entry_type VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    before_json JSON,
    after_json JSON,
    INDEX idx_ledger_org (organization_id),
    INDEX idx_ledger_invoice (invoice_id)
);

CREATE TABLE quality_rule_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    visit_id BIGINT,
    patient_id BIGINT,
    incident_report_id BIGINT,
    rule_code VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    result_details_json JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6),
    INDEX idx_qrr_org (organization_id),
    INDEX idx_qrr_visit (visit_id)
);

CREATE TABLE quality_override (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quality_rule_result_id BIGINT NOT NULL,
    overridden_by_user_id BIGINT NOT NULL,
    override_reason_code VARCHAR(255) NOT NULL,
    override_note TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_qo_result (quality_rule_result_id)
);

CREATE TABLE corrective_action (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    related_visit_id BIGINT,
    related_rule_result_id BIGINT,
    assigned_to_user_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    description TEXT NOT NULL,
    resolution_note TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    closed_at DATETIME(6),
    INDEX idx_ca_org (organization_id)
);

CREATE TABLE duplicate_fingerprint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    fingerprint_type VARCHAR(255) NOT NULL,
    fingerprint_value VARCHAR(255) NOT NULL,
    object_type VARCHAR(255) NOT NULL,
    object_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_dup_org_type_val (organization_id, fingerprint_type, fingerprint_value)
);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT,
    actor_user_id BIGINT,
    actor_username_snapshot VARCHAR(255),
    action_code VARCHAR(255) NOT NULL,
    object_type VARCHAR(255) NOT NULL,
    object_id VARCHAR(255),
    ip_address VARCHAR(255),
    workstation_id VARCHAR(255),
    before_json JSON,
    after_json JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_org (organization_id),
    INDEX idx_audit_actor (actor_user_id),
    INDEX idx_audit_object (object_type, object_id)
);

CREATE TABLE backup_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    backup_type VARCHAR(50) NOT NULL,
    output_path VARCHAR(1024),
    retention_expires_at DATETIME(6),
    status VARCHAR(50) NOT NULL DEFAULT 'running',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6),
    INDEX idx_backup_org (organization_id)
);

CREATE TABLE restore_test_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    performed_by_user_id BIGINT NOT NULL,
    backup_run_id BIGINT NOT NULL,
    result VARCHAR(50) NOT NULL,
    note TEXT,
    performed_at DATETIME(6) NOT NULL,
    INDEX idx_restore_org (organization_id)
);

CREATE TABLE retention_policy_hold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    hold_reason TEXT NOT NULL,
    hold_until DATETIME(6),
    created_by_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_hold_patient (patient_id)
);

CREATE TABLE favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_favorite (organization_id, user_id, content_type, content_id),
    INDEX idx_fav_content (content_type, content_id)
);

CREATE TABLE content_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_comment_content (content_type, content_id)
);

CREATE TABLE view_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT,
    content_type VARCHAR(50) NOT NULL,
    content_id BIGINT NOT NULL,
    viewed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_view_content (content_type, content_id)
);

CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    tender_type VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    external_reference VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_payment_invoice (invoice_id),
    INDEX idx_payment_org (organization_id)
);

CREATE TABLE sampling_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    period VARCHAR(50) NOT NULL,
    seed VARCHAR(255) NOT NULL,
    percentage INT NOT NULL DEFAULT 5,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_sampling_org (organization_id)
);

CREATE TABLE sampled_visit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sampling_run_id BIGINT NOT NULL,
    visit_id BIGINT NOT NULL,
    selection_reason VARCHAR(255) NOT NULL,
    INDEX idx_sv_run (sampling_run_id),
    INDEX idx_sv_visit (visit_id)
);

CREATE TABLE idempotency_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    idm_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(255),
    response_snapshot_json TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6),
    UNIQUE KEY uq_idempotency (organization_id, idm_key),
    INDEX idx_idem_expires (expires_at)
);

-- Seed default organization
INSERT INTO organization (id, code, name, is_active, created_at, updated_at)
VALUES (1, 'DEFAULT', 'Default Org', 1, NOW(6), NOW(6));

-- Sample billing rules
INSERT INTO billing_rule (organization_id, rule_type, code, name, amount, percentage, tax_rate, is_active, priority, created_at, updated_at)
VALUES
(1, 'SERVICE', 'OFFICE_VISIT', 'Office Visit', 100.00, NULL, NULL, 1, 10, NOW(6), NOW(6)),
(1, 'SERVICE', 'LAB_PANEL', 'Laboratory Panel', 50.00, NULL, NULL, 1, 20, NOW(6), NOW(6)),
(1, 'SERVICE', 'MEDICATION', 'Medication', 20.00, NULL, NULL, 1, 30, NOW(6), NOW(6)),
(1, 'DISCOUNT', 'STANDARD_DISCOUNT', 'Standard 10% Discount', NULL, 10.00, NULL, 1, 50, NOW(6), NOW(6)),
(1, 'TAX', 'STATE_TAX', 'State Tax 8%', NULL, NULL, 8.00, 1, 90, NOW(6), NOW(6));

-- Sample shelters
INSERT INTO shelter_resource (organization_id, name, category, neighborhood, address_text, latitude, longitude, is_active, created_at, updated_at)
VALUES
(1, 'Main Street Shelter', 'emergency_shelter', 'Downtown', '100 Main St', 37.7749, -122.4194, 1, NOW(6), NOW(6)),
(1, 'Eastside Community Clinic', 'clinic', 'Eastside', '200 East Ave', 37.7849, -122.4094, 1, NOW(6), NOW(6)),
(1, 'Northgate Resource Center', 'resource_center', 'Northgate', '300 North Blvd', 37.7649, -122.4294, 1, NOW(6), NOW(6));
