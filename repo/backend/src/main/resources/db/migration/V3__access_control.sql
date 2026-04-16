-- Per-organization allowlist and denylist entries.
-- Entries with organization_id NULL are global (apply across all orgs).
CREATE TABLE access_list_entry (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    organization_id   BIGINT       NULL,        -- NULL = global entry
    list_type         VARCHAR(10)  NOT NULL,    -- 'ALLOW' or 'DENY'
    subject_type      VARCHAR(20)  NOT NULL,    -- 'IP', 'USERNAME', 'WORKSTATION_ID'
    subject_value     VARCHAR(512) NOT NULL,
    reason            VARCHAR(512) NULL,
    created_by_user_id BIGINT      NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at        DATETIME(6)  NULL,        -- NULL = never expires
    INDEX idx_acl_lookup (organization_id, list_type, subject_type, subject_value),
    INDEX idx_acl_global (list_type, subject_type, subject_value)
);
