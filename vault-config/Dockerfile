FROM hashicorp/vault:1.15.6

# Copy the Vault configuration
COPY vault.hcl /vault/config/vault.hcl

# Set environment variables
ENV VAULT_ADDR=http://127.0.0.1:8200
ENV SKIP_CHOWN=true
ENV SKIP_SETCAP=true

# Expose the Vault port
EXPOSE 8200

# Start Vault server with config
ENTRYPOINT ["vault"]
CMD ["server", "-config=/vault/config/vault.hcl"]
