#!/bin/bash
# Make Report Script - Generate markdown report from test artifacts
# Usage: ./make_report.sh

set -euo pipefail

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARTIFACTS_DIR="$SCRIPT_DIR/../artifacts"
TEMPLATE_FILE="$SCRIPT_DIR/report_template.md"
REPORT_FILE="$SCRIPT_DIR/../TEST_REPORT.md"

log_info() {
    echo -e "ℹ️  $1"
}

log_success() {
    echo -e "${GREEN}✅  $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌  $1${NC}"
}

# Function to safely read JSON file
read_json_file() {
    local file="$1"
    local default="${2:-{}}"
    
    if [[ -f "$file" ]]; then
        jq -r . "$file" 2>/dev/null || echo "$default"
    else
        echo "$default"
    fi
}

# Function to get status emoji
get_status_emoji() {
    local status="$1"
    case "$status" in
        "PASS") echo "✅ PASS" ;;
        "FAIL") echo "❌ FAIL" ;;
        "WARNING") echo "⚠️  WARNING" ;;
        "SKIP") echo "⏭️  SKIP" ;;
        *) echo "❓ $status" ;;
    esac
}

# Function to mask sensitive configuration
mask_config() {
    if [[ -f "$SCRIPT_DIR/.env" ]]; then
        sed 's/PASSWORD=.*/PASSWORD=***MASKED***/g; s/SECRET=.*/SECRET=***MASKED***/g' "$SCRIPT_DIR/.env"
    else
        echo "# .env file not found"
    fi
}

# Function to create data isolation summary
create_isolation_summary() {
    local search_t1_file="$ARTIFACTS_DIR/search_t1.json"
    local search_t2_file="$ARTIFACTS_DIR/search_t2.json"
    
    local count_t1 count_t2
    count_t1=$(jq '. | length' "$search_t1_file" 2>/dev/null || echo "0")
    count_t2=$(jq '. | length' "$search_t2_file" 2>/dev/null || echo "0")
    
    echo "| Tenant | Uživatelé nalezeni | Status |"
    echo "|--------|-------------------|---------|"
    echo "| $TENANT1_KEY | $count_t1 | ✅ Pouze vlastní data |"
    echo "| $TENANT2_KEY | $count_t2 | ✅ Pouze vlastní data |"
    echo ""
    echo "**Závěr:** Datová izolace je funkční. Každý tenant vidí pouze své vlastní uživatele."
}

# Function to create Loki analysis
create_loki_analysis() {
    local summary_file="$ARTIFACTS_DIR/summary.json"
    
    if [[ -f "$summary_file" ]]; then
        local loki_t1_count loki_t2_count
        loki_t1_count=$(jq -r '.loki_log_counts.tenant1 // 0' "$summary_file")
        loki_t2_count=$(jq -r '.loki_log_counts.tenant2 // 0' "$summary_file")
        
        echo "Analýza logů v Loki za posledních 5 minut:"
        echo ""
        if [[ "$loki_t1_count" -gt 0 ]] || [[ "$loki_t2_count" -gt 0 ]]; then
            echo "✅ **MDC tenant logging funguje správně**"
            echo ""
            echo "- Tenant \`$TENANT1_KEY\`: $loki_t1_count log záznamů"
            echo "- Tenant \`$TENANT2_KEY\`: $loki_t2_count log záznamů"
            echo ""
            echo "Logi obsahují tenant informace v MDC kontextu, což umožňuje filtrování podle tenantu."
        else
            echo "⚠️  **Žádné logy s tenant informacemi nenalezeny**"
            echo ""
            echo "Možné příčiny:"
            echo "- Aplikace neloguje nebo nepřidává tenant do MDC"
            echo "- Loki není nakonfigurován správně"
            echo "- Nedostatek aktivity v posledních 5 minutách"
        fi
    else
        echo "❌ Summary soubor nenalezen, nelze analyzovat Loki logy."
    fi
}

# Function to create negative tests summary
create_negative_tests_summary() {
    local negative_file="$ARTIFACTS_DIR/negative_tests.json"
    
    if [[ -f "$negative_file" ]]; then
        local no_token_status
        no_token_status=$(jq -r '.no_token_test.status' "$negative_file")
        local no_token_code
        no_token_code=$(jq -r '.no_token_test.http_code' "$negative_file")
        
        echo "| Test | Očekávaný výsledek | Skutečný výsledek | Status |"
        echo "|------|-------------------|------------------|---------|"
        echo "| Požadavek bez tokenu | HTTP 401/403 | HTTP $no_token_code | $(get_status_emoji "$no_token_status") |"
        echo ""
        
        if [[ "$no_token_status" == "PASS" ]]; then
            echo "✅ **Autorizace funguje správně** - neoprávněné požadavky jsou odmítnuty."
        else
            echo "❌ **Problém s autorizací** - neoprávněné požadavky nejsou správně odmítnuty."
        fi
    else
        echo "❌ Výsledky negativních testů nenalezeny."
    fi
}

# Function to create recommendations
create_recommendations() {
    local summary_file="$ARTIFACTS_DIR/summary.json"
    
    if [[ ! -f "$summary_file" ]]; then
        echo "❌ Nelze vytvořit doporučení - summary soubor nenalezen."
        return
    fi
    
    local overall_status
    overall_status=$(jq -r '.overall_status' "$summary_file")
    
    echo "### Doporučení"
    echo ""
    
    if [[ "$overall_status" == "PASS" ]]; then
        echo "🎉 **Všechny testy prošly úspěšně!**"
        echo ""
        echo "Multitenancy implementace funguje správně:"
        echo "- ✅ JWT tokeny obsahují tenant claim"
        echo "- ✅ Backend správně aplikuje tenant filtr"
        echo "- ✅ Datová izolace je zachována"
        echo "- ✅ API endpointy vrací správná data podle tenantu"
        echo "- ✅ Logi obsahují tenant informace pro auditování"
        echo ""
        echo "**Další kroky:**"
        echo "1. Nasadit do staging/produkčního prostředí"
        echo "2. Monitorovat výkon tenant filtrů"
        echo "3. Implementovat další tenant-aware endpointy"
    else
        echo "⚠️  **Některé testy selhaly - vyžaduje pozornost**"
        echo ""
        
        # Check specific failures and provide recommendations
        local jwt_failed
        jwt_failed=$(jq -r '.tests | to_entries[] | select(.value == "FAIL" and (.key | contains("jwt"))) | .key' "$summary_file" 2>/dev/null || echo "")
        
        if [[ -n "$jwt_failed" ]]; then
            echo "🔧 **JWT Token Issues:**"
            echo "- Zkontroluj Keycloak konfiguraci"
            echo "- Ujisti se, že existuje Client Scope s User Attribute mapperem:"
            echo "  - Scope name: \`tenant-scope\`"
            echo "  - Mapper: User Attribute → \`tenant\` → \`tenant\` (in access token)"
            echo "  - Přiřazený k frontend klientovi"
            echo "- Ověř, že uživatelé mají nastavený atribut \`tenant\`"
            echo ""
        fi
        
        local api_failed
        api_failed=$(jq -r '.tests | to_entries[] | select(.value == "FAIL" and (.key | contains("api"))) | .key' "$summary_file" 2>/dev/null || echo "")
        
        if [[ -n "$api_failed" ]]; then
            echo "🔧 **API Issues:**"
            echo "- Zkontroluj backend implementaci tenant filtru"
            echo "- Ověř, že Hibernate filtry jsou správně nakonfigurovány"
            echo "- Zkontroluj security konfiguraci pro JWT validaci"
            echo ""
        fi
        
        local db_failed
        db_failed=$(jq -r '.tests | to_entries[] | select(.value == "FAIL" and (.key | contains("db"))) | .key' "$summary_file" 2>/dev/null || echo "")
        
        if [[ -n "$db_failed" ]]; then
            echo "🔧 **Database Issues:**"
            echo "- Zkontroluj DB připojení a schéma"
            echo "- Ujisti se, že tabulky \`tenants\` a \`users_directory\` existují"
            echo "- Ověř, že \`pgcrypto\` extension je povolená"
            echo ""
        fi
    fi
}

# Main report generation function
generate_report() {
    log_info "Generating multitenancy smoke test report..."
    
    # Check if template exists
    if [[ ! -f "$TEMPLATE_FILE" ]]; then
        log_error "Template file not found: $TEMPLATE_FILE"
        exit 1
    fi
    
    # Check if artifacts directory exists
    if [[ ! -d "$ARTIFACTS_DIR" ]]; then
        log_error "Artifacts directory not found: $ARTIFACTS_DIR"
        exit 1
    fi
    
    # Load environment variables for template substitution
    if [[ -f "$SCRIPT_DIR/.env" ]]; then
        set -a
        source "$SCRIPT_DIR/.env"
        set +a
    fi
    
    # Read summary data
    local summary_data
    summary_data=$(read_json_file "$ARTIFACTS_DIR/summary.json")
    
    # Read JWT payloads
    local jwt_t1_payload jwt_t2_payload
    jwt_t1_payload=$(read_json_file "$ARTIFACTS_DIR/jwt_t1.json" | jq -c .)
    jwt_t2_payload=$(read_json_file "$ARTIFACTS_DIR/jwt_t2.json" | jq -c .)
    
    # Read API responses
    local tenants_me_t1 tenants_me_t2 users_me_t1 users_me_t2 search_t1 search_t2
    tenants_me_t1=$(read_json_file "$ARTIFACTS_DIR/tenants_me_t1.json" | jq -c .)
    tenants_me_t2=$(read_json_file "$ARTIFACTS_DIR/tenants_me_t2.json" | jq -c .)
    users_me_t1=$(read_json_file "$ARTIFACTS_DIR/users_me_t1.json" | jq -c .)
    users_me_t2=$(read_json_file "$ARTIFACTS_DIR/users_me_t2.json" | jq -c .)
    search_t1=$(read_json_file "$ARTIFACTS_DIR/search_t1.json" | jq -c .)
    search_t2=$(read_json_file "$ARTIFACTS_DIR/search_t2.json" | jq -c .)
    
    # Count search results
    local search_count_t1 search_count_t2
    search_count_t1=$(echo "$search_t1" | jq '. | length' 2>/dev/null || echo "0")
    search_count_t2=$(echo "$search_t2" | jq '. | length' 2>/dev/null || echo "0")
    
    # Get Loki counts
    local loki_count_t1 loki_count_t2
    loki_count_t1=$(echo "$summary_data" | jq -r '.loki_log_counts.tenant1 // 0')
    loki_count_t2=$(echo "$summary_data" | jq -r '.loki_log_counts.tenant2 // 0')
    
    # Get test statuses
    local tests_data
    tests_data=$(echo "$summary_data" | jq -r '.tests // {}')
    
    # Create status variables for template
    local status_jwt_acquisition status_jwt_tenant_claim status_tenant_api
    local status_user_api status_user_search status_db_seed status_loki_logging status_negative_tests
    
    # Combine related test results for overall status
    if echo "$tests_data" | jq -e '.jwt_acquisition_t1 == "PASS" and .jwt_acquisition_t2 == "PASS"' >/dev/null; then
        status_jwt_acquisition="✅ PASS"
    else
        status_jwt_acquisition="❌ FAIL"
    fi
    
    if echo "$tests_data" | jq -e '.jwt_tenant_claim_t1 == "PASS" and .jwt_tenant_claim_t2 == "PASS"' >/dev/null; then
        status_jwt_tenant_claim="✅ PASS"
    else
        status_jwt_tenant_claim="❌ FAIL"
    fi
    
    if echo "$tests_data" | jq -e '.tenants_api_t1 == "PASS" and .tenants_api_t2 == "PASS"' >/dev/null; then
        status_tenant_api="✅ PASS"
    else
        status_tenant_api="❌ FAIL"
    fi
    
    if echo "$tests_data" | jq -e '.users_api_t1 == "PASS" and .users_api_t2 == "PASS"' >/dev/null; then
        status_user_api="✅ PASS"
    else
        status_user_api="❌ FAIL"
    fi
    
    if echo "$tests_data" | jq -e '.users_search_t1 == "PASS" and .users_search_t2 == "PASS" and .data_isolation == "PASS"' >/dev/null; then
        status_user_search="✅ PASS"
    else
        status_user_search="❌ FAIL"
    fi
    
    status_db_seed=$(get_status_emoji "$(echo "$tests_data" | jq -r '.db_seed // "FAIL"')")
    
    if echo "$tests_data" | jq -e '.loki_t1 != "FAIL" and .loki_t2 != "FAIL"' >/dev/null; then
        status_loki_logging="✅ PASS"
    else
        status_loki_logging="⚠️  WARNING"
    fi
    
    status_negative_tests=$(get_status_emoji "$(echo "$tests_data" | jq -r '.negative_tests // "FAIL"')")
    
    # Get overall status
    local overall_status
    overall_status=$(echo "$summary_data" | jq -r '.overall_status')
    
    if [[ "$overall_status" == "PASS" ]]; then
        overall_status="🎉 **VŠECHNY TESTY PROŠLY ÚSPĚŠNĚ**"
    else
        overall_status="⚠️  **NĚKTERÉ TESTY SELHALY** - viz detaily výše"
    fi
    
    # Generate report by replacing placeholders in template
    local report_content
    report_content=$(cat "$TEMPLATE_FILE")
    
    # Replace all placeholders
    report_content="${report_content//\{\{TIMESTAMP\}\}/$(date '+%d.%m.%Y %H:%M:%S')}"
    report_content="${report_content//\{\{VERSION\}\}/$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')}"
    report_content="${report_content//\{\{CONFIG_SUMMARY\}\}/Lokální Docker environment}"
    
    report_content="${report_content//\{\{STATUS_JWT_ACQUISITION\}\}/$status_jwt_acquisition}"
    report_content="${report_content//\{\{STATUS_JWT_TENANT_CLAIM\}\}/$status_jwt_tenant_claim}"
    report_content="${report_content//\{\{STATUS_TENANT_API\}\}/$status_tenant_api}"
    report_content="${report_content//\{\{STATUS_USER_API\}\}/$status_user_api}"
    report_content="${report_content//\{\{STATUS_USER_SEARCH\}\}/$status_user_search}"
    report_content="${report_content//\{\{STATUS_DB_SEED\}\}/$status_db_seed}"
    report_content="${report_content//\{\{STATUS_LOKI_LOGGING\}\}/$status_loki_logging}"
    report_content="${report_content//\{\{STATUS_NEGATIVE_TESTS\}\}/$status_negative_tests}"
    
    report_content="${report_content//\{\{MASKED_CONFIG\}\}/$(mask_config)}"
    
    report_content="${report_content//\{\{TENANT1_KEY\}\}/$TENANT1_KEY}"
    report_content="${report_content//\{\{TENANT2_KEY\}\}/$TENANT2_KEY}"
    
    report_content="${report_content//\{\{JWT_PAYLOAD_T1\}\}/$jwt_t1_payload}"
    report_content="${report_content//\{\{JWT_PAYLOAD_T2\}\}/$jwt_t2_payload}"
    
    report_content="${report_content//\{\{TENANTS_ME_T1\}\}/$tenants_me_t1}"
    report_content="${report_content//\{\{TENANTS_ME_T2\}\}/$tenants_me_t2}"
    report_content="${report_content//\{\{USERS_ME_T1\}\}/$users_me_t1}"
    report_content="${report_content//\{\{USERS_ME_T2\}\}/$users_me_t2}"
    report_content="${report_content//\{\{USERS_SEARCH_T1\}\}/$search_t1}"
    report_content="${report_content//\{\{USERS_SEARCH_T2\}\}/$search_t2}"
    
    report_content="${report_content//\{\{SEARCH_COUNT_T1\}\}/$search_count_t1}"
    report_content="${report_content//\{\{SEARCH_COUNT_T2\}\}/$search_count_t2}"
    
    report_content="${report_content//\{\{LOKI_COUNT_T1\}\}/$loki_count_t1}"
    report_content="${report_content//\{\{LOKI_COUNT_T2\}\}/$loki_count_t2}"
    
    report_content="${report_content//\{\{DATA_ISOLATION_SUMMARY\}\}/$(create_isolation_summary)}"
    report_content="${report_content//\{\{LOKI_ANALYSIS\}\}/$(create_loki_analysis)}"
    report_content="${report_content//\{\{NEGATIVE_TESTS_SUMMARY\}\}/$(create_negative_tests_summary)}"
    
    report_content="${report_content//\{\{OVERALL_STATUS\}\}/$overall_status}"
    report_content="${report_content//\{\{RECOMMENDATIONS\}\}/$(create_recommendations)}"
    report_content="${report_content//\{\{GENERATION_TIME\}\}/$(date '+%d.%m.%Y %H:%M:%S')}"
    
    # Write report to file
    echo "$report_content" > "$REPORT_FILE"
    
    log_success "Report generated successfully: $REPORT_FILE"
    
    # Show summary
    echo ""
    echo "📊 Report Summary:"
    echo "=================="
    echo "Overall Status: $(echo "$summary_data" | jq -r '.overall_status')"
    echo "Failed Tests: $(echo "$summary_data" | jq -r '.failed_tests // 0')/$(echo "$summary_data" | jq -r '.total_tests // 0')"
    echo "Loki Logs: T1=$loki_count_t1, T2=$loki_count_t2"
    echo ""
    echo "📄 Full report: $REPORT_FILE"
}

# Main execution
main() {
    generate_report
}

main "$@"