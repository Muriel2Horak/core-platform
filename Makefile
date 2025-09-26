# Makefile for core-platform project
# Includes multitenancy smoke tests

.PHONY: help test-mt report-mt test-and-report clean-artifacts

# Default target
help:
	@echo "Available targets:"
	@echo "  test-mt         - Run multitenancy smoke tests"
	@echo "  report-mt       - Generate test report from artifacts"
	@echo "  test-and-report - Run tests and generate report"
	@echo "  clean-artifacts - Clean test artifacts"
	@echo "  help           - Show this help"

# Run multitenancy smoke tests
test-mt:
	@echo "🧪 Running multitenancy smoke tests..."
	@bash tests/multitenancy_smoke.sh

# Generate report from test artifacts
report-mt:
	@echo "📊 Generating test report..."
	@bash tests/make_report.sh

# Run tests and generate report
test-and-report: test-mt report-mt
	@echo ""
	@echo "🎉 Tests completed and report generated!"
	@echo "REPORT: ./TEST_REPORT.md"

# Clean test artifacts
clean-artifacts:
	@echo "🧹 Cleaning test artifacts..."
	@rm -rf artifacts/
	@rm -f TEST_REPORT.md
	@echo "✅ Artifacts cleaned"