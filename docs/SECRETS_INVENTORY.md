# Secrets Inventory

## KV Namespace

Vault KV v2 mount: `kv`
Path prefix: `kv/core/*`

## Secrets

| Path | Owner | Rotation | Notes |
| --- | --- | --- | --- |
| kv/core/postgres | DBA | Quarterly | Core DB credentials |
| kv/core/keycloak-db | DBA | Quarterly | Keycloak DB credentials |
| kv/core/keycloak | Platform | Quarterly | Keycloak admin/client secrets |
| kv/core/grafana | Platform | Quarterly | Grafana admin + OAuth secrets |
| kv/core/pgadmin | Platform | Quarterly | PgAdmin creds |
| kv/core/redis | Platform | Quarterly | Redis password |
| kv/core/minio | Platform | Quarterly | MinIO credentials |
| kv/core/cube | Platform | Quarterly | Cube API secret |

## Rotation Runbooks

- Backend DB: `make vault-rotate-backend-db-pass`
