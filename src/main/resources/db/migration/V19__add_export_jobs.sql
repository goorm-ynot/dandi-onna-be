-- Async export jobs for sales Excel downloads.

DO $$
BEGIN
    CREATE TYPE export_job_status AS ENUM ('QUEUED', 'PROCESSING', 'DONE', 'FAILED', 'EXPIRED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    request_hash VARCHAR(64) NOT NULL,
    status export_job_status NOT NULL DEFAULT 'QUEUED',
    report_type TEXT NOT NULL DEFAULT 'OWNER_SALES',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    include_detail BOOLEAN NOT NULL DEFAULT TRUE,
    row_count INTEGER,
    file_key TEXT,
    file_expires_at TIMESTAMPTZ,
    error_message TEXT,
    format_version SMALLINT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT export_jobs_date_range CHECK (start_date <= end_date)
);

CREATE UNIQUE INDEX uniq_export_jobs_request_hash_active ON export_jobs(request_hash) WHERE active;
CREATE INDEX idx_export_jobs_store_id ON export_jobs(store_id);
CREATE INDEX idx_export_jobs_requested_by ON export_jobs(requested_by);
CREATE INDEX idx_export_jobs_status ON export_jobs(status);
CREATE INDEX idx_export_jobs_created_at ON export_jobs(created_at);
