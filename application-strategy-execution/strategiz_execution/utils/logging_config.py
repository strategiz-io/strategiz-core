"""Logging Configuration"""
import logging
import sys


def setup_logging(level='INFO'):
    """Setup structured JSON logging"""

    logging.basicConfig(
        level=getattr(logging, level.upper()),
        format='{"timestamp": "%(asctime)s", "logger": "%(name)s", "level": "%(levelname)s", "message": "%(message)s"}',
        handlers=[logging.StreamHandler(sys.stdout)]
    )
