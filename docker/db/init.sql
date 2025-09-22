-- docker/db/init.sql

-- Create Keycloak database and user
CREATE DATABASE keycloak;
CREATE USER keycloak WITH PASSWORD 'keycloak';
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Core application health check
CREATE TABLE IF NOT EXISTS health_check (
    id SERIAL PRIMARY KEY,
    status TEXT NOT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO health_check (status) VALUES ('OK');