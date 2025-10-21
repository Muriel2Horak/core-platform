-- docker/db/init.sql

-- Create Keycloak database and user
CREATE DATABASE keycloak;
CREATE USER keycloak WITH PASSWORD 'keycloak';
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- PostgreSQL 15+ requires explicit GRANT on public schema
\c keycloak;
GRANT ALL ON SCHEMA public TO keycloak;

-- Create Grafana database and user
-- Using dedicated grafana user for better isolation (same pattern as keycloak)
CREATE DATABASE grafana;
CREATE USER grafana WITH PASSWORD 'grafana';
GRANT ALL PRIVILEGES ON DATABASE grafana TO grafana;

-- PostgreSQL 15+ requires explicit GRANT on public schema
\c grafana;
GRANT ALL ON SCHEMA public TO grafana;

-- Core application health check
CREATE TABLE IF NOT EXISTS health_check (
    id SERIAL PRIMARY KEY,
    status TEXT NOT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO health_check (status) VALUES ('OK');