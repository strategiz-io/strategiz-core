ui = true
disable_mlock = true

# Listen on all interfaces for Cloud Run
listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = true  # Cloud Run handles TLS termination
}

# Use Google Cloud Storage for persistent storage
storage "gcs" {
  bucket     = "strategiz-vault-storage"
  ha_enabled = "false"
}

# Auto-unseal using Google Cloud KMS
seal "gcpckms" {
  project     = "strategiz-io"
  region      = "us-central1"
  key_ring    = "vault-keyring"
  crypto_key  = "vault-unseal-key"
}

# API address for Cloud Run
api_addr = "https://strategiz-vault-bflhiwsnmq-uc.a.run.app"
