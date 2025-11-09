# FWK-001: Maven Artifacts Distribution

**Status:** ⏳ **PENDING**  
**Effort:** 4 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** -  
**Category:** CORE as Framework

---

## 📖 User Story

**As a vendor (like Ivigee)**,  
I want to use Core Platform as Maven dependency,  
So that I can build on top of CORE without forking.

---

## 🎯 Acceptance Criteria

- ⏳ Maven artifacts published to Maven Central or private Nexus
- ⏳ Starter POM: `core-platform-starter` (includes all essentials)
- ⏳ Modular JARs: `core-metamodel`, `core-workflow`, `core-rbac`, `core-multitenancy`
- ⏳ Versioning: Semantic versioning (1.0.0, 1.1.0, 2.0.0)
- ⏳ Documentation: JavaDocs published
- ⏳ Bill of Materials (BOM) for version management

---

## 💻 Implementation

### Parent POM

**File:** `pom.xml` (root)

```xml
<project>
    <groupId>cz.muriel.core</groupId>
    <artifactId>core-platform-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>core-starter</module>
        <module>core-metamodel</module>
        <module>core-workflow</module>
        <module>core-rbac</module>
        <module>core-multitenancy</module>
    </modules>
    
    <distributionManagement>
        <repository>
            <id>maven-central</id>
            <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>
</project>
```

### Starter Module

**File:** `core-starter/pom.xml`

```xml
<project>
    <parent>
        <groupId>cz.muriel.core</groupId>
        <artifactId>core-platform-parent</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>core-platform-starter</artifactId>
    <name>Core Platform Starter</name>
    <description>All-in-one dependency for Core Platform</description>
    
    <dependencies>
        <!-- Includes all core modules -->
        <dependency>
            <groupId>cz.muriel.core</groupId>
            <artifactId>core-metamodel</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>cz.muriel.core</groupId>
            <artifactId>core-workflow</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>cz.muriel.core</groupId>
            <artifactId>core-rbac</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>cz.muriel.core</groupId>
            <artifactId>core-multitenancy</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

### Usage Example (Ivigee)

**File:** `ivigee-platform/pom.xml`

```xml
<project>
    <groupId>cz.ivigee</groupId>
    <artifactId>ivigee-platform</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <!-- Core Platform as dependency -->
        <dependency>
            <groupId>cz.muriel.core</groupId>
            <artifactId>core-platform-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
        
        <!-- Ivigee custom modules -->
        <dependency>
            <groupId>cz.ivigee</groupId>
            <artifactId>ivigee-custom-module</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

---

## 📦 Artifact Structure

```
cz.muriel.core:core-platform-parent:1.0.0
├── core-platform-starter:1.0.0        (all-in-one)
├── core-metamodel:1.0.0               (entity engine)
├── core-workflow:1.0.0                (state machine)
├── core-rbac:1.0.0                    (roles, permissions)
└── core-multitenancy:1.0.0            (tenant isolation)
```

---

## 🚀 Publishing Process

### 1. Configure Sonatype Credentials

**File:** `~/.m2/settings.xml`

```xml
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>${env.OSSRH_USERNAME}</username>
            <password>${env.OSSRH_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

### 2. GPG Signing

```bash
# Generate GPG key
gpg --gen-key

# Export public key
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys <KEY_ID>
```

### 3. Maven Deploy

```bash
# Deploy to Maven Central
mvn clean deploy -P release

# Or to private Nexus
mvn clean deploy -DaltDeploymentRepository=nexus::default::https://nexus.muriel.cz/repository/maven-releases/
```

---

## 📖 Documentation

### JavaDocs

```bash
# Generate JavaDocs
mvn javadoc:aggregate

# Publish to GitHub Pages
mvn site:deploy
```

### Usage Guide

**File:** `README.md` in each module

```markdown
# Core Metamodel

## Installation

```xml
<dependency>
    <groupId>cz.muriel.core</groupId>
    <artifactId>core-metamodel</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
@Autowired
private MetamodelEngine metamodel;

// Register custom entity
metamodel.registerEntity("ivg.CustomEntity", attributes, relationships);
```
```

---

## 🧪 Testing

```bash
# Test artifact resolution
mvn dependency:get -Dartifact=cz.muriel.core:core-platform-starter:1.0.0

# Verify checksums
mvn verify
```

---

## 📊 Success Metrics

- Artifacts published: 5 modules
- JavaDoc coverage: >90%
- Download count: Track via Maven Central stats
- Breaking changes: None in minor versions (semver)

---

**Last Updated:** 9. listopadu 2025
