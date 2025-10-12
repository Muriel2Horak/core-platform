# 🔒 Security Incident Response Runbook

**Status:** ✅ Active  
**Last Updated:** 2025-10-12 (S8)  
**Owner:** Security Team

---

## 🎯 Purpose

This runbook provides step-by-step procedures for responding to security incidents, vulnerabilities, and audit findings in the core-platform application.

---

## 📋 Table of Contents

1. [Vulnerability Detection](#vulnerability-detection)
2. [CVE Response Process](#cve-response-process)
3. [Security Incident Types](#security-incident-types)
4. [Escalation Matrix](#escalation-matrix)
5. [Remediation Procedures](#remediation-procedures)
6. [Post-Incident Review](#post-incident-review)

---

## 🔍 Vulnerability Detection

### Automated Scans

Security vulnerabilities are detected through multiple automated scans:

#### OWASP Dependency-Check (Weekly)
- **Trigger:** GitHub Actions workflow (Sundays 2 AM UTC)
- **Scope:** Maven dependencies (backend), npm packages (frontend)
- **Reports:** `backend/target/dependency-check/dependency-check-report.html`
- **Alert Threshold:** CVSS ≥ 7.0 (HIGH/CRITICAL)

#### Trivy Container Scan (Weekly)
- **Trigger:** GitHub Actions workflow (Sundays 2 AM UTC)
- **Scope:** Docker images, filesystem
- **Reports:** SARIF uploaded to GitHub Security tab
- **Alert Threshold:** HIGH/CRITICAL severities

#### Dependabot (Daily)
- **Trigger:** Automatic GitHub scan
- **Scope:** All dependency files (Maven, npm, Docker, GitHub Actions)
- **Alerts:** GitHub Security Advisories → Email + Slack
- **Auto-PRs:** Created for security updates

#### SonarCloud (Per Commit)
- **Trigger:** PR checks, main branch pushes
- **Scope:** Code quality, security hotspots, code smells
- **Reports:** https://sonarcloud.io/dashboard?id=Muriel2Horak_core-platform

---

## 🚨 CVE Response Process

### Severity Classification

| CVSS Score | Severity | Response Time | Action Required |
|------------|----------|---------------|-----------------|
| **9.0-10.0** | CRITICAL | **4 hours** | Immediate patch + emergency deployment |
| **7.0-8.9** | HIGH | **24 hours** | Patch within 1 business day |
| **4.0-6.9** | MEDIUM | **7 days** | Schedule patch in next sprint |
| **0.1-3.9** | LOW | **30 days** | Address during regular maintenance |

### Response Workflow

#### Step 1: Triage (Within 1 hour)

1. **Receive Alert**
   - Dependabot PR created → Slack notification
   - GitHub Security Advisory email
   - Weekly OWASP scan failure

2. **Assess Impact**
   ```bash
   # Check affected dependency
   cd backend
   ./mvnw dependency:tree | grep <affected-artifact>
   
   # Check CVE details
   open https://nvd.nist.gov/vuln/detail/<CVE-ID>
   ```

3. **Determine Exploitability**
   - Is vulnerable code path reachable in our application?
   - Are we using the vulnerable functionality?
   - Are there known public exploits?

4. **Calculate Risk Score**
   ```
   Risk = CVSS Score × Exploitability × Business Impact
   
   Exploitability:
   - Not reachable: 0.1
   - Reachable, no exploits: 0.5
   - Actively exploited: 1.0
   
   Business Impact:
   - Non-production dependency: 0.5
   - Production, non-critical: 0.7
   - Production, critical path: 1.0
   ```

#### Step 2: Remediation (Per severity timeline)

**Option A: Dependency Update (Preferred)**

1. **Check for patched version**
   ```bash
   # Maven
   ./mvnw versions:display-dependency-updates
   
   # npm
   npm outdated
   ```

2. **Update dependency**
   ```xml
   <!-- pom.xml -->
   <dependency>
       <groupId>org.example</groupId>
       <artifactId>vulnerable-lib</artifactId>
       <version>2.0.0-PATCHED</version> <!-- Updated -->
   </dependency>
   ```

3. **Test changes**
   ```bash
   # Run full test suite
   ./mvnw clean verify
   
   # Run security scan again
   ./mvnw org.owasp:dependency-check-maven:check
   ```

4. **Deploy via expedited release**

**Option B: Suppression (If false positive)**

1. **Document justification**
   ```xml
   <!-- backend/owasp-suppressions.xml -->
   <suppress>
       <notes><![CDATA[
       CVE-2023-12345: False positive
       
       Justification:
       - Vulnerable code path not used in our application
       - We only use com.example.SafeClass, not com.example.VulnerableClass
       - Confirmed with security team on 2025-10-12
       
       Reviewed by: @security-lead
       Approved by: @cto
       Expiration: 2025-11-12 (re-evaluate in 30 days)
       ]]></notes>
       <packageUrl regex="true">^pkg:maven/com\.example/vulnerable-lib@.*$</packageUrl>
       <cve>CVE-2023-12345</cve>
   </suppress>
   ```

2. **Get approval** from Security Lead + CTO

3. **Set expiration date** (max 90 days)

**Option C: Workaround (Temporary mitigation)**

1. **Implement compensating controls**
   - Add input validation
   - Restrict network access
   - Enable additional logging/monitoring

2. **Document in SECURITY_CHECKLIST.md**

3. **Create Jira ticket** for permanent fix

#### Step 3: Verification

1. **Re-run security scans**
   ```bash
   # Local scan
   ./scripts/security/check-vulnerabilities.sh
   
   # CI pipeline
   git push → wait for security-scan workflow
   ```

2. **Confirm CVE resolved**
   - OWASP report shows 0 HIGH/CRITICAL
   - Dependabot PR merged/closed
   - GitHub Security Advisory dismissed

3. **Deploy to production**

#### Step 4: Communication

1. **Internal Stakeholders**
   ```markdown
   Subject: [SECURITY] CVE-2023-12345 Resolved
   
   **CVE:** CVE-2023-12345
   **Severity:** HIGH (CVSS 8.5)
   **Affected Component:** org.example:vulnerable-lib:1.0.0
   **Resolution:** Updated to version 2.0.0
   **Deployed:** 2025-10-12 14:30 UTC
   **Verified:** Security scan clean ✅
   
   **Impact:** No known exploitation. Proactive patch applied.
   ```

2. **External Disclosure** (if applicable)
   - Notify customers if data breach suspected
   - Follow GDPR/CCPA reporting requirements
   - Coordinate with PR/Legal teams

---

## 🛡️ Security Incident Types

### Type 1: Dependency Vulnerability

**Example:** OWASP scan finds CVE-2023-12345 in Spring Boot

**Response:** Follow [CVE Response Process](#cve-response-process)

### Type 2: Secrets Exposure

**Example:** GitLeaks/TruffleHog detects API key in commit

**Response:**
1. **Immediate:** Rotate exposed secret (API key, password, token)
2. **Investigate:** Check for unauthorized access in logs
3. **Revoke:** Remove from Git history
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch path/to/secret" \
     --prune-empty --tag-name-filter cat -- --all
   ```
4. **Prevent:** Add secret to `.gitignore` and use Vault/AWS Secrets Manager

### Type 3: Authentication Bypass

**Example:** Keycloak vulnerability allows unauthorized access

**Response:**
1. **Immediate:** Disable affected endpoint or service
2. **Investigate:** Review access logs for suspicious activity
3. **Patch:** Update Keycloak or implement additional auth checks
4. **Audit:** Review all authentication/authorization flows

### Type 4: Data Breach

**Example:** SQL injection allows data exfiltration

**Response:**
1. **Immediate:** Isolate affected system, block attacker IP
2. **Investigate:** Determine scope of data exposure
3. **Legal:** Notify Legal/Compliance teams within 1 hour
4. **Remediate:** Patch vulnerability, restore from backup if needed
5. **Notify:** Affected users (per GDPR requirements)

### Type 5: DDoS Attack

**Example:** Kafka consumer overwhelmed with malicious messages

**Response:**
1. **Immediate:** Enable rate limiting, scale consumers
2. **Investigate:** Identify attack source (IP, user, topic)
3. **Mitigate:** Block attacker, implement DLT circuit breaker
4. **Review:** Kafka retry policies (S7 `@CriticalRetry` thresholds)

---

## 🚦 Escalation Matrix

| Severity | Initial Response | Escalation Level 1 | Escalation Level 2 | Escalation Level 3 |
|----------|------------------|--------------------|--------------------|-------------------|
| **CRITICAL** | On-call Engineer (15min) | Security Lead (30min) | CTO (1 hour) | CEO + Board (4 hours) |
| **HIGH** | On-call Engineer (1 hour) | Security Lead (4 hours) | CTO (24 hours) | - |
| **MEDIUM** | Dev Team (next business day) | Security Lead (3 days) | - | - |
| **LOW** | Dev Team (next sprint) | - | - | - |

### Contact Information

- **On-Call Engineer:** PagerDuty rotation (#oncall Slack)
- **Security Lead:** security-lead@muriel.cz
- **CTO:** cto@muriel.cz
- **Legal/Compliance:** legal@muriel.cz

---

## 🛠️ Remediation Procedures

### Procedure 1: Emergency Patch Deployment

```bash
# 1. Create hotfix branch
git checkout -b hotfix/CVE-2023-12345 main

# 2. Apply patch
vim backend/pom.xml  # Update dependency version

# 3. Test locally
./mvnw clean verify

# 4. Fast-track CI/CD
git commit -m "security: Fix CVE-2023-12345 (CRITICAL)"
git push origin hotfix/CVE-2023-12345

# 5. Create PR with "SECURITY: CRITICAL" label
gh pr create --title "[SECURITY] Fix CVE-2023-12345" --label "security,critical"

# 6. Approve & merge (requires 1 approval from Security Lead)
gh pr merge --auto --squash

# 7. Deploy to production immediately
kubectl rollout restart deployment/backend -n production
```

### Procedure 2: Secret Rotation

```bash
# 1. Generate new secret
NEW_SECRET=$(openssl rand -base64 32)

# 2. Update in Vault/K8s Secret
kubectl create secret generic api-key --from-literal=key=$NEW_SECRET -n production --dry-run=client -o yaml | kubectl apply -f -

# 3. Restart services
kubectl rollout restart deployment/backend -n production

# 4. Revoke old secret in external system (e.g., AWS IAM)
aws iam delete-access-key --access-key-id OLD_KEY_ID
```

### Procedure 3: Database Audit for Breach

```sql
-- Check for suspicious queries (SQL injection attempts)
SELECT * FROM pg_stat_statements
WHERE query LIKE '%--' OR query LIKE '%UNION%' OR query LIKE '%DROP%'
ORDER BY calls DESC;

-- Check for unusual access patterns
SELECT user, COUNT(*), MIN(query_start), MAX(query_start)
FROM pg_stat_activity
GROUP BY user
HAVING COUNT(*) > 1000;

-- Review recent data changes
SELECT * FROM audit_log
WHERE created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

---

## 📝 Post-Incident Review

### Timeline

- **Within 24 hours:** Initial incident report
- **Within 3 days:** Root cause analysis (RCA)
- **Within 7 days:** Preventive measures implemented

### RCA Template

```markdown
# Post-Incident Review: CVE-2023-12345

**Date:** 2025-10-12
**Incident ID:** INC-2025-001
**Severity:** HIGH
**Affected Service:** Backend API

## Timeline

| Time (UTC) | Event |
|------------|-------|
| 02:00 | OWASP scan detects CVE-2023-12345 |
| 02:15 | On-call engineer paged |
| 03:00 | Triage complete, HIGH severity confirmed |
| 04:00 | Patch PR created |
| 06:00 | PR approved and merged |
| 07:30 | Deployed to production |
| 08:00 | Verification complete ✅ |

## Root Cause

Vulnerable version of `com.example:vulnerable-lib:1.0.0` introduced in PR #456 (2025-09-15).
Dependabot alert was created but not reviewed within SLA.

## Impact

- **Users Affected:** 0 (no known exploitation)
- **Data Exposed:** None
- **Downtime:** 0 minutes

## Resolution

Updated to patched version `2.0.0`.

## Preventive Measures

1. **Immediate:**
   - [x] Enable Dependabot auto-merge for security patches
   - [x] Add CVSS threshold alert to Slack

2. **Short-term (1 week):**
   - [ ] Weekly security review meeting
   - [ ] Documented SLA for Dependabot PRs

3. **Long-term (1 month):**
   - [ ] Automated security patch deployment (non-breaking changes)
   - [ ] Security training for all engineers

## Lessons Learned

- Dependabot alerts need faster response
- OWASP suppressions should have expiration dates
- Security scanning should run on every PR
```

---

## 🔗 Related Resources

- **OWASP Top 10:** https://owasp.org/www-project-top-ten/
- **NVD Database:** https://nvd.nist.gov/
- **GitHub Security:** https://github.com/Muriel2Horak/core-platform/security
- **Dependency-Check Docs:** https://jeremylong.github.io/DependencyCheck/
- **SECURITY_CHECKLIST.md:** Complete security checklist

---

**Emergency Contacts:**
- **Security Hotline:** +420 XXX XXX XXX
- **Slack Channel:** #security-incidents
- **On-Call:** PagerDuty rotation

**Review Schedule:** Quarterly review and update
