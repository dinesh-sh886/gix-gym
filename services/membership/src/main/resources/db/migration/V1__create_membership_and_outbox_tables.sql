-- Gix Gym — Membership Service Database Migration
-- Owner: dinesh-sh886
-- V1: Create membership and outbox tables

-- Membership table
CREATE TABLE membership (
    membership_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    member_id UUID NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'FROZEN', 'CANCELLED', 'EXPIRED')),
    frozen_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for membership
CREATE INDEX idx_membership_org_id ON membership (organization_id);
CREATE INDEX idx_membership_branch_id ON membership (branch_id);
CREATE INDEX idx_membership_status ON membership (status);
CREATE INDEX idx_membership_member_id ON membership (member_id);
CREATE INDEX idx_membership_org_member_status ON membership (organization_id, member_id, status);

-- Outbox event table for transactional outbox pattern
CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(200) NOT NULL,
    schema_version VARCHAR(20) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    organization_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    metadata JSONB,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for outbox_event
CREATE INDEX idx_outbox_unpublished ON outbox_event (published_at) WHERE published_at IS NULL;
CREATE INDEX idx_outbox_aggregate ON outbox_event (aggregate_id);
CREATE INDEX idx_outbox_org_id ON outbox_event (organization_id);
CREATE INDEX idx_outbox_created_at ON outbox_event (created_at);

-- Comments for documentation
COMMENT ON TABLE membership IS 'Membership records owned by the membership service';
COMMENT ON COLUMN membership.membership_id IS 'Unique identifier for the membership';
COMMENT ON COLUMN membership.organization_id IS 'Tenant organization identifier';
COMMENT ON COLUMN membership.branch_id IS 'Branch/location identifier';
COMMENT ON COLUMN membership.member_id IS 'Member identifier';
COMMENT ON COLUMN membership.status IS 'Current lifecycle status: ACTIVE, FROZEN, CANCELLED, EXPIRED';
COMMENT ON COLUMN membership.version IS 'Optimistic locking version';

COMMENT ON TABLE outbox_event IS 'Transactional outbox for reliable event publishing';
COMMENT ON COLUMN outbox_event.event_id IS 'Unique event identifier';
COMMENT ON COLUMN outbox_event.event_type IS 'Event type (e.g., membership.created, membership.frozen)';
COMMENT ON COLUMN outbox_event.schema_version IS 'Event schema version for compatibility';
COMMENT ON COLUMN outbox_event.aggregate_type IS 'Domain aggregate type (e.g., Membership)';
COMMENT ON COLUMN outbox_event.aggregate_id IS 'Aggregate identifier as string';
COMMENT ON COLUMN outbox_event.organization_id IS 'Tenant organization for event routing';
COMMENT ON COLUMN outbox_event.occurred_at IS 'When the domain event occurred';
COMMENT ON COLUMN outbox_event.payload IS 'Event payload as JSONB';
COMMENT ON COLUMN outbox_event.metadata IS 'Optional event metadata as JSONB';
COMMENT ON COLUMN outbox_event.published_at IS 'Timestamp when event was published to Kafka (NULL = pending)';
COMMENT ON COLUMN outbox_event.created_at IS 'Timestamp when outbox record was created';