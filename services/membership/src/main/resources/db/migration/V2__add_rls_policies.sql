-- Gix Gym — Membership Service Database Migration
-- Owner: dinesh-sh886
-- V2: Add Row Level Security (RLS) policies for tenant isolation

-- Enable RLS on membership table
ALTER TABLE membership ENABLE ROW LEVEL SECURITY;

-- Enable RLS on outbox_event table
ALTER TABLE outbox_event ENABLE ROW LEVEL SECURITY;

-- Create policy for membership table: users can only see rows from their organization
CREATE POLICY membership_org_isolation ON membership
    USING (organization_id = current_setting('app.current_org_id', true)::uuid)
    WITH CHECK (organization_id = current_setting('app.current_org_id', true)::uuid);

-- Create policy for outbox_event table: users can only see events from their organization
CREATE POLICY outbox_event_org_isolation ON outbox_event
    USING (organization_id = current_setting('app.current_org_id', true)::uuid)
    WITH CHECK (organization_id = current_setting('app.current_org_id', true)::uuid);

-- Grant necessary permissions to the service role
GRANT SELECT, INSERT, UPDATE, DELETE ON membership TO gix_membership;
GRANT SELECT, INSERT, UPDATE, DELETE ON outbox_event TO gix_membership;

-- Grant usage on sequences
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO gix_membership;

-- Note: The service role 'gix_membership' should NOT have BYPASSRLS privilege
-- This ensures RLS policies are enforced even for the service role