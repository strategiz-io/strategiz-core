"""Configuration"""
import os


class Config:
    """Service configuration from environment variables"""

    def __init__(self):
        # Cloud Run sets PORT, fallback to GRPC_PORT for local dev
        self.port = int(os.getenv('PORT', os.getenv('GRPC_PORT', '50051')))
        self.max_workers = int(os.getenv('MAX_WORKERS', '20'))
        self.max_timeout_seconds = int(os.getenv('MAX_TIMEOUT_SECONDS', '30'))
        self.max_memory_mb = int(os.getenv('MAX_MEMORY_MB', '512'))
        self.log_level = os.getenv('LOG_LEVEL', 'INFO')
        self.environment = os.getenv('ENVIRONMENT', 'development')
