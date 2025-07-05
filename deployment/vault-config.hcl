storage "file" {
  path = "/app/vault/data"
}

listener "tcp" {
  address = "0.0.0.0:8200"
  tls_disable = 1
}

# API address for Vault
api_addr = "http://localhost:8200"

# Disable mlock for containers
disable_mlock = true

# Enable UI for development/debugging
ui = true

# Default lease TTL
default_lease_ttl = "168h"
max_lease_ttl = "720h" 