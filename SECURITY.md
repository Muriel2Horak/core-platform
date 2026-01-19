# Security Policy

## Supported Versions

# Security Policy

## Supported Versions

Core Platform is currently in active development. Security updates are provided for:

| Version | Supported          | Notes |
| ------- | ------------------ | ----- |
| main    | :white_check_mark: | Latest development version |
| < 1.0.0 | :white_check_mark: | Pre-release, active security fixes |

**Note:** This project is in active development. Once v1.0.0 is released, we will maintain security updates for the latest stable release and the previous major version.

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please follow these steps:

### 🔒 Private Disclosure (Recommended)

For sensitive security issues, please report privately:

1. **GitHub Security Advisory:** Use [GitHub's private vulnerability reporting](https://github.com/Muriel2Horak/core-platform/security/advisories/new)
2. **Email:** Send details to [martin.horak@muriel.cz](mailto:martin.horak@muriel.cz) with subject "SECURITY: core-platform"

### 📝 What to Include

Please provide:
- Description of the vulnerability
- Steps to reproduce
- Affected versions/components
- Potential impact
- Suggested fix (if available)

### ⏱️ Response Timeline

- **Initial Response:** Within 48 hours
- **Status Updates:** Every 7 days until resolved
- **Fix Timeline:** Critical issues within 7 days, others within 30 days

### 🎖️ Recognition

We appreciate responsible disclosure:
- Security researchers will be credited in release notes (if desired)
- We follow coordinated disclosure practices
- Public disclosure after fix is available

### 🚫 Out of Scope

The following are **NOT** considered vulnerabilities:
- Issues in development/demo environments
- Social engineering attacks
- Physical attacks on infrastructure
- Denial of Service (DoS) attacks on demo instances
- Issues requiring physical access to servers

## Security Best Practices

When deploying Core Platform:

1. **Never commit secrets** - Use environment variables and secrets management
2. **Keep dependencies updated** - Run `make verify` regularly
3. **Enable HTTPS** - Always use TLS in production
4. **Database security** - Use separate credentials per service (see `DB_SEPARATE_USERS_PLAN.md`)
5. **Regular audits** - Review `SECURITY_CONFIG_AUDIT.md` for security configuration

## Security Features

- JWT-based authentication via Keycloak
- Multi-tenant isolation with separate database schemas
- CORS protection
- SQL injection prevention (Hibernate/JPA)
- Secrets scanning (gitleaks)
- Dependency vulnerability scanning (OWASP Dependency-Check)
- Static analysis (CodeQL, SonarQube)
- Container security scanning

For more details, see our [Security Configuration Audit](SECURITY_CONFIG_AUDIT.md).
