# INF-001: Manifest Validator

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** MOD-001  
**Category:** Infrastructure

---

## 📖 User Story

**As a platform**,  
I want to validate module manifests before loading,  
So that invalid modules don't crash the system.

---

## 🎯 Acceptance Criteria

- ⏳ JSON Schema validation (required fields, types)
- ⏳ Dependency validation (required modules exist)
- ⏳ Namespace collision detection (`ivg.User` vs `core.User`)
- ⏳ Version constraint validation (semver format)
- ⏳ Validation report (warnings, errors, blocking issues)

---

## 💻 Implementation

### Validator Service

```java
@Service
public class ManifestValidator {
    
    private static final Logger log = LoggerFactory.getLogger(ManifestValidator.class);
    
    private final ObjectMapper objectMapper;
    private final JsonSchema manifestSchema;
    private final ModuleRepository moduleRepository;
    
    @PostConstruct
    public void init() {
        // Load JSON Schema from resources
        try (InputStream schemaStream = getClass().getResourceAsStream("/schemas/module-manifest.schema.json")) {
            JsonNode schemaNode = objectMapper.readTree(schemaStream);
            manifestSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load manifest schema", e);
        }
    }
    
    /**
     * Validate module manifest.
     * 
     * @param manifestJson raw JSON manifest
     * @return validation result with errors/warnings
     */
    public ValidationResult validate(String manifestJson) {
        ValidationResult result = new ValidationResult();
        
        try {
            JsonNode manifestNode = objectMapper.readTree(manifestJson);
            
            // 1. JSON Schema validation
            Set<ValidationMessage> schemaErrors = manifestSchema.validate(manifestNode);
            if (!schemaErrors.isEmpty()) {
                schemaErrors.forEach(msg -> 
                    result.addError("Schema validation failed: " + msg.getMessage())
                );
                return result; // Blocking errors, stop here
            }
            
            ModuleManifest manifest = objectMapper.treeToValue(manifestNode, ModuleManifest.class);
            
            // 2. Dependency validation
            validateDependencies(manifest, result);
            
            // 3. Namespace collision detection
            validateNamespaces(manifest, result);
            
            // 4. Version constraint validation
            validateVersionConstraints(manifest, result);
            
            // 5. Entity validation (no duplicates)
            validateEntities(manifest, result);
            
            log.info("Manifest validation completed: {} errors, {} warnings", 
                result.getErrors().size(), result.getWarnings().size());
            
        } catch (Exception e) {
            log.error("Validation error", e);
            result.addError("Failed to parse manifest: " + e.getMessage());
        }
        
        return result;
    }
    
    private void validateDependencies(ModuleManifest manifest, ValidationResult result) {
        Map<String, String> requires = manifest.getRequires();
        if (requires == null) return;
        
        requires.forEach((depModuleId, versionConstraint) -> {
            // Check if dependency module exists
            Optional<ModuleInfo> depModule = moduleRepository.findByModuleId(depModuleId);
            if (depModule.isEmpty()) {
                result.addError("Dependency not found: " + depModuleId);
            } else if (depModule.get().getStatus() != ModuleStatus.ENABLED) {
                result.addWarning("Dependency not enabled: " + depModuleId);
            }
            
            // Validate semver constraint format
            if (!isValidSemverConstraint(versionConstraint)) {
                result.addError("Invalid version constraint: " + versionConstraint + 
                    " (expected format: '>=1.0.0', '^2.0.0', etc.)");
            }
        });
    }
    
    private void validateNamespaces(ModuleManifest manifest, ValidationResult result) {
        String moduleNamespace = manifest.getId(); // e.g., "helpdesk"
        
        // Check if namespace already used by another module
        List<ModuleInfo> existingModules = moduleRepository.findAll();
        
        manifest.getProvides().getEntities().forEach(entity -> {
            String fullName = entity.getNamespace() + "." + entity.getName();
            
            // Check collision with existing entities
            existingModules.forEach(existing -> {
                existing.getManifest().getProvides().getEntities().forEach(existingEntity -> {
                    String existingFullName = existingEntity.getNamespace() + "." + existingEntity.getName();
                    
                    if (fullName.equals(existingFullName)) {
                        result.addError("Entity name collision: " + fullName + 
                            " already exists in module " + existing.getModuleId());
                    }
                });
            });
        });
    }
    
    private void validateVersionConstraints(ModuleManifest manifest, ValidationResult result) {
        Map<String, String> requires = manifest.getRequires();
        if (requires == null) return;
        
        requires.forEach((depModuleId, constraint) -> {
            try {
                // Parse semver constraint
                Range range = Range.valueOf(constraint);
                
                // Check if constraint is satisfiable
                Optional<ModuleInfo> depModule = moduleRepository.findByModuleId(depModuleId);
                if (depModule.isPresent()) {
                    Version depVersion = Version.valueOf(depModule.get().getVersion());
                    
                    if (!range.isSatisfiedBy(depVersion)) {
                        result.addError("Version constraint not satisfied: " + 
                            depModuleId + " " + constraint + 
                            " (installed: " + depVersion + ")");
                    }
                }
            } catch (Exception e) {
                result.addError("Invalid semver constraint: " + constraint);
            }
        });
    }
    
    private void validateEntities(ModuleManifest manifest, ValidationResult result) {
        Set<String> entityNames = new HashSet<>();
        
        manifest.getProvides().getEntities().forEach(entity -> {
            String fullName = entity.getNamespace() + "." + entity.getName();
            
            // Check for duplicates within same manifest
            if (!entityNames.add(fullName)) {
                result.addError("Duplicate entity in manifest: " + fullName);
            }
            
            // Validate entity name format
            if (!entity.getName().matches("[A-Z][a-zA-Z0-9]*")) {
                result.addWarning("Entity name should be PascalCase: " + entity.getName());
            }
        });
    }
    
    private boolean isValidSemverConstraint(String constraint) {
        return constraint.matches("^(>=|>|<=|<|\\^|~)?\\d+\\.\\d+\\.\\d+$");
    }
}
```

---

## 📊 Validation Result

```java
@Data
public class ValidationResult {
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    
    public void addError(String message) {
        errors.add(message);
    }
    
    public void addWarning(String message) {
        warnings.add(message);
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
```

---

## 🧪 Testing

```java
@SpringBootTest
class ManifestValidatorTest {
    
    @Autowired
    private ManifestValidator validator;
    
    @Test
    void shouldRejectInvalidSchema() {
        String invalidManifest = """
        {
            "name": "test-module"
            // Missing required field: "id"
        }
        """;
        
        ValidationResult result = validator.validate(invalidManifest);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.contains("required")));
    }
    
    @Test
    void shouldDetectNamespaceCollision() {
        // Given: Existing module with entity "core.User"
        // When: New module tries to create "core.User"
        String manifest = """
        {
            "id": "new-module",
            "provides": {
                "entities": [{
                    "namespace": "core",
                    "name": "User"
                }]
            }
        }
        """;
        
        ValidationResult result = validator.validate(manifest);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.contains("collision")));
    }
}
```

---

## 📊 Success Metrics

- Validation time: <100ms per manifest
- False positives: <5%
- Collision detection: 100%

---

**Last Updated:** 9. listopadu 2025
