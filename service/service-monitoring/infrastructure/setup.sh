#!/bin/bash

# 🔍 Strategiz Observability Stack Setup Script
# This script sets up the complete observability infrastructure

set -e

echo "🚀 Setting up Strategiz Observability Stack..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose not found. Please install docker-compose."
    exit 1
fi

print_status "Docker is running ✅"

# Create necessary directories
print_status "Creating log directories..."
mkdir -p ../../../../logs
mkdir -p ../dashboards/grafana/provisioning/{datasources,dashboards}

# Set permissions
chmod 755 ../../../../logs
chmod -R 755 ../dashboards/

print_status "Directories created ✅"

# Create logs directory for container access
if [ ! -f ../../../../logs/.gitkeep ]; then
    touch ../../../../logs/.gitkeep
    echo "# Logs directory for observability stack" > ../../../../logs/README.md
fi

# Stop existing containers if running
print_status "Stopping any existing observability containers..."
docker-compose -f docker-compose.observability.yml down --remove-orphans 2>/dev/null || true

# Pull latest images
print_status "Pulling latest container images..."
docker-compose -f docker-compose.observability.yml pull

# Start the observability stack
print_status "Starting observability stack..."
docker-compose -f docker-compose.observability.yml up -d

# Wait for services to be ready
print_status "Waiting for services to start..."
sleep 10

# Check service health
print_status "Checking service health..."

# Check Loki
if curl -s http://localhost:3100/ready > /dev/null; then
    print_status "Loki is ready ✅"
else
    print_warning "Loki might not be ready yet ⚠️"
fi

# Check Prometheus
if curl -s http://localhost:9090/-/ready > /dev/null; then
    print_status "Prometheus is ready ✅"
else
    print_warning "Prometheus might not be ready yet ⚠️"
fi

# Check Grafana
if curl -s http://localhost:3001/api/health > /dev/null; then
    print_status "Grafana is ready ✅"
else
    print_warning "Grafana might not be ready yet ⚠️"
fi

# Create sample log entry
print_status "Creating sample log entry..."
echo '{"timestamp":"'$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")'"},{"level":"INFO","logger":"io.strategiz.setup","message":"Observability stack setup completed","mdc":{"requestId":"setup_'$(date +%s)'","component":"setup-script"}}' >> ../../../../logs/application.log

print_status "Sample log entry created ✅"

echo ""
echo "🎉 Observability Stack Setup Complete!"
echo ""
echo "Access your dashboards:"
echo "  📊 Grafana:    http://localhost:3001 (admin/strategiz123)"
echo "  📈 Prometheus: http://localhost:9090"
echo "  📋 Loki:      http://localhost:3100"
echo ""
echo "📚 Next Steps:"
echo "  1. Check the Strategiz Overview dashboard in Grafana"
echo "  2. Start your Spring Boot app to see real metrics"
echo "  3. Check logs with: {job=\"strategiz-app\"}"
echo "  4. Read README.md for more details"
echo ""
echo "🔧 Management Commands:"
echo "  Stop:    docker-compose -f docker-compose.observability.yml down"
echo "  Restart: docker-compose -f docker-compose.observability.yml restart"
echo "  Logs:    docker-compose -f docker-compose.observability.yml logs -f"
echo ""

# Show container status
echo "📦 Container Status:"
docker-compose -f docker-compose.observability.yml ps

print_status "Setup completed successfully! 🚀" 