-- Gix Gym — Set Service Role Passwords
-- Owner: dinesh-sh886
-- Run MANUALLY after first startup if using password authentication:
--   docker compose exec -T postgres psql -U postgres -f /docker-entrypoint-initdb.d/02-set-passwords.sql
-- Or include in a custom entrypoint that interpolates ${VAR} from environment.

-- Passwords should be set via .env and interpolated externally.
-- Placeholders shown here; replace with actual values from .env

ALTER ROLE gix_membership WITH PASSWORD 'changeme_membership';
ALTER ROLE gix_facility WITH PASSWORD 'changeme_facility';
ALTER ROLE gix_booking WITH PASSWORD 'changeme_booking';
ALTER ROLE gix_payment WITH PASSWORD 'changeme_payment';
ALTER ROLE gix_notification WITH PASSWORD 'changeme_notification';

-- Verify
\du gix_membership gix_facility gix_booking gix_payment gix_notification