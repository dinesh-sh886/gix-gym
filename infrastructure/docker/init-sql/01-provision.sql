-- Gix Gym — Local Database Provisioning
-- Owner: dinesh-sh886
-- Runs ONLY when PostgreSQL data volume is empty (first startup)
-- To reset: docker compose down -v && docker compose up -d

-- Create databases (one per service)
CREATE DATABASE gix_membership;
CREATE DATABASE gix_facility;
CREATE DATABASE gix_booking;
CREATE DATABASE gix_payment;
CREATE DATABASE gix_notification;

-- Create least-privilege roles (passwords from environment via .env)
-- Note: Password substitution requires POSTGRES init script templating.
-- For local dev, use static passwords or run this manually after startup.
-- See: https://github.com/docker-library/postgres/issues/550

-- Roles will be created with passwords via environment variables in a follow-up script
-- or manually. For now, create roles without passwords (local trust auth can be used).

CREATE ROLE gix_membership WITH LOGIN;
CREATE ROLE gix_facility WITH LOGIN;
CREATE ROLE gix_booking WITH LOGIN;
CREATE ROLE gix_payment WITH LOGIN;
CREATE ROLE gix_notification WITH LOGIN;

-- Grant CONNECT on respective databases
GRANT CONNECT ON DATABASE gix_membership TO gix_membership;
GRANT CONNECT ON DATABASE gix_facility TO gix_facility;
GRANT CONNECT ON DATABASE gix_booking TO gix_booking;
GRANT CONNECT ON DATABASE gix_payment TO gix_payment;
GRANT CONNECT ON DATABASE gix_notification TO gix_notification;

-- Grant schema usage and create rights (Flyway needs this)
\c gix_membership
GRANT USAGE, CREATE ON SCHEMA public TO gix_membership;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gix_membership;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gix_membership;

\c gix_facility
GRANT USAGE, CREATE ON SCHEMA public TO gix_facility;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gix_facility;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gix_facility;

\c gix_booking
GRANT USAGE, CREATE ON SCHEMA public TO gix_booking;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gix_booking;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gix_booking;

\c gix_payment
GRANT USAGE, CREATE ON SCHEMA public TO gix_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gix_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gix_payment;

\c gix_notification
GRANT USAGE, CREATE ON SCHEMA public TO gix_notification;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gix_notification;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gix_notification;

-- Return to default database
\c postgres

-- Note: For production-like local setup with passwords, run 02-set-passwords.sql manually
-- after containers are up, or use a custom entrypoint script that interpolates env vars.