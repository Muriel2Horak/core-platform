# Security Checklist - Reporting Module

This checklist maps reporting module security controls to **OWASP ASVS 4.0** requirements.

## ✅ Completed Security Controls

### V1: Architecture, Design and Threat Modeling
- [x] **V1.11.2**: Threat model updated with reporting module attack surface
- [x] **V1.11.3**: Tenant isolation enforced via RLS (Cube.js)

### V2: Authentication
- [x] **V2.1.1**: JWT-based authentication enforced on all endpoints
- [x] **V2.1.2**: 401 Unauthorized returned for missing/invalid tokens
- [x] **V2.9.3**: Token-based authentication (no session state)

### V3: Session Management
- [x] **V3.2.1**: Stateless session (JWT tokens)
- [x] **V3.2.3**: No session fixation risk (no server-side sessions)

### V4: Access Control
- [x] **V4.1.1**: Per-tenant RLS enforced at database level (Cube.js)
- [x] **V4.1.2**: Security context extracted from JWT (CubeSecurityContext)
- [x] **V4.1.5**: Rate limiting per tenant (120 req/min)
- [x] **V4.2.1**: Access control enforced on all requests (SecurityFilter)
- [x] **V4.3.2**: Circuit Breaker prevents cascade failures

### V5: Validation, Sanitization and Encoding
- [x] **V5.1.1**: Input validation on all POST/PATCH endpoints
- [x] **V5.1.2**: Content-Type enforcement (ContentTypeValidationFilter)
- [x] **V5.1.5**: JSON schema validation (@Valid annotations)
- [x] **V5.3.1**: Output encoding (Spring Boot auto-escaping)

### V7: Error Handling and Logging
- [x] **V7.1.1**: Generic error messages (no stack traces to clients)
- [x] **V7.1.2**: Error details logged server-side only
- [x] **V7.2.1**: Sensitive data redacted from logs (LogRedactor)
- [x] **V7.3.1**: Audit log for all entity updates (updated_by, updated_at)
- [x] **V7.3.4**: Timestamp in UTC for all log entries

### V8: Data Protection
- [x] **V8.1.1**: Sensitive data encrypted in transit (HTTPS)
- [x] **V8.1.6**: Cache-Control: no-store for sensitive endpoints
- [x] **V8.2.1**: Database encryption at rest (PostgreSQL TDE)
- [x] **V8.3.4**: Tenant data isolated (RLS enforcement)

### V9: Communications Security
- [x] **V9.1.1**: HTTPS enforced (HSTS header)
- [x] **V9.1.2**: TLS 1.2+ only
- [x] **V9.2.1**: Strong cipher suites configured

### V10: Malicious Code
- [x] **V10.3.1**: Dependency scanning (OWASP Dependency-Check)
- [x] **V10.3.2**: npm audit for frontend dependencies
- [x] **V10.3.3**: Trivy container scanning

### V11: Business Logic
- [x] **V11.1.4**: Circuit Breaker prevents resource exhaustion
- [x] **V11.1.5**: Query deduplication (Single-Flight pattern)
- [x] **V11.1.8**: Optimistic locking for concurrent updates (If-Match)

### V12: Files and Resources
- [x] **V12.5.2**: File upload validation (Content-Type check)
- [x] **V12.6.1**: No directory traversal in API endpoints

### V13: API and Web Service
- [x] **V13.1.1**: RESTful API design with proper HTTP verbs
- [x] **V13.1.3**: JWT authentication on all API endpoints
- [x] **V13.2.1**: Rate limiting (120 req/min per tenant)
- [x] **V13.2.6**: 429 Too Many Requests with Retry-After header
- [x] **V13.3.1**: Security headers on all responses
- [x] **V13.4.2**: No sensitive data in URLs

### V14: Configuration
- [x] **V14.1.3**: Automated security testing in CI/CD
- [x] **V14.2.1**: All dependencies up to date
- [x] **V14.2.3**: Performance monitoring (Grafana dashboard)
- [x] **V14.4.3**: Security headers validated in tests
- [x] **V14.5.3**: HTTP security headers configured

---

## 🔒 Security Headers Checklist

All headers validated in `SecurityHeadersFilterIT.java`:

- [x] **Content-Security-Policy**: `default-src 'self'; script-src 'self'; frame-ancestors 'none'`
- [x] **X-Content-Type-Options**: `nosniff`
- [x] **X-Frame-Options**: `DENY`
- [x] **X-XSS-Protection**: `1; mode=block`
- [x] **Strict-Transport-Security**: `max-age=31536000` (1 year)
- [x] **Referrer-Policy**: `strict-origin-when-cross-origin`
- [x] **Cache-Control**: `no-store` (sensitive endpoints)

---

## 🛡️ OWASP Top 10 2021 Coverage

### A01:2021 - Broken Access Control
- [x] RLS enforcement (Cube.js tenant isolation)
- [x] JWT authentication on all endpoints
- [x] Security context validation (CubeSecurityContext)

### A02:2021 - Cryptographic Failures
- [x] HTTPS enforced (HSTS)
- [x] TLS 1.2+ only
- [x] Secure cookie flags (HttpOnly, Secure, SameSite)

### A03:2021 - Injection
- [x] Parameterized queries (jOOQ)
- [x] Input validation (@Valid)
- [x] Content-Type enforcement

### A04:2021 - Insecure Design
- [x] Circuit Breaker pattern
- [x] Rate limiting
- [x] Query deduplication

### A05:2021 - Security Misconfiguration
- [x] Security headers configured
- [x] Error messages sanitized
- [x] Default credentials removed

### A06:2021 - Vulnerable and Outdated Components
- [x] OWASP Dependency-Check (CI)
- [x] npm audit (CI)
- [x] Trivy container scan (CI)

### A07:2021 - Identification and Authentication Failures
- [x] JWT token validation
- [x] No session management vulnerabilities
- [x] Authentication required on all endpoints

### A08:2021 - Software and Data Integrity Failures
- [x] Dependency integrity (lock files)
- [x] Git commit signing (optional)
- [x] Container image signing (optional)

### A09:2021 - Security Logging and Monitoring Failures
- [x] Audit logs for all updates
- [x] PII redaction (LogRedactor)
- [x] Prometheus metrics (Grafana)

### A10:2021 - Server-Side Request Forgery (SSRF)
- [x] No user-controlled URLs in backend
- [x] Cube.js URL from config only
- [x] Network isolation (Docker)

---

## 📋 Penetration Testing Preparation

### Pre-Test Checklist
- [ ] Deploy to staging environment
- [ ] Enable debug logging for security events
- [ ] Backup database before test
- [ ] Notify team of planned test window
- [ ] Review firewall rules

### Test Scope
1. **Authentication**: JWT bypass attempts
2. **Authorization**: Tenant isolation validation
3. **Input Validation**: XSS, SQLi, LDAP injection
4. **Rate Limiting**: 429 response validation
5. **Circuit Breaker**: Failure handling
6. **Security Headers**: Header presence and values
7. **HTTPS**: Certificate validation, cipher suites

### Post-Test Actions
- [ ] Review findings with security team
- [ ] Create Jira tickets for HIGH/CRITICAL issues
- [ ] Update owasp-suppressions.xml if needed
- [ ] Re-run automated scans after fixes
- [ ] Update SECURITY_CHECKLIST.md

---

## 🚨 Known Issues & Suppressions

### owasp-suppressions.xml
- CVE-YYYY-XXXX: [Reason for suppression]
- CVE-YYYY-YYYY: [False positive - justification]

### Risk Acceptance
- **Medium Risk**: Cube.js metadata endpoint caching (30s TTL)
  - Mitigated by: Circuit Breaker, Rate Limiting
  - Accepted by: [Name], [Date]

---

## 📞 Security Contact

**Security Issues**: security@muriel.cz  
**Emergency Contact**: +420 XXX XXX XXX  
**PGP Key**: [Link to public key]

---

**Last Updated**: 2025-01-10  
**Next Review**: 2025-04-10 (Quarterly)
