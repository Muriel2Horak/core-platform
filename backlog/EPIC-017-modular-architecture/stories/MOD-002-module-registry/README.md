# MOD-002: Module Registry

**Status:** ⏳ **PENDING**  
**Effort:** 4 dny  
**Priority:** 🔥 HIGH (core modulárního systému)  
**Dependencies:** MOD-001 (Manifest Loader)  
**Category:** Module System

---

## 📖 User Story

**As a platform**,  
I want a runtime module registry to manage installed modules,  
So that I can enable/disable modules, track their state, and enforce lifecycle rules.

---

## 🎯 Acceptance Criteria

- ⏳ Registry tracks all modules (available, installed, enabled, disabled)
- ⏳ API to install/enable/disable/uninstall modules
- ⏳ State transitions validated (can't enable uninstalled module)
- ⏳ Module activation triggers entity registration in metamodel
- ⏳ Module deactivation hides entities (logical, not physical delete)
- ⏳ Dependency checks prevent removing module if others depend on it
- ⏳ Audit log records all state changes (who, when, why)

---

## 📐 Module Lifecycle

```
┌──────────────┐
│  AVAILABLE   │  Manifest loaded, not installed
└──────┬───────┘
       │ install()
       ▼
┌──────────────┐
│ INSTALLING   │  Running migrations, registering entities
└──────┬───────┘
       │ success
       ▼
┌──────────────┐
│  INSTALLED   │  Data migrated, entities registered (but disabled)
└──────┬───────┘
       │ enable()
       ▼
┌──────────────┐
│   ENABLED    │  Active, API accessible, menu visible
└──────┬───────┘
       │ disable()
       ▼
┌──────────────┐
│  DISABLED    │  Inactive, API returns 403, menu hidden
└──────┬───────┘
       │ uninstall()
       ▼
┌──────────────┐
│ UNINSTALLED  │  Removed from registry, data retained
└──────────────┘
```

---

## 💻 Implementation

### API Endpoints

**Controller:** `ModuleController.java`

```java
@RestController
@RequestMapping("/api/admin/modules")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class ModuleController {
    
    private final ModuleRegistry moduleRegistry;
    
    @GetMapping
    public List<ModuleInfo> listModules(@RequestParam(required = false) String status) {
        if (status != null) {
            return moduleRegistry.getModulesByStatus(ModuleStatus.valueOf(status));
        }
        return moduleRegistry.getAllModules();
    }
    
    @GetMapping("/{moduleId}")
    public ModuleInfo getModule(@PathVariable String moduleId) {
        return moduleRegistry.getModule(moduleId)
            .orElseThrow(() -> new ModuleNotFoundException(moduleId));
    }
    
    @PostMapping("/{moduleId}/install")
    public ModuleInfo installModule(@PathVariable String moduleId) {
        return moduleRegistry.installModule(moduleId);
    }
    
    @PostMapping("/{moduleId}/enable")
    public ModuleInfo enableModule(@PathVariable String moduleId) {
        return moduleRegistry.enableModule(moduleId);
    }
    
    @PostMapping("/{moduleId}/disable")
    public ModuleInfo disableModule(@PathVariable String moduleId) {
        return moduleRegistry.disableModule(moduleId);
    }
    
    @DeleteMapping("/{moduleId}")
    public void uninstallModule(@PathVariable String moduleId) {
        moduleRegistry.uninstallModule(moduleId);
    }
}
```

### Service Implementation

**Service:** `ModuleRegistry.java`

```java
@Service
@Transactional
public class ModuleRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);
    
    private final ModuleRepository moduleRepository;
    private final MetamodelEngine metamodelEngine;
    private final WorkflowEngine workflowEngine;
    private final ModuleMigrationRunner migrationRunner;
    private final ModuleAuditLogger auditLogger;
    
    // In-memory cache for fast lookups
    private final Map<String, ModuleInfo> moduleCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        refreshCache();
    }
    
    /**
     * Install module: run migrations, register entities.
     */
    public ModuleInfo installModule(String moduleId) {
        log.info("Installing module: {}", moduleId);
        
        ModuleInfo module = getModule(moduleId)
            .orElseThrow(() -> new ModuleNotFoundException(moduleId));
        
        // 1. Validate state transition
        if (module.getStatus() != ModuleStatus.AVAILABLE) {
            throw new IllegalStateException(
                "Cannot install module in state: " + module.getStatus()
            );
        }
        
        // 2. Check dependencies
        validateDependencies(module);
        
        // 3. Update status
        updateModuleStatus(moduleId, ModuleStatus.INSTALLING);
        
        try {
            // 4. Run database migrations
            migrationRunner.runMigrations(module);
            
            // 5. Register entities in metamodel
            registerEntities(module);
            
            // 6. Register workflows
            registerWorkflows(module);
            
            // 7. Update status to INSTALLED
            updateModuleStatus(moduleId, ModuleStatus.INSTALLED);
            
            // 8. Audit log
            auditLogger.logModuleInstalled(moduleId, getCurrentUser());
            
            log.info("✅ Module installed: {}", moduleId);
            
            return getModule(moduleId).get();
            
        } catch (Exception e) {
            log.error("Failed to install module: {}", moduleId, e);
            updateModuleStatus(moduleId, ModuleStatus.ERROR);
            throw new ModuleInstallationException(moduleId, e);
        }
    }
    
    /**
     * Enable module: make it active for API + UI.
     */
    public ModuleInfo enableModule(String moduleId) {
        log.info("Enabling module: {}", moduleId);
        
        ModuleInfo module = getModule(moduleId)
            .orElseThrow(() -> new ModuleNotFoundException(moduleId));
        
        // 1. Validate state (must be INSTALLED or DISABLED)
        if (module.getStatus() != ModuleStatus.INSTALLED 
            && module.getStatus() != ModuleStatus.DISABLED) {
            throw new IllegalStateException(
                "Cannot enable module in state: " + module.getStatus()
            );
        }
        
        // 2. Check license (if required)
        if (module.isLicenseRequired()) {
            validateLicense(moduleId);
        }
        
        // 3. Enable entities in metamodel
        metamodelEngine.enableEntities(module.getProvidedEntities());
        
        // 4. Enable workflows
        workflowEngine.enableWorkflows(module.getProvidedWorkflows());
        
        // 5. Update status
        updateModuleStatus(moduleId, ModuleStatus.ENABLED);
        
        // 6. Audit log
        auditLogger.logModuleEnabled(moduleId, getCurrentUser());
        
        log.info("✅ Module enabled: {}", moduleId);
        
        return getModule(moduleId).get();
    }
    
    /**
     * Disable module: hide from API + UI (logical delete).
     */
    public ModuleInfo disableModule(String moduleId) {
        log.info("Disabling module: {}", moduleId);
        
        ModuleInfo module = getModule(moduleId)
            .orElseThrow(() -> new ModuleNotFoundException(moduleId));
        
        // 1. Validate state
        if (module.getStatus() != ModuleStatus.ENABLED) {
            throw new IllegalStateException(
                "Cannot disable module in state: " + module.getStatus()
            );
        }
        
        // 2. Check if other modules depend on this
        List<ModuleInfo> dependents = findDependentModules(moduleId);
        if (!dependents.isEmpty()) {
            throw new ModuleDependencyException(
                "Cannot disable module: " + dependents.size() + " modules depend on it"
            );
        }
        
        // 3. Disable entities in metamodel
        metamodelEngine.disableEntities(module.getProvidedEntities());
        
        // 4. Disable workflows
        workflowEngine.disableWorkflows(module.getProvidedWorkflows());
        
        // 5. Update status
        updateModuleStatus(moduleId, ModuleStatus.DISABLED);
        
        // 6. Audit log
        auditLogger.logModuleDisabled(moduleId, getCurrentUser());
        
        log.info("✅ Module disabled: {}", moduleId);
        
        return getModule(moduleId).get();
    }
    
    /**
     * Uninstall module: remove from registry (data retained).
     */
    public void uninstallModule(String moduleId) {
        log.info("Uninstalling module: {}", moduleId);
        
        ModuleInfo module = getModule(moduleId)
            .orElseThrow(() -> new ModuleNotFoundException(moduleId));
        
        // 1. Must be DISABLED first
        if (module.getStatus() == ModuleStatus.ENABLED) {
            throw new IllegalStateException("Disable module before uninstalling");
        }
        
        // 2. Check dependencies
        List<ModuleInfo> dependents = findDependentModules(moduleId);
        if (!dependents.isEmpty()) {
            throw new ModuleDependencyException(
                "Cannot uninstall: " + dependents.size() + " modules depend on it"
            );
        }
        
        // 3. Unregister entities (metamodel keeps them as disabled)
        metamodelEngine.unregisterEntities(module.getProvidedEntities());
        
        // 4. Remove from registry
        moduleRepository.delete(module.getId());
        moduleCache.remove(moduleId);
        
        // 5. Audit log
        auditLogger.logModuleUninstalled(moduleId, getCurrentUser());
        
        log.info("✅ Module uninstalled: {}", moduleId);
    }
    
    // === Helper Methods ===
    
    private void registerEntities(ModuleInfo module) {
        module.getManifest().getProvides().getEntities()
            .forEach(entity -> {
                metamodelEngine.registerEntity(
                    entity.getNamespace() + "." + entity.getName(),
                    entity.getAttributes(),
                    entity.getRelationships()
                );
            });
    }
    
    private void registerWorkflows(ModuleInfo module) {
        module.getManifest().getProvides().getWorkflows()
            .forEach(workflow -> {
                workflowEngine.registerWorkflow(
                    workflow.getName(),
                    workflow.getStates(),
                    workflow.getTransitions()
                );
            });
    }
    
    private void validateDependencies(ModuleInfo module) {
        Map<String, String> requires = module.getManifest().getRequires();
        if (requires == null) return;
        
        requires.forEach((depModuleId, versionConstraint) -> {
            ModuleInfo depModule = getModule(depModuleId)
                .orElseThrow(() -> new ModuleDependencyException(
                    "Dependency not found: " + depModuleId
                ));
            
            if (depModule.getStatus() != ModuleStatus.ENABLED) {
                throw new ModuleDependencyException(
                    "Dependency not enabled: " + depModuleId
                );
            }
            
            // TODO: Version constraint check
        });
    }
    
    private List<ModuleInfo> findDependentModules(String moduleId) {
        return getAllModules().stream()
            .filter(m -> m.getManifest().getRequires() != null)
            .filter(m -> m.getManifest().getRequires().containsKey(moduleId))
            .filter(m -> m.getStatus() == ModuleStatus.ENABLED)
            .collect(Collectors.toList());
    }
    
    private void updateModuleStatus(String moduleId, ModuleStatus newStatus) {
        moduleRepository.updateStatus(moduleId, newStatus);
        refreshCache();
    }
    
    private void refreshCache() {
        moduleCache.clear();
        moduleRepository.findAll().forEach(module -> 
            moduleCache.put(module.getModuleId(), module)
        );
    }
    
    public Optional<ModuleInfo> getModule(String moduleId) {
        return Optional.ofNullable(moduleCache.get(moduleId));
    }
    
    public List<ModuleInfo> getAllModules() {
        return new ArrayList<>(moduleCache.values());
    }
    
    public List<ModuleInfo> getModulesByStatus(ModuleStatus status) {
        return moduleCache.values().stream()
            .filter(m -> m.getStatus() == status)
            .collect(Collectors.toList());
    }
}
```

---

## 🧪 Testing

### Integration Test

```java
@SpringBootTest
@Testcontainers
class ModuleRegistryTest {
    
    @Autowired
    private ModuleRegistry registry;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldInstallAndEnableModule() {
        // Given: Available module
        String moduleId = "task-management";
        
        // When: Install
        ModuleInfo installed = registry.installModule(moduleId);
        
        // Then: Status = INSTALLED
        assertThat(installed.getStatus()).isEqualTo(ModuleStatus.INSTALLED);
        
        // When: Enable
        ModuleInfo enabled = registry.enableModule(moduleId);
        
        // Then: Status = ENABLED
        assertThat(enabled.getStatus()).isEqualTo(ModuleStatus.ENABLED);
    }
    
    @Test
    void shouldPreventDisablingModuleWithDependents() {
        // Given: Module A enabled, Module B depends on A
        registry.installModule("core");
        registry.enableModule("core");
        
        registry.installModule("task-management"); // depends on core
        registry.enableModule("task-management");
        
        // When/Then: Cannot disable core (task-management depends on it)
        assertThatThrownBy(() -> registry.disableModule("core"))
            .isInstanceOf(ModuleDependencyException.class)
            .hasMessageContaining("modules depend on it");
    }
    
    @Test
    void shouldEnforceLifecycleStateTransitions() {
        // Given: Available module
        String moduleId = "helpdesk";
        
        // When/Then: Cannot enable before install
        assertThatThrownBy(() -> registry.enableModule(moduleId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot enable module in state: AVAILABLE");
    }
}
```

---

## 📊 Success Metrics

- **State transition time:** <500ms (install/enable/disable)
- **Cache hit ratio:** >99% (modules rarely change)
- **Dependency validation:** 100% conflicts prevented
- **Audit coverage:** 100% state changes logged

---

## 🚀 Rollout Plan

1. **Repository + Entity** (0.5d)
2. **Registry service** (1.5d)
3. **REST API** (1d)
4. **Testing** (1d)

**Total:** 4 days

---

**Last Updated:** 9. listopadu 2025  
**Status:** ⏳ PENDING
