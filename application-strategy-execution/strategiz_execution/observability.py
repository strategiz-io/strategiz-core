"""
OpenTelemetry Instrumentation for Strategy Execution Service
Exports metrics and traces to Cloud Monitoring/Trace
"""

import logging
from opentelemetry import metrics, trace
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource, SERVICE_NAME, SERVICE_VERSION
from opentelemetry.exporter.otlp.proto.grpc.metric_exporter import OTLPMetricExporter
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.grpc import GrpcInstrumentorServer

logger = logging.getLogger(__name__)


class ObservabilityManager:
    """Manages OpenTelemetry instrumentation"""

    def __init__(self, service_name: str = "strategiz-execution", environment: str = "production"):
        self.service_name = service_name
        self.environment = environment

        # Create resource with service info
        self.resource = Resource.create({
            SERVICE_NAME: service_name,
            SERVICE_VERSION: "1.0.0",
            "environment": environment,
        })

        # Initialize providers
        self._setup_tracing()
        self._setup_metrics()

        # Get tracer and meter
        self.tracer = trace.get_tracer(__name__)
        self.meter = metrics.get_meter(__name__)

        # Create custom metrics
        self._create_metrics()

        logger.info(f"✅ OpenTelemetry initialized for {service_name} ({environment})")

    def _setup_tracing(self):
        """Setup distributed tracing - exports to Grafana Cloud"""
        import os

        # Create tracer provider
        tracer_provider = TracerProvider(resource=self.resource)

        # Get Grafana Cloud OTLP config from environment (same as Java app)
        grafana_endpoint = os.getenv(
            'GRAFANA_OTLP_ENDPOINT',
            'https://otlp-gateway-prod-us-central-0.grafana.net/otlp'
        )
        grafana_auth = os.getenv('GRAFANA_OTLP_AUTH_HEADER', '')

        # Add OTLP span exporter (sends to Grafana Cloud)
        headers = {}
        if grafana_auth:
            headers['Authorization'] = f'Basic {grafana_auth}'

        otlp_exporter = OTLPSpanExporter(
            endpoint=grafana_endpoint,
            headers=headers
        )

        tracer_provider.add_span_processor(
            BatchSpanProcessor(otlp_exporter)
        )

        # Set global tracer provider
        trace.set_tracer_provider(tracer_provider)

        logger.info(f"✅ Tracing configured: {grafana_endpoint}")

    def _setup_metrics(self):
        """Setup metrics collection - exports to Grafana Cloud"""
        import os

        # Get Grafana Cloud OTLP config from environment (same as Java app)
        grafana_endpoint = os.getenv(
            'GRAFANA_OTLP_ENDPOINT',
            'https://otlp-gateway-prod-us-central-0.grafana.net/otlp'
        )
        grafana_auth = os.getenv('GRAFANA_OTLP_AUTH_HEADER', '')

        # Create headers with authentication
        headers = {}
        if grafana_auth:
            headers['Authorization'] = f'Basic {grafana_auth}'

        # Create OTLP metric exporter (sends to Grafana Cloud)
        otlp_exporter = OTLPMetricExporter(
            endpoint=grafana_endpoint,
            headers=headers
        )

        # Create metric reader with 60s export interval
        metric_reader = PeriodicExportingMetricReader(
            otlp_exporter,
            export_interval_millis=60000  # Export every 60s
        )

        # Create meter provider
        meter_provider = MeterProvider(
            resource=self.resource,
            metric_readers=[metric_reader]
        )

        # Set global meter provider
        metrics.set_meter_provider(meter_provider)

        logger.info(f"✅ Metrics configured: {grafana_endpoint}")

    def _create_metrics(self):
        """Create custom metrics for strategy execution"""

        # Execution time histogram (ms)
        self.execution_time_histogram = self.meter.create_histogram(
            name="strategy.execution.time",
            description="Strategy execution time in milliseconds",
            unit="ms"
        )

        # Compilation time histogram (ms)
        self.compilation_time_histogram = self.meter.create_histogram(
            name="strategy.compilation.time",
            description="Strategy compilation time in milliseconds",
            unit="ms"
        )

        # Cache hit counter
        self.cache_hit_counter = self.meter.create_counter(
            name="strategy.cache.hits",
            description="Number of code cache hits"
        )

        # Cache miss counter
        self.cache_miss_counter = self.meter.create_counter(
            name="strategy.cache.misses",
            description="Number of code cache misses"
        )

        # Request counter (by language and status)
        self.request_counter = self.meter.create_counter(
            name="strategy.requests.total",
            description="Total strategy execution requests"
        )

        # Error counter (by error type)
        self.error_counter = self.meter.create_counter(
            name="strategy.errors.total",
            description="Total execution errors"
        )

        # Active executions gauge
        self.active_executions = self.meter.create_up_down_counter(
            name="strategy.executions.active",
            description="Number of currently executing strategies"
        )

        # Market data size histogram (number of bars)
        self.market_data_size = self.meter.create_histogram(
            name="strategy.market_data.size",
            description="Number of market data bars processed",
            unit="bars"
        )

    def instrument_grpc_server(self):
        """Instrument gRPC server with OpenTelemetry"""
        # Auto-instrument all gRPC servers
        GrpcInstrumentorServer().instrument()
        logger.info("✅ gRPC auto-instrumentation enabled")

    def record_execution(
        self,
        execution_time_ms: float,
        language: str,
        success: bool,
        cache_hit: bool = False,
        compilation_time_ms: float = 0,
        market_data_bars: int = 0,
        error_type: str = None
    ):
        """Record a strategy execution"""

        # Record execution time
        self.execution_time_histogram.record(
            execution_time_ms,
            {"language": language, "success": str(success)}
        )

        # Record compilation time if applicable
        if compilation_time_ms > 0:
            self.compilation_time_histogram.record(
                compilation_time_ms,
                {"language": language}
            )

        # Record cache hit/miss
        if cache_hit:
            self.cache_hit_counter.add(1, {"language": language})
        else:
            self.cache_miss_counter.add(1, {"language": language})

        # Record request
        status = "success" if success else "failure"
        self.request_counter.add(1, {"language": language, "status": status})

        # Record error if failed
        if not success and error_type:
            self.error_counter.add(1, {"language": language, "error_type": error_type})

        # Record market data size
        if market_data_bars > 0:
            self.market_data_size.record(market_data_bars, {"language": language})

    def start_execution(self):
        """Mark execution started"""
        self.active_executions.add(1)

    def end_execution(self):
        """Mark execution ended"""
        self.active_executions.add(-1)


# Global instance
_observability = None


def get_observability(service_name: str = "strategiz-execution", environment: str = "production") -> ObservabilityManager:
    """Get or create observability manager singleton"""
    global _observability
    if _observability is None:
        _observability = ObservabilityManager(service_name, environment)
    return _observability
