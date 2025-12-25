"""
Strategy Execution Service - gRPC Server

Isolated service for executing trading strategies with complete security isolation.
"""

import grpc
import logging
import signal
from concurrent import futures

from grpc_health.v1 import health
from grpc_health.v1 import health_pb2
from grpc_health.v1 import health_pb2_grpc
from grpc_reflection.v1alpha import reflection

from strategiz_execution.service.execution_servicer import StrategyExecutionServicer
from strategiz_execution.generated import strategy_execution_pb2
from strategiz_execution.generated import strategy_execution_pb2_grpc
from strategiz_execution.utils.logging_config import setup_logging
from strategiz_execution.config import Config

logger = logging.getLogger(__name__)


class GracefulKiller:
    """Handle graceful shutdown on SIGTERM/SIGINT"""

    kill_now = False

    def __init__(self):
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)

    def exit_gracefully(self, signum, frame):
        logger.info(f"Received signal {signum}, shutting down gracefully...")
        self.kill_now = True


def serve():
    """Start the gRPC server"""

    # Load configuration
    config = Config()

    # Setup logging
    setup_logging(config.log_level)
    logger.info("Starting Strategy Execution Service...")

    # Create gRPC server
    server = grpc.server(
        futures.ThreadPoolExecutor(max_workers=config.max_workers),
        options=[
            ('grpc.max_receive_message_length', 50 * 1024 * 1024),  # 50MB
            ('grpc.max_send_message_length', 50 * 1024 * 1024),
            ('grpc.keepalive_time_ms', 30000),
            ('grpc.keepalive_timeout_ms', 10000),
        ]
    )

    # Add main execution service
    strategy_execution_pb2_grpc.add_StrategyExecutionServiceServicer_to_server(
        StrategyExecutionServicer(), server
    )

    # Add health check service
    health_servicer = health.HealthServicer()
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    health_servicer.set(
        "strategiz.execution.v1.StrategyExecutionService",
        health_pb2.HealthCheckResponse.SERVING
    )

    # Add reflection service
    service_names = (
        strategy_execution_pb2.DESCRIPTOR.services_by_name['StrategyExecutionService'].full_name,
        reflection.SERVICE_NAME,
    )
    reflection.enable_server_reflection(service_names, server)

    # Start server
    server.add_insecure_port(f'[::]:{config.port}')
    server.start()

    logger.info(f"✅ Strategy Execution Service listening on port {config.port}")
    logger.info(f"   Environment: {config.environment}")
    logger.info(f"   Max workers: {config.max_workers}")
    logger.info(f"   Max timeout: {config.max_timeout_seconds}s")

    # Wait for termination
    killer = GracefulKiller()
    try:
        import time
        while not killer.kill_now:
            time.sleep(1)
    except KeyboardInterrupt:
        pass

    # Graceful shutdown
    logger.info("Shutting down gRPC server...")
    health_servicer.enter_graceful_shutdown()
    server.stop(grace=30)
    logger.info("Server stopped")


if __name__ == '__main__':
    serve()
