# META-006: Code Generation (JPA Entities, Repositories, Controllers)

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED** (Design phase)  
**Priorita:** P2 (Nice-to-have automation)  
**Estimated LOC:** ~600 řádků  

---

## 📋 Story Description

Jako **platform developer**, chci **auto-generovat Java kód z YAML metamodelu**, abych **eliminoval boilerplate code a zajistil konzistenci mezi schema a JPA entitami**.

---

## 🎯 Acceptance Criteria

### AC1: JPA Entity Generation
- **GIVEN** YAML entity definition
- **WHEN** spustím generator
- **THEN** vygeneruje JPA `@Entity` class:
  - `@Table(name = "...")` z YAML `table`
  - `@Column` annotations pro každý field
  - `@ManyToOne`, `@OneToMany` relationships
  - Lombok `@Data`, `@Builder`
  - Tenant isolation (`@Where` clause)

### AC2: Spring Data Repository Generation
- **GIVEN** vygenerovaná entity
- **WHEN** generuji repository
- **THEN** vytvoří:
  ```java
  @Repository
  public interface UserRepository extends JpaRepository<User, Long>, TenantAwareRepository<User> {
      Optional<User> findByEmail(String email);
      List<User> findByRole(String role);
  }
  ```

### AC3: REST Controller Generation
- **GIVEN** entity + repository
- **WHEN** generuji controller
- **THEN** vytvoří CRUD endpoints:
  - `GET /api/users` (list)
  - `GET /api/users/{id}` (detail)
  - `POST /api/users` (create)
  - `PUT /api/users/{id}` (update)
  - `DELETE /api/users/{id}` (delete)

### AC4: Incremental Generation
- **GIVEN** existující ručně upravená entity
- **WHEN** re-generuji
- **THEN** zachová custom metody (via `// CUSTOM CODE START/END` blocks)

---

## 🏗️ Planned Implementation

### Template Engine (Apache Velocity)

```java
@Service
public class CodeGenerator {
    
    private final VelocityEngine velocityEngine;
    
    public String generateEntity(EntitySchema schema) {
        VelocityContext context = new VelocityContext();
        context.put("entity", schema);
        context.put("table", schema.getTable());
        context.put("fields", schema.getFields());
        context.put("packageName", "cz.muriel.core.generated");
        
        Template template = velocityEngine.getTemplate("templates/entity.vm");
        
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        
        return writer.toString();
    }
    
    public String generateRepository(EntitySchema schema) {
        VelocityContext context = new VelocityContext();
        context.put("entity", schema);
        context.put("repositoryName", schema.getName() + "Repository");
        context.put("packageName", "cz.muriel.core.generated.repository");
        
        Template template = velocityEngine.getTemplate("templates/repository.vm");
        
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        
        return writer.toString();
    }
    
    public String generateController(EntitySchema schema) {
        VelocityContext context = new VelocityContext();
        context.put("entity", schema);
        context.put("controllerName", schema.getName() + "Controller");
        context.put("packageName", "cz.muriel.core.generated.controller");
        context.put("basePath", "/api/" + schema.getTable());
        
        Template template = velocityEngine.getTemplate("templates/controller.vm");
        
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        
        return writer.toString();
    }
}
```

### Entity Template (`templates/entity.vm`)

```java
package ${packageName}.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "${table}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "tenant_id = :tenantId")
public class ${entity.name} {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
#foreach($field in $fields)
#if($field.type == "string")
    @Column(name = "${field.name}"#if($field.unique), unique = true#end#if(!$field.nullable), nullable = false#end)
    private String ${field.name};
#elseif($field.type == "integer")
    @Column(name = "${field.name}")
    private Integer ${field.name};
#elseif($field.type == "decimal")
    @Column(name = "${field.name}", precision = 19, scale = 2)
    private BigDecimal ${field.name};
#elseif($field.type == "boolean")
    @Column(name = "${field.name}")
    private Boolean ${field.name};
#elseif($field.type == "date")
    @Column(name = "${field.name}")
    private LocalDateTime ${field.name};
#end
#end

    // CUSTOM CODE START
    // Add your custom methods here - this block will be preserved during re-generation
    // CUSTOM CODE END
}
```

### Repository Template (`templates/repository.vm`)

```java
package ${packageName}.repository;

import ${packageName}.entity.${entity.name};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import cz.muriel.core.repository.TenantAwareRepository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ${repositoryName} extends JpaRepository<${entity.name}, Long>, TenantAwareRepository<${entity.name}> {
    
#foreach($field in $fields)
#if($field.unique)
    Optional<${entity.name}> findBy${field.name.substring(0,1).toUpperCase()}${field.name.substring(1)}(String ${field.name});
#end
#end

    // CUSTOM QUERIES START
    // Add your custom query methods here
    // CUSTOM QUERIES END
}
```

### Controller Template (`templates/controller.vm`)

```java
package ${packageName}.controller;

import ${packageName}.entity.${entity.name};
import ${packageName}.repository.${repositoryName};
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${basePath}")
@RequiredArgsConstructor
public class ${controllerName} {
    
    private final ${repositoryName} repository;
    
    @GetMapping
    public List<${entity.name}> findAll() {
        return repository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<${entity.name}> findById(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ${entity.name} create(@RequestBody ${entity.name} entity) {
        return repository.save(entity);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<${entity.name}> update(@PathVariable Long id, @RequestBody ${entity.name} entity) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entity.setId(id);
        return ResponseEntity.ok(repository.save(entity));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // CUSTOM ENDPOINTS START
    // Add your custom endpoints here
    // CUSTOM ENDPOINTS END
}
```

---

## 🧪 Planned Tests

```java
@Test
void shouldGenerateEntityClass() {
    // Given: User entity YAML
    EntitySchema schema = EntitySchema.builder()
        .name("User")
        .table("users")
        .fields(List.of(
            FieldDefinition.builder().name("email").type("string").unique(true).build(),
            FieldDefinition.builder().name("name").type("string").build()
        ))
        .build();
    
    // When: Generate code
    String javaCode = codeGenerator.generateEntity(schema);
    
    // Then: Valid Java class
    assertThat(javaCode).contains("@Entity");
    assertThat(javaCode).contains("@Table(name = \"users\")");
    assertThat(javaCode).contains("public class User");
    assertThat(javaCode).contains("private String email");
    assertThat(javaCode).contains("@Column(name = \"email\", unique = true)");
}

@Test
void shouldPreserveCustomCode() {
    // Given: Existing entity with custom method
    String existingCode = """
        @Entity
        public class User {
            private String email;
            
            // CUSTOM CODE START
            public boolean isAdmin() { return email.endsWith("@admin.com"); }
            // CUSTOM CODE END
        }
        """;
    
    // When: Re-generate
    String newCode = codeGenerator.regenerateEntity(existingCode, schema);
    
    // Then: Custom method preserved
    assertThat(newCode).contains("public boolean isAdmin()");
}
```

---

## 💡 Expected Value

### Benefits
- **Zero Boilerplate**: No manual JPA annotations
- **Consistency**: YAML → DB → JPA always in sync
- **Productivity**: 5 min to generate full CRUD module (vs 1 hour manual)
- **Maintainability**: Re-generate after YAML change

### Time Savings
- **Manual Entity**: ~15 min (class + annotations + relationships)
- **Generated Entity**: ~10 seconds
- **Manual CRUD Controller**: ~20 min
- **Generated Controller**: ~10 seconds

---

## 🔗 Related

- **Depends On:** [META-001 (Schema Diff)](META-001.md)
- **Inspired By:** 
  - JHipster (entity generation)
  - Prisma (schema → code sync)
  - GraphQL Code Generator

---

## 📚 References

- **Apache Velocity:** https://velocity.apache.org/
- **Freemarker Alternative:** https://freemarker.apache.org/
- **JHipster Entity Generation:** https://www.jhipster.tech/creating-an-entity/
