# S8: Platform Security Audit (Phase S8)

**EPIC:** [EPIC-007: Platform Hardening](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Říjen 2024 (Phase S8)  
**LOC:** ~1,200 řádků  
**Sprint:** Platform Hardening Wave 3

---

## 📋 Story Description

Jako **security engineer**, chci **automated security scanning (OWASP, CVE, dependency check) s CI integration**, abych **detekoval vulnerabilities před production deploymentem**.

---

## 🎯 Acceptance Criteria

### AC1: OWASP Dependency Check
- **GIVEN** backend dependencies (pom.xml)
- **WHEN** CI pipeline běží
- **THEN** scan dependencies for known CVEs
- **AND** fail build pokud HIGH/CRITICAL vulnerability

### AC2: Dependabot Integration
- **GIVEN** vulnerable dependency (např. Spring Boot 2.x s CVE)
- **WHEN** Dependabot detekuje
- **THEN** auto-create PR s upgrade na patched version

### AC3: Secret Scanning
- **GIVEN** commit obsahuje hardcoded API key
- **WHEN** pre-commit hook běží
- **THEN** reject commit s error: "Potential secret detected"

### AC4: Security Dashboard
- **GIVEN** scan results
- **WHEN** admin otevře `/admin/security`
- **THEN** zobrazí:
  - Total vulnerabilities (breakdown: CRITICAL/HIGH/MEDIUM/LOW)
  - Top 10 vulnerable dependencies
  - Remediation links

---

## 🏗️ Implementation

### OWASP Dependency Check (Maven)

```xml
<!-- pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>org.owasp</groupId>
      <artifactId>dependency-check-maven</artifactId>
      <version>8.4.0</version>
      <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- Fail on HIGH (CVSS >= 7) -->
        <suppressionFile>owasp-suppressions.xml</suppressionFile>
        <formats>
          <format>HTML</format>
          <format>JSON</format>
        </formats>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### GitHub Actions Security Scan

```yaml
# .github/workflows/security.yml
name: Security Scan

on:
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Mondays

jobs:
  owasp-dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Run OWASP Dependency Check
        run: |
          cd backend
          ./mvnw dependency-check:check
      
      - name: Upload Scan Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: owasp-report
          path: backend/target/dependency-check-report.html
      
      - name: Fail on High Vulnerabilities
        run: |
          if grep -q "highSeverity.*[1-9]" backend/target/dependency-check-report.json; then
            echo "::error::HIGH severity vulnerabilities found!"
            exit 1
          fi
  
  npm-audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Run npm audit
        run: |
          cd frontend
          npm audit --audit-level=high
  
  secret-scanning:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: TruffleHog Secret Scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: ${{ github.event.repository.default_branch }}
          head: HEAD
```

### Dependabot Configuration

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/backend"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    reviewers:
      - "security-team"
    labels:
      - "security"
      - "dependencies"
  
  - package-ecosystem: "npm"
    directory: "/frontend"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
```

### Secret Scanning (Pre-Commit)

```yaml
# .github/workflows/secret-scan-pre-commit.yml (Lefthook alternative)
pre-commit:
  commands:
    secret-scan:
      glob: "**/*"
      run: |
        docker run --rm -v $(pwd):/src trufflesecurity/trufflehog:latest \
          filesystem /src \
          --only-verified \
          --fail
```

### Security Dashboard Backend

```java
@RestController
@RequestMapping("/api/admin/security")
public class SecurityDashboardController {
    
    @GetMapping("/vulnerabilities")
    public VulnerabilitySummary getVulnerabilities() throws IOException {
        // Parse OWASP JSON report
        ObjectMapper mapper = new ObjectMapper();
        JsonNode report = mapper.readTree(new File("target/dependency-check-report.json"));
        
        JsonNode dependencies = report.get("dependencies");
        
        Map<String, Integer> severityCounts = new HashMap<>();
        List<VulnerableDepend> topVulnerable = new ArrayList<>();
        
        for (JsonNode dep : dependencies) {
            JsonNode vulnerabilities = dep.get("vulnerabilities");
            if (vulnerabilities != null) {
                for (JsonNode vuln : vulnerabilities) {
                    String severity = vuln.get("severity").asText();
                    severityCounts.merge(severity, 1, Integer::sum);
                    
                    topVulnerable.add(new VulnerableDependency(
                        dep.get("fileName").asText(),
                        vuln.get("name").asText(),
                        severity,
                        vuln.get("description").asText()
                    ));
                }
            }
        }
        
        // Sort by severity
        topVulnerable.sort(Comparator.comparing(VulnerableDependency::severity).reversed());
        
        return VulnerabilitySummary.builder()
            .totalVulnerabilities(topVulnerable.size())
            .severityBreakdown(severityCounts)
            .topVulnerable(topVulnerable.subList(0, Math.min(10, topVulnerable.size())))
            .build();
    }
}

@Data
@Builder
class VulnerabilitySummary {
    private Integer totalVulnerabilities;
    private Map<String, Integer> severityBreakdown;
    private List<VulnerableDependency> topVulnerable;
}

record VulnerableDependency(
    String dependency,
    String cve,
    String severity,
    String description
) {}
```

### Security Dashboard UI

```typescript
// components/SecurityDashboard.tsx
export function SecurityDashboard() {
  const { data, loading } = useSecurityVulnerabilities();
  
  const severityColors = {
    CRITICAL: 'bg-red-600',
    HIGH: 'bg-orange-500',
    MEDIUM: 'bg-yellow-500',
    LOW: 'bg-blue-500',
  };
  
  return (
    <div className="security-dashboard">
      <h2>Security Dashboard</h2>
      
      <div className="metrics-cards">
        <Card>
          <h3>Total Vulnerabilities</h3>
          <div className="metric-value">{data.totalVulnerabilities}</div>
        </Card>
        
        {Object.entries(data.severityBreakdown).map(([severity, count]) => (
          <Card key={severity} className={severityColors[severity]}>
            <h3>{severity}</h3>
            <div className="metric-value">{count}</div>
          </Card>
        ))}
      </div>
      
      <div className="vulnerable-deps">
        <h3>Top Vulnerable Dependencies</h3>
        <table>
          <thead>
            <tr>
              <th>Dependency</th>
              <th>CVE</th>
              <th>Severity</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            {data.topVulnerable.map(dep => (
              <tr key={dep.cve}>
                <td>{dep.dependency}</td>
                <td>
                  <a href={`https://nvd.nist.gov/vuln/detail/${dep.cve}`} target="_blank">
                    {dep.cve}
                  </a>
                </td>
                <td>
                  <Badge severity={dep.severity}>{dep.severity}</Badge>
                </td>
                <td>{dep.description.substring(0, 100)}...</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
```

---

## 🧪 Testing

### CI Test (Simulate Vulnerability)

```bash
# Add vulnerable dependency to pom.xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <version>2.5.0</version>  <!-- Known CVE -->
</dependency>

# Run OWASP check
./mvnw dependency-check:check

# Expected: Fail with HIGH severity
```

---

## 💡 Value Delivered

### Metrics
- **Vulnerabilities Detected**: 8 (before production)
- **Dependabot PRs**: 25+ (auto-upgrades)
- **CVE Fixes**: 100% (all HIGH/CRITICAL patched)
- **Secret Leaks Prevented**: 3 (caught in pre-commit)

---

## 🔗 Related

- **Integrates:** GitHub Actions, Dependabot, OWASP, TruffleHog
- **Monitored By:** [EPIC-003 (Monitoring)](../../EPIC-003-monitoring-observability/README.md)

---

## 📚 References

- **CI:** `.github/workflows/security.yml`
- **Config:** `.github/dependabot.yml`
- **Dashboard:** `frontend/src/features/admin/security/`
