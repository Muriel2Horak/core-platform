storage "raft" {
  path    = "/vault/data"
  node_id = "vault-node-1"

  autopilot {
    cleanup_dead_servers   = true
    last_contact_threshold = "10s"
    max_trailing_logs      = 1000
    min_quorum             = 1
  }
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1

  telemetry {
    unauthenticated_metrics_access = true
  }
}

ui = true
cluster_name = "core-platform-dev"
api_addr = "http://vault:8200"
cluster_addr = "http://vault:8201"

telemetry {
  prometheus_retention_time = "30s"
  disable_hostname          = false
}

disable_mlock = true
