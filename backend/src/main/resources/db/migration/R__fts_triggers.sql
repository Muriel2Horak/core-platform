-- =====================================================
-- REPEATABLE: FULLTEXT SEARCH TRIGGERS
-- Auto-update tsvector when document_index changes
-- =====================================================

-- Function to update search_vector from extracted_text
CREATE OR REPLACE FUNCTION update_document_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('simple', COALESCE(NEW.extracted_text, ''));
    NEW.indexed_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search_vector
DROP TRIGGER IF EXISTS document_index_search_vector_trigger ON document_index;
CREATE TRIGGER document_index_search_vector_trigger
    BEFORE INSERT OR UPDATE OF extracted_text ON document_index
    FOR EACH ROW
    EXECUTE FUNCTION update_document_search_vector();

COMMENT ON FUNCTION update_document_search_vector IS 'Auto-update tsvector for fulltext search';
