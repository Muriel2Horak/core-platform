#!/bin/bash
# 🔒 Check for vulnerabilities in dependencies (S8)
# This script runs OWASP Dependency Check and parses results

set -e

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo "🔒 Running security vulnerability check..."
echo ""

# Navigate to backend directory
cd "$(dirname "$0")/../../backend"

# Run OWASP Dependency Check
echo "📦 Analyzing dependencies with OWASP Dependency-Check..."
./mvnw org.owasp:dependency-check-maven:check \
  -DfailBuildOnCVSS=7 \
  -DsuppressionFile=owasp-suppressions.xml \
  -Dformats=HTML,JSON

# Check if report was generated
REPORT_JSON="target/dependency-check/dependency-check-report.json"
REPORT_HTML="target/dependency-check/dependency-check-report.html"

if [ ! -f "$REPORT_JSON" ]; then
  echo "${RED}❌ Security scan report not found!${NC}"
  exit 1
fi

# Parse results
echo ""
echo "📊 Parsing results..."
TOTAL_DEPS=$(jq '.dependencies | length' "$REPORT_JSON")
VULN_DEPS=$(jq '[.dependencies[] | select(.vulnerabilities != null)] | length' "$REPORT_JSON")

if [ "$VULN_DEPS" -eq 0 ]; then
  echo "${GREEN}✅ No vulnerabilities found in $TOTAL_DEPS dependencies${NC}"
  echo ""
  echo "📄 Report: $REPORT_HTML"
  exit 0
fi

# Extract vulnerability details
echo "${YELLOW}⚠️  Found vulnerabilities in $VULN_DEPS out of $TOTAL_DEPS dependencies${NC}"
echo ""

# Count by severity
CRITICAL=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "CRITICAL")] | length' "$REPORT_JSON")
HIGH=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "HIGH")] | length' "$REPORT_JSON")
MEDIUM=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "MEDIUM")] | length' "$REPORT_JSON")
LOW=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "LOW")] | length' "$REPORT_JSON")

echo "📊 Severity breakdown:"
[ "$CRITICAL" -gt 0 ] && echo "${RED}  - CRITICAL: $CRITICAL${NC}" || echo "  - CRITICAL: 0"
[ "$HIGH" -gt 0 ] && echo "${RED}  - HIGH: $HIGH${NC}" || echo "  - HIGH: 0"
[ "$MEDIUM" -gt 0 ] && echo "${YELLOW}  - MEDIUM: $MEDIUM${NC}" || echo "  - MEDIUM: 0"
echo "  - LOW: $LOW"
echo ""

# List affected dependencies
echo "🔍 Affected dependencies:"
jq -r '.dependencies[] | select(.vulnerabilities != null) | 
  "  - " + .fileName + " (" + (.vulnerabilities | map(.name) | join(", ")) + ")"' "$REPORT_JSON"

echo ""
echo "📄 Full report: $REPORT_HTML"
echo ""

# Fail if HIGH or CRITICAL vulnerabilities found
if [ "$CRITICAL" -gt 0 ] || [ "$HIGH" -gt 0 ]; then
  echo "${RED}❌ Build failed: HIGH or CRITICAL vulnerabilities found${NC}"
  echo ""
  echo "💡 Actions to take:"
  echo "  1. Review the full report: $REPORT_HTML"
  echo "  2. Update affected dependencies in pom.xml"
  echo "  3. If false positive, add to owasp-suppressions.xml"
  echo "  4. Re-run this script to verify fixes"
  exit 1
fi

echo "${YELLOW}⚠️  Warning: MEDIUM or LOW vulnerabilities found${NC}"
echo "Consider updating affected dependencies or suppressing false positives."
exit 0
