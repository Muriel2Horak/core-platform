# 🔄 Controller Migration Example: UserManagementController

> **Příklad migrace** z RBAC role-based security na ABAC PolicyEngine-based security

---

## 📋 **BEFORE (V1.0 - Role-based)**

```java
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
public class UserManagementController {

  private final KeycloakAdminService keycloakAdminService;

  @GetMapping
  public ResponseEntity<List<UserDto>> searchUsers(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "0") Integer first,
      @RequestParam(defaultValue = "20") Integer max) {

    List<UserDto> users = keycloakAdminService.searchUsers(
        username, email, firstName, lastName, enabled, first, max);

    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUserById(@PathVariable String id) {
    UserDto user = keycloakAdminService.getUserById(id);
    return ResponseEntity.ok(user);
  }

  @PostMapping
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
    UserDto createdUser = keycloakAdminService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserDto> updateUser(
      @PathVariable String id,
      @Valid @RequestBody UserUpdateRequest request) {
    UserDto updatedUser = keycloakAdminService.updateUser(id, request);
    return ResponseEntity.ok(updatedUser);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    keycloakAdminService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}
```

**Problémy:**
- ❌ Hrubé oprávnění na úrovni třídy (všechno nebo nic)
- ❌ Žádné column projection (sensitive fields viditelné pro všechny)
- ❌ Žádné row filtering (všichni uživatelé přístupní)
- ❌ Žádná scope kontrola (self vs tenant vs all)

---

## ✅ **AFTER (V2.0 - PolicyEngine-based)**

```java
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserManagementController {

  private final KeycloakAdminService keycloakAdminService;
  private final PolicyEngine policyEngine;

  /**
   * 🔍 LIST USERS - s row filtering podle data scope
   */
  @GetMapping
  @PreAuthorize("@policyMethods.canList('User')")
  public ResponseEntity<List<UserDto>> searchUsers(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "0") Integer first,
      @RequestParam(defaultValue = "20") Integer max,
      Authentication auth) {

    // Row filtering - filter podle data scope uživatele
    String whereClause = policyEngine.getRowFilter(auth, "User", "list");
    
    List<UserDto> users = keycloakAdminService.searchUsers(
        username, email, firstName, lastName, enabled, first, max);

    // Aplikovat row filter (in-memory pro Keycloak data)
    List<UserDto> filteredUsers = applyRowFilter(users, whereClause, auth);

    // Column projection pro každého uživatele
    return ResponseEntity.ok(
      filteredUsers.stream()
        .map(user -> projectColumns(user, auth, "list"))
        .collect(Collectors.toList())
    );
  }

  /**
   * 🔍 GET USER BY ID - s column projection
   */
  @GetMapping("/{id}")
  @PreAuthorize("@policyMethods.canRead('User', #id)")
  public ResponseEntity<UserDto> getUserById(
      @PathVariable String id,
      Authentication auth) {
    
    UserDto user = keycloakAdminService.getUserById(id);
    
    // Column projection - skrýt sensitive fields podle role
    UserDto projected = projectColumns(user, auth, "read", id);
    
    return ResponseEntity.ok(projected);
  }

  /**
   * 🆕 CREATE USER - s scope kontrolou
   */
  @PostMapping
  @PreAuthorize("@policyMethods.canCreate('User')")
  public ResponseEntity<UserDto> createUser(
      @Valid @RequestBody UserCreateRequest request,
      Authentication auth) {
    
    // Kontrola scope - může vytvářet uživatele v tomto tenantu?
    if (!policyEngine.check(auth, "User", "create", null)) {
      throw new AccessDeniedException("Cannot create users in this tenant");
    }
    
    UserDto createdUser = keycloakAdminService.createUser(request);
    
    // Column projection i pro response
    UserDto projected = projectColumns(createdUser, auth, "read", createdUser.getId());
    
    return ResponseEntity.status(HttpStatus.CREATED).body(projected);
  }

  /**
   * ✏️ UPDATE USER - kontrola, zda může editovat (self vs all)
   */
  @PutMapping("/{id}")
  @PreAuthorize("@policyMethods.canWrite('User', #id)")
  public ResponseEntity<UserDto> updateUser(
      @PathVariable String id,
      @Valid @RequestBody UserUpdateRequest request,
      Authentication auth) {
    
    // Extra kontrola - může editovat jen některé fieldy?
    Set<String> writableColumns = policyEngine.projectColumns(
        auth, "User", "update", id);
    
    // Validace requestu proti writableColumns
    validateUpdateRequest(request, writableColumns);
    
    UserDto updatedUser = keycloakAdminService.updateUser(id, request);
    
    // Column projection pro response
    UserDto projected = projectColumns(updatedUser, auth, "read", id);
    
    return ResponseEntity.ok(projected);
  }

  /**
   * 🗑️ DELETE USER - pouze pokud má delete permission
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("@policyMethods.canDelete('User', #id)")
  public ResponseEntity<Void> deleteUser(
      @PathVariable String id,
      Authentication auth) {
    
    // Extra kontrola - nelze smazat sám sebe
    String currentUserId = auth.getName();
    if (id.equals(currentUserId)) {
      throw new IllegalArgumentException("Cannot delete yourself");
    }
    
    keycloakAdminService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  // =========================================================================
  // HELPER METHODS
  // =========================================================================

  /**
   * Aplikuje column projection na UserDto
   */
  private UserDto projectColumns(
      UserDto user, 
      Authentication auth, 
      String action) {
    return projectColumns(user, auth, action, user.getId());
  }

  private UserDto projectColumns(
      UserDto user, 
      Authentication auth, 
      String action, 
      String userId) {
    
    Set<String> allowedColumns = policyEngine.projectColumns(
        auth, "User", action, userId);
    
    // Vytvořit kopii s pouze povolenými fieldy
    UserDto projected = new UserDto();
    projected.setId(user.getId());
    projected.setUsername(user.getUsername());
    
    if (allowedColumns.contains("email")) {
      projected.setEmail(user.getEmail());
    }
    
    if (allowedColumns.contains("firstName")) {
      projected.setFirstName(user.getFirstName());
    }
    
    if (allowedColumns.contains("lastName")) {
      projected.setLastName(user.getLastName());
    }
    
    if (allowedColumns.contains("phone")) {
      projected.setPhone(user.getPhone());
    }
    
    if (allowedColumns.contains("roles")) {
      projected.setRoles(user.getRoles());
    }
    
    // Sensitive fields - pouze pro adminy
    if (allowedColumns.contains("personalId")) {
      projected.setPersonalId(user.getPersonalId());
    }
    
    if (allowedColumns.contains("salary")) {
      projected.setSalary(user.getSalary());
    }
    
    return projected;
  }

  /**
   * Aplikuje row filter na seznam uživatelů
   */
  private List<UserDto> applyRowFilter(
      List<UserDto> users, 
      String whereClause, 
      Authentication auth) {
    
    if (whereClause == null || whereClause.isEmpty()) {
      return users; // No filter = all visible
    }
    
    // Parse WHERE clause a aplikovat
    // Příklady:
    // - "tenant_id = 'abc123'" → filter podle tenantu
    // - "id = '${userId}'" → pouze vlastní data
    // - "1=1" → všechno viditelné
    
    String currentTenant = getCurrentTenant(auth);
    String currentUserId = auth.getName();
    
    return users.stream()
      .filter(user -> {
        // Implementace podle whereClause
        if (whereClause.contains("tenant_id")) {
          return user.getTenantId().equals(currentTenant);
        }
        if (whereClause.contains("id = '${userId}'")) {
          return user.getId().equals(currentUserId);
        }
        return true; // all_tenants scope
      })
      .collect(Collectors.toList());
  }

  /**
   * Validuje update request proti writableColumns
   */
  private void validateUpdateRequest(
      UserUpdateRequest request, 
      Set<String> writableColumns) {
    
    // Pokud request obsahuje field, který není writable → error
    if (request.getEmail() != null && !writableColumns.contains("email")) {
      throw new AccessDeniedException("Cannot update email field");
    }
    
    if (request.getPhone() != null && !writableColumns.contains("phone")) {
      throw new AccessDeniedException("Cannot update phone field");
    }
    
    if (request.getRoles() != null && !writableColumns.contains("roles")) {
      throw new AccessDeniedException("Cannot update roles field");
    }
    
    // etc.
  }

  private String getCurrentTenant(Authentication auth) {
    if (auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getClaimAsString("tenant");
    }
    throw new IllegalStateException("Cannot extract tenant from auth");
  }
}
```

---

## 🎯 **KLÍČOVÉ ZMĚNY**

### **1. @PreAuthorize s PolicyMethods**
```java
// BEFORE:
@PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")

// AFTER:
@PreAuthorize("@policyMethods.canRead('User', #id)")
@PreAuthorize("@policyMethods.canWrite('User', #id)")
@PreAuthorize("@policyMethods.canDelete('User', #id)")
@PreAuthorize("@policyMethods.canCreate('User')")
@PreAuthorize("@policyMethods.canList('User')")
```

### **2. Column Projection**
```java
Set<String> allowedColumns = policyEngine.projectColumns(auth, "User", "read", userId);

// Příklad výsledku podle role:
// ADMIN:        ["id", "username", "email", "phone", "roles", "personalId", "salary"]
// MANAGER:      ["id", "username", "email", "phone", "roles"]
// USER (self):  ["id", "username", "email", "phone"]
// USER (other): ["id", "username"] // Pouze veřejné info
```

### **3. Row Filtering**
```java
String whereClause = policyEngine.getRowFilter(auth, "User", "list");

// Příklad výsledku podle data scope:
// all_tenants:  "1=1"                      → všechny tenanty
// own_tenant:   "tenant_id = 'abc123'"     → pouze own tenant
// own_data:     "id = '${userId}'"         → pouze vlastní data
```

### **4. Scope Validation**
```java
// Kontrola, zda může vytvářet uživatele v tomto context
if (!policyEngine.check(auth, "User", "create", null)) {
  throw new AccessDeniedException("Cannot create users in this tenant");
}
```

---

## 📊 **PERMISSION RULES EXAMPLE**

### **V permissions.yml (deprecated fallback):**
```yaml
access_policies:
  - entity_type: User
    action: read
    rule:
      type: OR
      rules:
        - type: ROLE
          expression: "hasRole('ADMIN')"
        - type: AND
          rules:
            - type: ROLE
              expression: "hasRole('MANAGER')"
            - type: TENANT
              expression: "sameAsTenant"

column_policies:
  - entity_type: User
    action: read
    columns:
      - id
      - username
      - email
    rule:
      type: ROLE
      expression: "hasRole('USER')"
  
  - entity_type: User
    action: read
    columns:
      - personalId
      - salary
    rule:
      type: ROLE
      expression: "hasRole('ADMIN')"

row_policies:
  - entity_type: User
    action: list
    filter: "tenant_id = '${tenantId}'"
    rule:
      type: ROLE
      expression: "hasRole('MANAGER')"
  
  - entity_type: User
    action: list
    filter: "id = '${userId}'"
    rule:
      type: ROLE
      expression: "hasRole('USER')"
```

### **V budoucnu: Metamodel definition**
```json
{
  "entity": "User",
  "policies": {
    "access": [
      {
        "action": "read",
        "scope": "all",
        "roles": ["ADMIN"]
      },
      {
        "action": "read",
        "scope": "tenant",
        "roles": ["MANAGER"]
      },
      {
        "action": "read",
        "scope": "self",
        "roles": ["USER"]
      }
    ],
    "columns": {
      "public": ["id", "username"],
      "basic": ["email", "phone"],
      "sensitive": ["personalId", "salary"],
      "roles": {
        "USER": ["public", "basic"],
        "MANAGER": ["public", "basic"],
        "ADMIN": ["public", "basic", "sensitive"]
      }
    }
  }
}
```

---

## 🔄 **MIGRATION STEPS**

### **Step 1: Přidat PolicyEngine dependency**
```java
@RequiredArgsConstructor
public class UserManagementController {
  private final KeycloakAdminService keycloakAdminService;
  private final PolicyEngine policyEngine; // ← ADD THIS
}
```

### **Step 2: Nahradit class-level @PreAuthorize**
```java
// REMOVE:
@PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")

// ADD na jednotlivé metody:
@PreAuthorize("@policyMethods.canRead('User', #id)")
```

### **Step 3: Přidat column projection**
```java
@GetMapping("/{id}")
@PreAuthorize("@policyMethods.canRead('User', #id)")
public ResponseEntity<UserDto> getUserById(
    @PathVariable String id,
    Authentication auth) { // ← ADD AUTH
  
  UserDto user = keycloakAdminService.getUserById(id);
  UserDto projected = projectColumns(user, auth, "read", id); // ← ADD PROJECTION
  return ResponseEntity.ok(projected);
}
```

### **Step 4: Přidat row filtering do LIST endpointů**
```java
@GetMapping
@PreAuthorize("@policyMethods.canList('User')")
public ResponseEntity<List<UserDto>> searchUsers(..., Authentication auth) {
  String whereClause = policyEngine.getRowFilter(auth, "User", "list"); // ← ADD FILTER
  List<UserDto> users = keycloakAdminService.searchUsers(...);
  List<UserDto> filtered = applyRowFilter(users, whereClause, auth);
  return ResponseEntity.ok(filtered.stream()
    .map(u -> projectColumns(u, auth, "list"))
    .collect(Collectors.toList()));
}
```

### **Step 5: Testování**
```java
@Test
void testColumnProjection_UserRole() {
  // User vidí jen public fields
  Authentication userAuth = createAuthWithRole("USER");
  UserDto projected = projectColumns(testUser, userAuth, "read", testUser.getId());
  
  assertNotNull(projected.getId());
  assertNotNull(projected.getUsername());
  assertNull(projected.getPersonalId()); // Sensitive field hidden
  assertNull(projected.getSalary());
}

@Test
void testRowFilter_ManagerRole() {
  // Manager vidí jen own tenant
  Authentication managerAuth = createAuthWithRole("MANAGER", "tenant-123");
  String filter = policyEngine.getRowFilter(managerAuth, "User", "list");
  
  assertEquals("tenant_id = 'tenant-123'", filter);
}
```

---

## 📈 **BENEFITS**

### **Security:**
- ✅ Granulární oprávnění na úrovni entity + field + row
- ✅ Self-scope pro vlastní data
- ✅ Tenant isolation automaticky
- ✅ Sensitive fields skryté podle role

### **Flexibility:**
- ✅ Permission changes bez redeploy (pouze metadata)
- ✅ Multiple scopes (all, tenant, self)
- ✅ Custom rules přes SpEL expressions

### **Auditability:**
- ✅ Všechny checks logované přes PolicyEngine
- ✅ Jasné trace kdo vidí co
- ✅ Centrální policy definice

---

**Autor:** GitHub Copilot  
**Datum:** 8. října 2025
