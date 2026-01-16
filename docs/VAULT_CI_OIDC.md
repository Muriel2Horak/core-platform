# Vault CI OIDC (GitHub Actions)

## Prerequisites

- Vault running and unsealed
- `VAULT_TOKEN` available (or `~/.vault-token`)
- GitHub org/repo values

## Setup

```bash
export GITHUB_ORG=your-org
export GITHUB_REPO=your-repo
export OIDC_AUDIENCE=vault
make vault-oidc-setup
```

## Notes

- Role name: `github-actions`
- JWT auth uses GitHub OIDC issuer: `https://token.actions.githubusercontent.com`
- Policy attached: `core-platform-agent`
