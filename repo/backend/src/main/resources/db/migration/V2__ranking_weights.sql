-- Persist per-organization ranking weights so they survive backend restarts.
-- One row per organization; organization_id is the natural PK.
CREATE TABLE org_ranking_weights (
    organization_id  BIGINT       NOT NULL PRIMARY KEY,
    recency          DOUBLE       NOT NULL DEFAULT 1.0,
    favorites        DOUBLE       NOT NULL DEFAULT 2.0,
    comments         DOUBLE       NOT NULL DEFAULT 1.5,
    moderator_boost  DOUBLE       NOT NULL DEFAULT 5.0,
    cold_start_base  DOUBLE       NOT NULL DEFAULT 0.5,
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);
