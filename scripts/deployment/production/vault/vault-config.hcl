ui = true

listener "tcp" {
  address = "0.0.0.0:8200"
  tls_disable = "true"
}

storage "gcs" {
  bucket = "strategiz-io-vault-storage"
  ha_enabled = "true"
}

api_addr = "https://strategiz-vault-strategiz-io-us-central1.a.run.app"
cluster_addr = "https://strategiz-vault-strategiz-io-us-central1.a.run.app:8201"

log_level = "INFO"
default_lease_ttl = "168h"
max_lease_ttl = "720h"
