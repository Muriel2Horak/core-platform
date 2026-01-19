-- Align outbox_final schema with streaming entity mappings.
ALTER TABLE outbox_final
  ADD COLUMN IF NOT EXISTS operation VARCHAR(20) NOT NULL DEFAULT 'CREATE',
  ADD COLUMN IF NOT EXISTS diff_json JSONB,
  ADD COLUMN IF NOT EXISTS snapshot_json JSONB,
  ADD COLUMN IF NOT EXISTS headers_json JSONB,
  ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS error_message TEXT;

CREATE INDEX IF NOT EXISTS idx_outbox_final_created_at ON outbox_final(created_at);
