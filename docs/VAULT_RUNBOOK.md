# Vault Operations Runbook

## Quick Start

```bash
make vault-up
make vault-bootstrap
make vault-smoke
```

## Rotation

Rotate backend DB password:

```bash
make vault-rotate-backend-db-pass
```

Notes:
- Uses `kv/core/postgres` to fetch username/db name.
- Restarts backend if running.

## Smoke Checks

```bash
make vault-smoke
make vault-smoke-runtime
```

## Audit Log

Vault audit log is written to `/vault/logs/audit.log` and scraped by Promtail.
Use Loki to query `job="vault-audit"`.

## Alerts

Prometheus alerts are defined in `docker/prometheus/alerts/axiom_vault.yml`:
- `VaultSealed` (critical)
- `VaultNoLeader` (warning)

## Break-Glass

If Vault token is lost, use the unseal key file:
- `~/.vault-unseal-key` (local)
- Regenerate token using `vault operator generate-root` inside Vault container.

## DR / Backup

Vault data is stored in Raft at `/vault/data`.
Snapshots can be taken with:

```bash
docker exec core-vault vault operator raft snapshot save /vault/data/vault.snap
```

Restore uses `vault operator raft snapshot restore` in a sealed state.
