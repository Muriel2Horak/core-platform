-- =====================================================
-- V1.1: SEED DATA FOR DEV/CI TESTING
-- Creates demo tenants, users, and documents
-- =====================================================

-- Seed 2 demo tenants (admin already exists from V1)
INSERT INTO tenants (id, key) VALUES
    (generate_tenant_uuid('test-tenant'), 'test-tenant'),
    (generate_tenant_uuid('company-b'), 'company-b')
ON CONFLICT (key) DO NOTHING;

-- Seed demo users for test-tenant
INSERT INTO users_directory (
    tenant_id, username, email, first_name, last_name, 
    display_name, department, position, active
) VALUES
    (generate_tenant_uuid('test-tenant'), 'alice', 'alice@test-tenant.local', 'Alice', 'Anderson', 'Alice Anderson', 'Engineering', 'Senior Developer', true),
    (generate_tenant_uuid('test-tenant'), 'bob', 'bob@test-tenant.local', 'Bob', 'Brown', 'Bob Brown', 'Engineering', 'Team Lead', true),
    (generate_tenant_uuid('test-tenant'), 'charlie', 'charlie@test-tenant.local', 'Charlie', 'Chen', 'Charlie Chen', 'Product', 'Product Manager', true)
ON CONFLICT DO NOTHING;

-- Seed demo users for company-b
INSERT INTO users_directory (
    tenant_id, username, email, first_name, last_name, 
    display_name, department, position, active
) VALUES
    (generate_tenant_uuid('company-b'), 'diana', 'diana@company-b.local', 'Diana', 'Davis', 'Diana Davis', 'Sales', 'Account Manager', true),
    (generate_tenant_uuid('company-b'), 'eric', 'eric@company-b.local', 'Eric', 'Evans', 'Eric Evans', 'Support', 'Support Engineer', true)
ON CONFLICT DO NOTHING;

-- Create user profiles for test-tenant users
INSERT INTO user_profile (tenant_id, user_id, full_name, email, department, position, bio)
SELECT 
    ud.tenant_id as tenant_id,
    ud.id as user_id,
    ud.display_name as full_name,
    ud.email,
    ud.department,
    ud.position,
    'Demo user for testing' as bio
FROM users_directory ud
WHERE ud.tenant_id IN (generate_tenant_uuid('test-tenant'), generate_tenant_uuid('company-b'))
  AND NOT EXISTS (SELECT 1 FROM user_profile up WHERE up.user_id = ud.id);

-- Set initial states for user profiles
INSERT INTO entity_state (entity_type, entity_id, tenant_id, state_code, sla_minutes)
SELECT 
    'UserProfile' as entity_type,
    up.id::TEXT as entity_id,
    up.tenant_id,
    'active' as state_code,
    NULL as sla_minutes
FROM user_profile up
WHERE NOT EXISTS (
    SELECT 1 FROM entity_state es 
    WHERE es.entity_type = 'UserProfile' 
      AND es.entity_id = up.id::TEXT
);

-- Seed demo documents for fulltext search testing
DO $$
DECLARE
    v_alice_id UUID;
    v_bob_id UUID;
    v_doc1_id UUID := gen_random_uuid();
    v_doc2_id UUID := gen_random_uuid();
    v_doc3_id UUID := gen_random_uuid();
BEGIN
    -- Get user IDs
    SELECT id INTO v_alice_id FROM users_directory WHERE username = 'alice' AND tenant_id = generate_tenant_uuid('test-tenant');
    SELECT id INTO v_bob_id FROM users_directory WHERE username = 'bob' AND tenant_id = generate_tenant_uuid('test-tenant');
    
    IF v_alice_id IS NOT NULL AND v_bob_id IS NOT NULL THEN
        -- Insert demo documents
        INSERT INTO document (id, tenant_id, entity_type, entity_id, filename, mime_type, size_bytes, storage_key, uploaded_by) VALUES
            (v_doc1_id, 'test-tenant', 'UserProfile', v_alice_id::TEXT, 'project-proposal.pdf', 'application/pdf', 52480, 'demo/project-proposal.pdf', 'alice'),
            (v_doc2_id, 'test-tenant', 'UserProfile', v_bob_id::TEXT, 'technical-spec.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 128000, 'demo/technical-spec.docx', 'bob'),
            (v_doc3_id, 'test-tenant', 'UserProfile', v_alice_id::TEXT, 'meeting-notes.txt', 'text/plain', 4096, 'demo/meeting-notes.txt', 'alice')
        ON CONFLICT DO NOTHING;
        
        -- Insert fulltext search indexes with demo content
        INSERT INTO document_index (document_id, tenant_id, extracted_text) VALUES
            (v_doc1_id, 'test-tenant', 
             E'Project Proposal: New Metamodel Architecture\n\n' ||
             'Executive Summary:\n' ||
             'This document outlines the implementation plan for a new metamodel-driven CRUD architecture. ' ||
             'The system will support dynamic entity management with attribute-based access control (ABAC), ' ||
             'version control, and multi-tenancy. Key features include workflow state management, ' ||
             'document storage with fulltext search, and real-time presence tracking.\n\n' ||
             'Technical Stack: Spring Boot, PostgreSQL, Redis, MinIO\n' ||
             'Timeline: Q1 2025\n' ||
             'Budget: $150,000'),
            
            (v_doc2_id, 'test-tenant', 
             E'Technical Specification: Workflow Engine\n\n' ||
             'Overview:\n' ||
             'The workflow engine manages entity state transitions with SLA tracking and guard conditions. ' ||
             'Each entity type (UserProfile, Document, etc.) can define custom state machines with validation rules. ' ||
             'SLA breach detection triggers notifications when transitions exceed defined time limits.\n\n' ||
             'Implementation Details:\n' ||
             '- State storage in entity_state table with PostgreSQL triggers\n' ||
             '- Guard evaluation using Spring Expression Language (SpEL)\n' ||
             '- Audit trail in entity_state_log for compliance\n' ||
             '- Integration with Redis for caching current states'),
            
            (v_doc3_id, 'test-tenant', 
             E'Meeting Notes: Architecture Review - January 9, 2025\n\n' ||
             'Attendees: Alice, Bob, Charlie\n\n' ||
             'Discussion Points:\n' ||
             '1. Fulltext search implementation using PostgreSQL tsvector and GIN indexes\n' ||
             '2. Document storage strategy: MinIO for object storage with versioning\n' ||
             '3. Cache invalidation via PostgreSQL NOTIFY/LISTEN pattern\n' ||
             '4. RLS policies for tenant isolation in multi-tenant environment\n\n' ||
             'Action Items:\n' ||
             '- Alice: Implement FilterParser for advanced query DSL\n' ||
             '- Bob: Set up Testcontainers for integration tests\n' ||
             '- Charlie: Review ABAC policy definitions\n\n' ||
             'Next meeting: January 16, 2025')
        ON CONFLICT DO NOTHING;
        
        RAISE NOTICE '✅ Demo documents created with fulltext content';
    END IF;
END $$;

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
DECLARE
    v_tenant_count INTEGER;
    v_user_count INTEGER;
    v_profile_count INTEGER;
    v_doc_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_tenant_count FROM tenants;
    SELECT COUNT(*) INTO v_user_count FROM users_directory;
    SELECT COUNT(*) INTO v_profile_count FROM user_profile;
    SELECT COUNT(*) INTO v_doc_count FROM document;
    
    RAISE NOTICE '✅ V1.1 Seed data initialized successfully';
    RAISE NOTICE '📊 Tenants: %', v_tenant_count;
    RAISE NOTICE '📊 Users: %', v_user_count;
    RAISE NOTICE '📊 User Profiles: %', v_profile_count;
    RAISE NOTICE '📊 Documents: %', v_doc_count;
    RAISE NOTICE '🔍 Fulltext search ready for testing';
END $$;
