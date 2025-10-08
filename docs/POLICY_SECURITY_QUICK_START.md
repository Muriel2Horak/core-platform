# 🚀 Policy-Based Security - Quick Start Guide

> **Pro vývojáře:** Jak používat nový PolicyEngine security systém

---

## 📋 **TL;DR - Checklist**

Při vytváření nového REST endpointu:

- [ ] Použij `@PreAuthorize("@policyMethods.canXxx(...)")` místo role checks
- [ ] Přidej `Authentication auth` parametr do metody
- [ ] Implementuj **column projection** pro GET endpointy (skrýt sensitive fields)
- [ ] Implementuj **row filtering** pro LIST endpointy (data scope)
- [ ] Validuj **scope** u POST/PUT/DELETE (self vs tenant vs all)
- [ ] Test permissions s různými rolemi

---

## 🎯 **1. ZÁKLADNÍ POUŽITÍ**

### **Controller Setup:**
```java
@RestController
@RequestMapping("/api/myresource")
@RequiredArgsConstructor
public class MyResourceController {
  
  private final MyResourceService service;
  private final PolicyEngine policyEngine; // ← Inject PolicyEngine
  
  // ... endpoints
}
```

### **Endpoint Security:**
```java
// ✅ GOOD - PolicyEngine-based
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id,
    Authentication auth) { // ← Always add auth parameter
  
  MyResourceDto resource = service.findById(id);
  
  // Column projection
  Set<String> columns = policyEngine.projectColumns(auth, "MyResource", "read", id);
  MyResourceDto projected = filterFields(resource, columns);
  
  return ResponseEntity.ok(projected);
}

// ❌ BAD - Old role-based
@GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<MyResourceDto> get(@PathVariable UUID id) {
  return ResponseEntity.ok(service.findById(id));
}
```

---

## 🔐 **2. @PreAuthorize PATTERNS**

### **CRUD Operations:**
```java
// CREATE
@PostMapping
@PreAuthorize("@policyMethods.canCreate('MyResource')")
public ResponseEntity<MyResourceDto> create(
    @RequestBody MyResourceDto dto, 
    Authentication auth) {
  // ...
}

// READ (single)
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id, 
    Authentication auth) {
  // ...
}

// READ (list)
@GetMapping
@PreAuthorize("@policyMethods.canList('MyResource')")
public ResponseEntity<List<MyResourceDto>> list(Authentication auth) {
  // ...
}

// UPDATE
@PutMapping("/{id}")
@PreAuthorize("@policyMethods.canWrite('MyResource', #id)")
public ResponseEntity<MyResourceDto> update(
    @PathVariable UUID id,
    @RequestBody MyResourceDto dto,
    Authentication auth) {
  // ...
}

// DELETE
@DeleteMapping("/{id}")
@PreAuthorize("@policyMethods.canDelete('MyResource', #id)")
public ResponseEntity<Void> delete(
    @PathVariable UUID id,
    Authentication auth) {
  // ...
}
```

### **Special Operations:**
```java
// ASSIGN (např. role assignment)
@PostMapping("/{id}/assign")
@PreAuthorize("@policyMethods.canAssign('MyResource', #id)")
public ResponseEntity<Void> assign(
    @PathVariable UUID id,
    @RequestBody AssignmentDto dto,
    Authentication auth) {
  // ...
}

// EXECUTE (custom akce)
@PostMapping("/{id}/approve")
@PreAuthorize("@policyMethods.canExecute('MyResource', 'approve', #id)")
public ResponseEntity<Void> approve(
    @PathVariable UUID id,
    Authentication auth) {
  // ...
}
```

---

## 🎨 **3. COLUMN PROJECTION**

### **Basic Pattern:**
```java
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id,
    Authentication auth) {
  
  MyResourceDto resource = service.findById(id);
  
  // 1. Získat povolené columns
  Set<String> allowedColumns = policyEngine.projectColumns(
      auth, "MyResource", "read", id);
  
  // 2. Vyfiltrovat fields
  MyResourceDto projected = new MyResourceDto();
  projected.setId(resource.getId());
  projected.setName(resource.getName());
  
  if (allowedColumns.contains("email")) {
    projected.setEmail(resource.getEmail());
  }
  
  if (allowedColumns.contains("salary")) { // Sensitive field
    projected.setSalary(resource.getSalary());
  }
  
  return ResponseEntity.ok(projected);
}
```

### **Helper Method Pattern:**
```java
private MyResourceDto projectColumns(
    MyResourceDto resource, 
    Authentication auth, 
    String action, 
    UUID id) {
  
  Set<String> allowed = policyEngine.projectColumns(
      auth, "MyResource", action, id);
  
  MyResourceDto projected = new MyResourceDto();
  
  // Always visible
  projected.setId(resource.getId());
  
  // Conditional fields
  if (allowed.contains("name")) projected.setName(resource.getName());
  if (allowed.contains("email")) projected.setEmail(resource.getEmail());
  if (allowed.contains("phone")) projected.setPhone(resource.getPhone());
  if (allowed.contains("salary")) projected.setSalary(resource.getSalary());
  
  return projected;
}

// Použití:
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id,
    Authentication auth) {
  
  MyResourceDto resource = service.findById(id);
  MyResourceDto projected = projectColumns(resource, auth, "read", id);
  return ResponseEntity.ok(projected);
}
```

---

## 🔍 **4. ROW FILTERING**

### **Basic Pattern:**
```java
@GetMapping
@PreAuthorize("@policyMethods.canList('MyResource')")
public ResponseEntity<List<MyResourceDto>> list(Authentication auth) {
  
  // 1. Získat WHERE clause pro filtering
  String whereClause = policyEngine.getRowFilter(auth, "MyResource", "list");
  
  // 2. Aplikovat na query (v service layer)
  List<MyResourceDto> resources = service.findAll(whereClause);
  
  // 3. Column projection pro každý item
  List<MyResourceDto> projected = resources.stream()
      .map(r -> projectColumns(r, auth, "list", r.getId()))
      .collect(Collectors.toList());
  
  return ResponseEntity.ok(projected);
}
```

### **JPA Query Integration:**
```java
// Service layer:
public List<MyResource> findAll(String whereClause) {
  if (whereClause == null || whereClause.equals("1=1")) {
    // No filter - all visible
    return repository.findAll();
  }
  
  // Parse whereClause a aplikovat
  if (whereClause.contains("tenant_id")) {
    String tenantId = extractTenantId(whereClause);
    return repository.findByTenantId(UUID.fromString(tenantId));
  }
  
  if (whereClause.contains("created_by")) {
    String userId = extractUserId(whereClause);
    return repository.findByCreatedBy(userId);
  }
  
  return repository.findAll();
}
```

### **In-Memory Filtering (pro Keycloak data):**
```java
private List<MyResourceDto> applyRowFilter(
    List<MyResourceDto> resources,
    String whereClause,
    Authentication auth) {
  
  if (whereClause == null || whereClause.equals("1=1")) {
    return resources; // No filter
  }
  
  String currentTenant = getCurrentTenant(auth);
  String currentUserId = auth.getName();
  
  return resources.stream()
      .filter(r -> {
        if (whereClause.contains("tenant_id")) {
          return r.getTenantId().equals(currentTenant);
        }
        if (whereClause.contains("id = '${userId}'")) {
          return r.getId().equals(currentUserId);
        }
        return true;
      })
      .collect(Collectors.toList());
}
```

---

## 🎯 **5. SCOPE VALIDATION**

### **Check Before Action:**
```java
@PostMapping
@PreAuthorize("@policyMethods.canCreate('MyResource')")
public ResponseEntity<MyResourceDto> create(
    @RequestBody MyResourceDto dto,
    Authentication auth) {
  
  // Extra kontrola - může vytvářet v tomto context?
  if (!policyEngine.check(auth, "MyResource", "create", null)) {
    throw new AccessDeniedException("Cannot create in this context");
  }
  
  MyResourceDto created = service.create(dto);
  return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

### **Self-Scope Check:**
```java
@PutMapping("/{id}")
@PreAuthorize("@policyMethods.canWrite('MyResource', #id)")
public ResponseEntity<MyResourceDto> update(
    @PathVariable UUID id,
    @RequestBody MyResourceDto dto,
    Authentication auth) {
  
  String currentUserId = auth.getName();
  
  // Pokud má pouze self scope, může editovat jen sebe
  if (id.toString().equals(currentUserId)) {
    // OK - edituje sám sebe
  } else {
    // Kontrola, zda má tenant/all scope
    if (!policyEngine.check(auth, "MyResource", "update", id)) {
      throw new AccessDeniedException("Can only update own resource");
    }
  }
  
  MyResourceDto updated = service.update(id, dto);
  return ResponseEntity.ok(updated);
}
```

---

## 🧪 **6. TESTING**

### **Test Permission Checks:**
```java
@Test
void shouldAllowAdminToReadAllFields() {
  // Arrange
  Authentication adminAuth = createAuthWithRole("ADMIN");
  MyResourceDto resource = createTestResource();
  
  // Act
  Set<String> columns = policyEngine.projectColumns(
      adminAuth, "MyResource", "read", resource.getId());
  
  // Assert
  assertTrue(columns.contains("id"));
  assertTrue(columns.contains("name"));
  assertTrue(columns.contains("email"));
  assertTrue(columns.contains("salary")); // Sensitive field
}

@Test
void shouldHideSensitiveFieldsForUser() {
  // Arrange
  Authentication userAuth = createAuthWithRole("USER");
  MyResourceDto resource = createTestResource();
  
  // Act
  Set<String> columns = policyEngine.projectColumns(
      userAuth, "MyResource", "read", resource.getId());
  
  // Assert
  assertTrue(columns.contains("id"));
  assertTrue(columns.contains("name"));
  assertFalse(columns.contains("salary")); // Hidden!
}
```

### **Test Row Filtering:**
```java
@Test
void shouldFilterByTenantForManager() {
  // Arrange
  Authentication managerAuth = createAuthWithRole("MANAGER", "tenant-123");
  
  // Act
  String whereClause = policyEngine.getRowFilter(
      managerAuth, "MyResource", "list");
  
  // Assert
  assertEquals("tenant_id = 'tenant-123'", whereClause);
}

@Test
void shouldShowOwnDataOnlyForUser() {
  // Arrange
  Authentication userAuth = createAuthWithRole("USER", "tenant-123");
  
  // Act
  String whereClause = policyEngine.getRowFilter(
      userAuth, "MyResource", "list");
  
  // Assert
  assertTrue(whereClause.contains("created_by = '${userId}'"));
}
```

---

## 📚 **7. COMMON PATTERNS**

### **Pattern 1: Public + Protected Fields**
```java
// Všichni vidí public fields, jen někteří protected
Set<String> columns = policyEngine.projectColumns(auth, entity, action, id);

MyDto dto = new MyDto();
// Public (always)
dto.setId(entity.getId());
dto.setName(entity.getName());

// Protected (conditional)
if (columns.contains("email")) dto.setEmail(entity.getEmail());
if (columns.contains("phone")) dto.setPhone(entity.getPhone());
if (columns.contains("salary")) dto.setSalary(entity.getSalary());
```

### **Pattern 2: Data Scope Hierarchy**
```java
// all_tenants > own_tenant > own_data
String whereClause = policyEngine.getRowFilter(auth, entity, "list");

if (whereClause.equals("1=1")) {
  // ADMIN - všechny tenanty
  return repository.findAll();
} else if (whereClause.contains("tenant_id")) {
  // MANAGER - vlastní tenant
  return repository.findByTenantId(tenantId);
} else if (whereClause.contains("created_by")) {
  // USER - vlastní data
  return repository.findByCreatedBy(userId);
}
```

### **Pattern 3: Writable Fields Validation**
```java
// Kontrola, které fieldy může editovat
Set<String> writable = policyEngine.projectColumns(
    auth, "MyResource", "update", id);

if (request.getEmail() != null && !writable.contains("email")) {
  throw new AccessDeniedException("Cannot update email");
}

if (request.getSalary() != null && !writable.contains("salary")) {
  throw new AccessDeniedException("Cannot update salary");
}
```

---

## ⚠️ **COMMON MISTAKES**

### **❌ Mistake 1: Zapomenutý Authentication parameter**
```java
// BAD:
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(@PathVariable UUID id) {
  // Jak získat auth pro column projection? ❌
}

// GOOD:
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id,
    Authentication auth) { // ✅
  // ...
}
```

### **❌ Mistake 2: Žádná column projection**
```java
// BAD - vrací všechny fields včetně sensitive!
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id,
    Authentication auth) {
  return ResponseEntity.ok(service.findById(id)); // ❌ No projection!
}

// GOOD:
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('MyResource', #id)")
public ResponseEntity<MyResourceDto> get(
    @PathVariable UUID id,
    Authentication auth) {
  MyResourceDto resource = service.findById(id);
  MyResourceDto projected = projectColumns(resource, auth, "read", id); // ✅
  return ResponseEntity.ok(projected);
}
```

### **❌ Mistake 3: Žádný row filtering**
```java
// BAD - vrací všechny rows bez ohledu na data scope!
@GetMapping
@PreAuthorize("@policyMethods.canList('MyResource')")
public ResponseEntity<List<MyResourceDto>> list(Authentication auth) {
  return ResponseEntity.ok(service.findAll()); // ❌ No filtering!
}

// GOOD:
@GetMapping
@PreAuthorize("@policyMethods.canList('MyResource')")
public ResponseEntity<List<MyResourceDto>> list(Authentication auth) {
  String whereClause = policyEngine.getRowFilter(auth, "MyResource", "list");
  List<MyResourceDto> resources = service.findAll(whereClause); // ✅
  return ResponseEntity.ok(resources);
}
```

---

## 🎓 **DALŠÍ ZDROJE**

- 📖 [RBAC_TO_ABAC_REFACTOR.md](./RBAC_TO_ABAC_REFACTOR.md) - Kompletní architektura
- 📝 [CONTROLLER_MIGRATION_EXAMPLE.md](./CONTROLLER_MIGRATION_EXAMPLE.md) - Detailní migration example
- 🔧 [PolicyEngine.java](../backend/src/main/java/cz/muriel/core/security/PolicyEngine.java) - Core interface
- 🎨 [PolicyMethods.java](../backend/src/main/java/cz/muriel/core/security/PolicyMethods.java) - SpEL helpers

---

**Happy Coding! 🚀**

_Pokud narazíš na problém, check dokumentaci nebo se zeptej týmu._
