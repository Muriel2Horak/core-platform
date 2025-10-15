# AI Preflight CI Gate

This document describes the AI preflight checks that run in CI to ensure AI hooks quality and META_ONLY compliance.

## 🎯 Purpose

The AI Preflight workflow validates:
1. **AI Schema** - Metamodel AI configuration is valid
2. **Endpoints** - AI REST endpoints work correctly
3. **META_ONLY** - No data values leak through AI context
4. **Monitoring** - Grafana dashboards are valid JSON
5. **Integration** - AI integration tests pass

## 📋 Jobs

### 1. ai-schema-validation

**Purpose:** Validate AI schema classes and metamodel configuration

**Steps:**
- Run `AiSchemaValidatorTest` to validate AI policies
- Check `global-config.yaml` has AI section
- Count entity YAML files with AI configuration

**Pass Criteria:**
- ✅ All schema validation tests pass
- ✅ global-config.yaml contains `ai:` section

### 2. ai-endpoints-smoke

**Purpose:** Smoke test AI REST endpoints and verify META_ONLY mode

**Steps:**
- Build and start backend
- Test `/api/ai/context` endpoint
- Verify META_ONLY (fail if `value` or `rows` fields present)
- Test MCP endpoints (ui_context, wf_context, auth, data_context)
- Test AI metrics endpoint

**Pass Criteria:**
- ✅ Backend starts successfully
- ✅ `/api/ai/context` returns 404 (AI disabled) or 200 (AI enabled)
- ✅ If AI enabled, response contains NO `value` or `rows` fields
- ✅ MCP `data_context` returns 501 NOT_IMPLEMENTED
- ✅ AI metrics are registered in Prometheus

**Critical Check - META_ONLY Enforcement:**
```bash
# This regex fails the build if data values are found
if echo "$CONTEXT" | grep -q '"value"\s*:'; then
  echo "❌ FAIL: META_ONLY violation"
  exit 1
fi
```

### 3. ai-grafana-dashboards

**Purpose:** Validate Grafana AI dashboards

**Steps:**
- Check `ai-overview.json` exists
- Check `ai-ops.json` exists
- Validate JSON syntax with `jq`
- Verify dashboards contain AI metrics

**Pass Criteria:**
- ✅ Both dashboard files exist
- ✅ JSON syntax is valid
- ✅ Dashboards reference `ai_requests_total`, `mcp_calls_total`

### 4. ai-integration-tests

**Purpose:** Run all AI integration tests

**Steps:**
- Start PostgreSQL test database
- Run all `*Ai*IT` integration tests

**Pass Criteria:**
- ✅ All integration tests pass
- ✅ Tests verify RBAC (PLATFORM_ADMIN, OPS, TENANT_ADMIN)
- ✅ Tests verify META_ONLY mode

### 5. summary

**Purpose:** Aggregate results and report overall status

**Pass Criteria:**
- ✅ All previous jobs succeeded

## 🚀 Running Locally

### Run Schema Validation

```bash
cd backend
./mvnw test -Dtest=AiSchemaValidatorTest
```

### Run Endpoints Smoke Test

```bash
# Start backend
cd backend
./mvnw spring-boot:run

# In another terminal, test endpoints
curl -s http://localhost:8080/api/ai/context?routeId=users.list | jq

# Verify META_ONLY (should fail if data values present)
curl -s http://localhost:8080/api/ai/context?routeId=users.list | grep -q '"value"'
echo $?  # Should be 1 (not found)
```

### Run Integration Tests

```bash
cd backend
./mvnw test -Dtest=*Ai*IT
```

### Validate Grafana Dashboards

```bash
jq empty docker/grafana/provisioning/dashboards/ai-overview.json
jq empty docker/grafana/provisioning/dashboards/ai-ops.json
```

## 🔍 Troubleshooting

### Job: ai-schema-validation fails

**Symptom:** AiSchemaValidatorTest fails

**Possible causes:**
- Invalid AI policies (e.g., maxFields > 100)
- Missing required fields in GlobalAiConfig
- Enum validation failure

**Solution:**
- Check test output for specific validation errors
- Verify `global-config.yaml` AI section matches schema
- Run test locally: `./mvnw test -Dtest=AiSchemaValidatorTest`

### Job: ai-endpoints-smoke fails with META_ONLY violation

**Symptom:** Error: `Found 'value' field in response (META_ONLY violation)`

**This is CRITICAL - it means data is leaking through AI context!**

**Solution:**
1. Check which endpoint returned data values
2. Review ContextAssembler.java - ensure no `value` fields
3. Review UiContextService.java - ensure no `rows` with data
4. Run locally and inspect response:
   ```bash
   curl -s http://localhost:8080/api/ai/context?routeId=users.list | jq . > context.json
   grep -n "value" context.json
   ```
5. Fix code to exclude data values
6. Re-run smoke test

### Job: ai-grafana-dashboards fails

**Symptom:** Invalid JSON in dashboard file

**Solution:**
- Validate JSON syntax: `jq empty docker/grafana/provisioning/dashboards/ai-overview.json`
- Check for trailing commas, missing brackets
- Use VSCode JSON formatter

### Job: ai-integration-tests fails

**Symptom:** Integration test failures

**Solution:**
- Check test logs for specific failures
- Run locally: `./mvnw test -Dtest=AiContextControllerIT`
- Verify test database is running
- Check RBAC configuration

## 📊 Metrics

The CI workflow tracks:
- **Build time** for each job
- **Test count** in integration tests
- **META_ONLY violations** (should always be 0)

## 🔗 Related Workflows

- **ci.yml** - Main CI pipeline (runs before deployment)
- **pre-deploy.yml** - Pre-deployment validation (includes E2E tests)
- **e2e.yml** - Full E2E test suite (includes AI E2E tests)

## ✅ Success Criteria

For the AI Preflight workflow to pass:

1. ✅ All schema validation tests pass
2. ✅ No META_ONLY violations detected
3. ✅ All MCP endpoints respond correctly
4. ✅ Grafana dashboards are valid JSON
5. ✅ All integration tests pass

**If any job fails, the PR should not be merged.**

## 🔐 Security Notes

**META_ONLY Mode is Critical:**
- The CI gate MUST fail if any data values leak through AI context
- This protects PII and sensitive data from AI exposure
- Even a single `value` field should fail the build

**Test with AI Enabled and Disabled:**
- Tests run with AI disabled by default (404 expected)
- If AI is enabled, META_ONLY is strictly enforced
- Future: Add explicit AI enabled tests in separate job

---

**Last Updated:** 2025-10-15  
**Workflow File:** `.github/workflows/ai-preflight.yml`
