#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║  MQTT EMQX SDK - Integration Test Runner                  ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Parse arguments
ACTION="${1:-up}"

case "$ACTION" in
  up)
    echo "🚀 Starting EMQX broker..."
    cd "$PROJECT_DIR"
    docker-compose up -d

    echo ""
    echo "⏳ Waiting for EMQX to be ready (30 seconds)..."
    sleep 10

    # Check if broker is ready
    for i in {1..20}; do
      if curl -s -u admin:public http://localhost:18083/api/v5/nodes > /dev/null 2>&1; then
        echo "✅ EMQX is ready!"
        echo ""
        echo "📊 Dashboard: http://localhost:18083"
        echo "   Username: admin"
        echo "   Password: public"
        echo ""
        echo "🧪 Running integration tests..."
        echo ""

        cd "$PROJECT_DIR"
        mvn clean test -Dtest=EmqxSdkClientExternalIntegrationTest

        if [ $? -eq 0 ]; then
          echo ""
          echo "✅ Integration tests passed!"
        else
          echo ""
          echo "❌ Integration tests failed!"
          exit 1
        fi
        exit 0
      fi

      if [ $((i % 5)) -eq 0 ]; then
        echo "  Attempt $i/20..."
      fi
      sleep 1.5
    done

    echo "❌ EMQX failed to start. Check logs:"
    docker logs mqtt-emqx-test
    exit 1
    ;;

  down)
    echo "🛑 Stopping EMQX broker..."
    cd "$PROJECT_DIR"
    docker-compose down
    echo "✅ EMQX stopped."
    ;;

  test)
    echo "🧪 Running integration tests against existing EMQX..."
    cd "$PROJECT_DIR"
    mvn clean test -Dtest=EmqxSdkClientExternalIntegrationTest
    ;;

  logs)
    echo "📋 EMQX logs:"
    docker logs mqtt-emqx-test
    ;;

  status)
    echo "📊 EMQX status:"
    docker ps | grep emqx || echo "EMQX is not running"
    ;;

  unit)
    echo "🧪 Running unit tests (with mocks, no Docker needed)..."
    cd "$PROJECT_DIR"
    mvn test -Dtest=EmqxSdkClientTest
    ;;

  all)
    echo "🧪 Running all tests (unit + integration)..."
    cd "$PROJECT_DIR"
    mvn test
    ;;

  clean)
    echo "🧹 Cleaning up..."
    cd "$PROJECT_DIR"
    docker-compose down 2>/dev/null || true
    mvn clean
    echo "✅ Cleaned."
    ;;

  *)
    echo "Usage: $0 {up|down|test|logs|status|unit|all|clean}"
    echo ""
    echo "Commands:"
    echo "  up      - Start EMQX and run integration tests"
    echo "  down    - Stop EMQX"
    echo "  test    - Run integration tests (EMQX must be running)"
    echo "  logs    - Show EMQX container logs"
    echo "  status  - Show EMQX container status"
    echo "  unit    - Run unit tests only (no Docker needed)"
    echo "  all     - Run all tests (unit + integration)"
    echo "  clean   - Stop EMQX and clean build artifacts"
    echo ""
    exit 1
    ;;
esac
