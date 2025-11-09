# FWK-003: Public API Stabilization

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** -  
**Category:** CORE as Framework

---

## 📖 User Story

**As a vendor**,  
I want stable public APIs with semver guarantees,  
So that my modules don't break on CORE upgrades.

---

## 🎯 Acceptance Criteria

- ⏳ Define public API interfaces (what vendors can use)
- ⏳ Document breaking change policy
- ⏳ Deprecation warnings (mark @Deprecated, remove in next major)
- ⏳ API changelog per release
- ⏳ Integration tests for API stability

---

## 💻 Implementation

### Public API Interfaces

**File:** `core-api/src/main/java/cz/muriel/core/api/ModuleRegistry.java`

```java
package cz.muriel.core.api;

/**
 * Public API for module registration.
 * 
 * <p><strong>Stability:</strong> STABLE
 * <p><strong>Since:</strong> 1.0.0
 * 
 * @see ModuleManifest
 */
public interface ModuleRegistry {
    
    /**
     * Register a new module.
     * 
     * @param manifest module manifest
     * @return module ID
     * @since 1.0.0
     */
    String registerModule(ModuleManifest manifest);
    
    /**
     * Enable module for tenant.
     * 
     * @param moduleId module ID
     * @param tenantId tenant ID
     * @since 1.0.0
     */
    void enableModule(String moduleId, String tenantId);
    
    /**
     * Get module information.
     * 
     * @param moduleId module ID
     * @return module info
     * @since 1.0.0
     */
    Optional<ModuleInfo> getModule(String moduleId);
}
```

### Deprecation Policy

```java
/**
 * @deprecated Use {@link #registerModule(ModuleManifest)} instead.
 * Will be removed in version 2.0.0.
 * @since 1.0.0
 */
@Deprecated(since = "1.5.0", forRemoval = true)
public String registerModuleOld(String manifestJson) {
    // Old implementation
}
```

---

## 📖 API Changelog

**File:** `API_CHANGELOG.md`

```markdown
# API Changelog

## Version 1.1.0 (2026-02-01)

### Added
- `ModuleRegistry.disableModule()`
- `MetamodelExtension.addRelationship()`

### Deprecated
- `registerModuleOld()` - Use `registerModule()` instead

### Breaking Changes
- None

## Version 1.0.0 (2026-01-01)

Initial public API release.
```

---

**Last Updated:** 9. listopadu 2025
