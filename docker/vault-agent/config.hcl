# Vault Agent configuration for core-platform
# Generates secret files for services that cannot read from Vault API directly.

pid_file = "/tmp/vault-agent.pid"

vault {
  address = "__VAULT_ADDR__"
}

auto_auth {
  method "approle" {
    mount_path = "auth/approle"
    config = {
      role_id_file_path   = "/vault/approle/agent-role-id"
      secret_id_file_path = "/vault/approle/agent-secret-id"
      remove_secret_id_file_after_reading = false
    }
  }

  sink "file" {
    config = {
      path = "/tmp/vault-token"
      mode = 0640
    }
  }
}

# Core database password
template {
  source      = "/vault/templates/postgres-password.ctmpl"
  destination = "/vault/secrets/postgres-password"
  perms       = 0640
}

# Keycloak DB password
template {
  source      = "/vault/templates/keycloak-db-password.ctmpl"
  destination = "/vault/secrets/keycloak-db-password"
  perms       = 0640
}

# Backend secrets env
template {
  source      = "/vault/templates/backend.env.ctmpl"
  destination = "/vault/secrets/backend.env"
  perms       = 0640
}

# Keycloak secrets env
template {
  source      = "/vault/templates/keycloak.env.ctmpl"
  destination = "/vault/secrets/keycloak.env"
  perms       = 0640
}

# Grafana secrets
template {
  source      = "/vault/templates/grafana-admin-password.ctmpl"
  destination = "/vault/secrets/grafana-admin-password"
  perms       = 0640
}

template {
  source      = "/vault/templates/grafana-db-password.ctmpl"
  destination = "/vault/secrets/grafana-db-password"
  perms       = 0640
}

template {
  source      = "/vault/templates/grafana-oidc-secret.ctmpl"
  destination = "/vault/secrets/grafana-oidc-secret"
  perms       = 0640
}

template {
  source      = "/vault/templates/grafana-jwt-secret.ctmpl"
  destination = "/vault/secrets/grafana-jwt-secret"
  perms       = 0640
}

# PgAdmin env
template {
  source      = "/vault/templates/pgadmin.env.ctmpl"
  destination = "/vault/secrets/pgadmin.env"
  perms       = 0640
}

# Redis password
template {
  source      = "/vault/templates/redis-password.ctmpl"
  destination = "/vault/secrets/redis-password"
  perms       = 0640
}

# MinIO env
template {
  source      = "/vault/templates/minio.env.ctmpl"
  destination = "/vault/secrets/minio.env"
  perms       = 0640
}

# Cube env
template {
  source      = "/vault/templates/cube.env.ctmpl"
  destination = "/vault/secrets/cube.env"
  perms       = 0640
}

# Postgres exporter env
template {
  source      = "/vault/templates/postgres-exporter.env.ctmpl"
  destination = "/vault/secrets/postgres-exporter.env"
  perms       = 0640
}
