-- Certificate workflow migration: DRAFT -> SIGNED -> REVOKED
-- Compatible with PostgreSQL

ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS draft_pdf_s3_path VARCHAR(512);

ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS signed_pdf_s3_path VARCHAR(512);

ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS claim_code VARCHAR(255);

ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS claim_code_expires_at TIMESTAMP;

-- Backfill status from legacy data:
-- signed_pdf_hash exists => SIGNED, otherwise DRAFT
UPDATE certificates
SET status = CASE
                 WHEN signed_pdf_hash IS NOT NULL THEN
                     CASE WHEN COALESCE(is_valid, true) = false THEN 'REVOKED' ELSE 'SIGNED' END
                 ELSE 'DRAFT'
    END
WHERE status IS NULL;

-- Best effort backfill for old single-path storage
UPDATE certificates
SET signed_pdf_s3_path = pdf_s3_path
WHERE signed_pdf_s3_path IS NULL
  AND signed_pdf_hash IS NOT NULL;

UPDATE certificates
SET draft_pdf_s3_path = pdf_s3_path
WHERE draft_pdf_s3_path IS NULL
  AND signed_pdf_hash IS NULL;

ALTER TABLE certificates
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_certificate_status ON certificates(status);
CREATE INDEX IF NOT EXISTS idx_certificate_claim_code ON certificates(claim_code);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND indexname = 'uk_certificates_claim_code'
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX uk_certificates_claim_code ON certificates(claim_code) WHERE claim_code IS NOT NULL';
    END IF;
END $$;
