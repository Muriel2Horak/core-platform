-- docker/db/init.sql

CREATE TABLE IF NOT EXISTS health_check (
    id SERIAL PRIMARY KEY,
    status TEXT NOT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO health_check (status) VALUES ('OK');