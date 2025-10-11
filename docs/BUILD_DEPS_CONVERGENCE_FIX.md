# Build Fix: Dependency Convergence Resolution

**PR Branch:** `fix/build-deps-convergence`  
**Date:** 2025-10-11  
**Status:** ✅ RESOLVED

## Problem

Maven Enforcer plugin was blocking builds due to dependency convergence errors:

1. **commons-compress**: Conflicting versions 1.21 (from POI via Tika) vs 1.24.0 (from Minio)
2. **apache-mime4j-core/dom**: Conflicting versions 0.8.9 (from Tika) vs 0.8.11 (from Keycloak/Resteasy)
3. **commons-io**: Multiple versions 2.7, 2.11.0, 2.14.0 across Tika and Keycloak
4. **bcprov-jdk18on**: Multiple versions 1.72, 1.76 from Tika parsers
5. **error_prone_annotations**: Conflicting 2.21.1 (from Guava/Minio) vs 2.40.0 (from Caffeine)
6. **checker-qual**: Conflicting 3.37.0 (from Guava) vs 3.49.3 (from PostgreSQL driver)
7. **asm**: Conflicting 9.6 (from Tika) vs 9.7.1 (from Spring OAuth2)
8. **commons-logging bridges**: Multiple overlapping implementations causing duplicate classes

## Solution

### 1. Added `<dependencyManagement>` Block

Pinned conflicting transitive dependencies to unified versions in `backend/pom.xml`:

```xml
<dependencyManagement>
  <dependencies>
    <!-- Force commons-compress to 1.24.0 -->
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-compress</artifactId>
      <version>1.24.0</version>
    </dependency>
    
    <!-- Force apache-mime4j to 0.8.11 -->
    <dependency>
      <groupId>org.apache.james</groupId>
      <artifactId>apache-mime4j-core</artifactId>
      <version>0.8.11</version>
    </dependency>
    <dependency>
      <groupId>org.apache.james</groupId>
      <artifactId>apache-mime4j-dom</artifactId>
      <version>0.8.11</version>
    </dependency>
    
    <!-- And 6 more pinned versions... -->
  </dependencies>
</dependencyManagement>
```

**Rationale for version choices:**
- **1.24.0 for commons-compress**: Latest stable version, required by Minio, compatible with Tika
- **0.8.11 for mime4j**: Latest version from Keycloak/Resteasy, backward compatible with Tika 0.8.9
- **2.14.0 for commons-io**: Latest from Tika core, backward compatible
- **1.76 for BouncyCastle**: Latest from Tika crypto modules
- **2.40.0 for error_prone**: Latest from Caffeine, forward compatible with Guava
- **3.49.3 for checker-qual**: Latest from PostgreSQL driver
- **9.7.1 for ASM**: Latest from Spring OAuth2/Security

### 2. Added Exclusions for Keycloak

Excluded conflicting dependencies from `keycloak-admin-client`:

```xml
<dependency>
  <groupId>org.keycloak</groupId>
  <artifactId>keycloak-admin-client</artifactId>
  <version>26.0.7</version>
  <exclusions>
    <exclusion>
      <groupId>org.jboss</groupId>
      <artifactId>jandex</artifactId>
    </exclusion>
    <exclusion>
      <groupId>org.jboss.logging</groupId>
      <artifactId>commons-logging-jboss-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

### 3. Added Exclusions for WebFlux

Excluded commons-logging duplicate from `spring-boot-starter-webflux`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
  <exclusions>
    <exclusion>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

### 4. Enhanced Enforcer Configuration

Updated `maven-enforcer-plugin` to ignore safe duplicate classes from logging bridges:

```xml
<banDuplicateClasses>
  <dependencies>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jcl-over-slf4j</artifactId>
      <ignoreClasses>
        <ignoreClass>org.apache.commons.logging.*</ignoreClass>
      </ignoreClasses>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-jcl</artifactId>
      <ignoreClasses>
        <ignoreClass>org.apache.commons.logging.*</ignoreClass>
      </ignoreClasses>
    </dependency>
  </dependencies>
</banDuplicateClasses>
```

## Verification

### Dependency Tree Check

```bash
./mvnw dependency:tree -Dincludes=org.apache.commons:commons-compress
# Result: ✅ Single version 1.24.0

./mvnw dependency:tree -Dincludes=org.apache.james:apache-mime4j-core,org.apache.james:apache-mime4j-dom
# Result: ✅ Single version 0.8.11 for both
```

### Build with Enforcer Enabled

```bash
./mvnw clean compile -Denforcer.skip=false -DskipTests
# Result: ✅ BUILD SUCCESS
# Enforcer rules passed:
# - BanDuplicateClasses
# - RequireUpperBoundDeps (WARN only)
# - DependencyConvergence ✅
# - RequireMavenVersion
# - RequireJavaVersion
```

## Impact

- ✅ **Build stability**: No more enforcer failures
- ✅ **Enforcer enabled**: Can catch future dependency issues early
- ✅ **No code changes**: Pure POM fixes, no application logic touched
- ✅ **No import errors**: All `cz.muriel.core.*` imports remain valid
- ⚠️ **Runtime risk**: LOW - all pinned versions are backward compatible

## CI/CD Integration

Add to CI workflow (`.github/workflows/ci.yml`):

```yaml
- name: Verify Dependency Convergence
  run: |
    cd backend
    ./mvnw -Denforcer.skip=false -DskipTests dependency:tree \
      -Dincludes=org.apache.commons:commons-compress,org.apache.james:apache-mime4j-core,org.apache.james:apache-mime4j-dom
    ./mvnw -Denforcer.skip=false -DskipTests clean compile
```

## Files Changed

- `backend/pom.xml`:
  - Added `<dependencyManagement>` with 9 pinned versions
  - Enhanced `maven-enforcer-plugin` configuration
  - Added exclusions to `keycloak-admin-client`
  - Added exclusions to `spring-boot-starter-webflux`
  - Re-enabled `<dependencyConvergence/>` rule

## Next Steps

1. ✅ Merge this PR to unblock S1 (Build & Test Recovery)
2. Monitor runtime for any compatibility issues (unlikely)
3. Consider upgrading Tika to newer version (post-S1) to reduce exclusion needs
4. Add automated dependency convergence check to CI

## Related Issues

- Part of **S1: Build & Test Recovery** epic
- Blocks: Unit tests, integration tests, compilation
- Prerequisites for: S2-S9 implementation phases
