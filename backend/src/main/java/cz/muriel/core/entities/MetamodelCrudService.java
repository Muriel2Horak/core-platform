package cz.muriel.core.entities;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import cz.muriel.core.security.PolicyEngine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic CRUD service for metamodel entities
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetamodelCrudService {
    
    private final MetamodelRegistry registry;
    private final PolicyEngine policyEngine;
    private final EntityManager entityManager;
    
    /**
     * List entities with filtering, sorting and pagination
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(
        String entityType, 
        Map<String, String> filters,
        String sort,
        int page, 
        int size, 
        Authentication auth
    ) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        
        // Check read permission (entity-level, no specific instance)
        if (!policyEngine.check(auth, entityType, "read", null)) {
            throw new AccessDeniedException("No permission to read " + entityType);
        }
        
        // Get allowed columns
        Set<String> allowedColumns = policyEngine.projectColumns(auth, entityType, "read");
        
        // Build query
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<?> root = query.from(getEntityClass(schema.getTable()));
        
        // Apply filters
        List<Predicate> predicates = buildPredicates(cb, root, filters, schema);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }
        
        // Apply sorting
        if (sort != null && !sort.isBlank()) {
            Order order = buildOrder(cb, root, sort);
            if (order != null) {
                query.orderBy(order);
            }
        }
        
        // Select only allowed columns
        List<Selection<?>> selections = buildSelections(root, allowedColumns, schema);
        query.multiselect(selections);
        
        // Execute with pagination
        TypedQuery<Object[]> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        
        List<Object[]> results = typedQuery.getResultList();
        
        // Map to response
        return results.stream()
            .map(row -> mapRowToMap(row, allowedColumns, schema))
            .collect(Collectors.toList());
    }
    
    /**
     * Get entity by ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getById(String entityType, String id, Authentication auth) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        
        // Load entity
        Object entity = findEntityById(schema, id);
        if (entity == null) {
            throw new EntityNotFoundException(entityType, id);
        }
        
        // Check permission with entity context
        if (!policyEngine.check(auth, entityType, "read", entity)) {
            throw new AccessDeniedException("No permission to read this " + entityType);
        }
        
        // Get allowed columns
        Set<String> allowedColumns = policyEngine.projectColumns(auth, entityType, "read");
        
        // Project to map
        return projectEntityToMap(entity, allowedColumns, schema);
    }
    
    /**
     * Create new entity
     */
    @Transactional
    public Map<String, Object> create(String entityType, Map<String, Object> data, Authentication auth) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        
        // Add tenant_id from JWT
        String tenantId = getTenantId(auth);
        if (schema.getTenantField() != null) {
            data.put(schema.getTenantField(), tenantId);
        }
        
        // Set version to 0
        if (schema.getVersionField() != null) {
            data.put(schema.getVersionField(), 0L);
        }
        
        // Check permission (with data as pseudo-entity)
        if (!policyEngine.check(auth, entityType, "create", data)) {
            throw new AccessDeniedException("No permission to create " + entityType);
        }
        
        // Execute native INSERT
        String insertSql = buildInsertSql(schema, data);
        int affected = entityManager.createNativeQuery(insertSql).executeUpdate();
        
        if (affected == 0) {
            throw new RuntimeException("Failed to create entity");
        }
        
        // Return created entity
        Object id = data.get(schema.getIdField());
        return getById(entityType, id.toString(), auth);
    }
    
    /**
     * Update entity with optimistic locking
     */
    @Transactional
    public Map<String, Object> update(
        String entityType, 
        String id, 
        long expectedVersion,
        Map<String, Object> data, 
        Authentication auth
    ) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        
        // Load current entity
        Object entity = findEntityById(schema, id);
        if (entity == null) {
            throw new EntityNotFoundException(entityType, id);
        }
        
        // Check permission
        if (!policyEngine.check(auth, entityType, "update", entity)) {
            throw new AccessDeniedException("No permission to update this " + entityType);
        }
        
        // Check version
        Long currentVersion = extractVersion(entity, schema);
        if (currentVersion == null || currentVersion != expectedVersion) {
            throw new VersionMismatchException(
                "Version mismatch",
                currentVersion != null ? currentVersion : 0L,
                projectEntityToMap(entity, 
                    policyEngine.projectColumns(auth, entityType, "read"), 
                    schema)
            );
        }
        
        // Build UPDATE with version check
        String updateSql = buildUpdateSql(schema, id, data, expectedVersion);
        int affected = entityManager.createNativeQuery(updateSql).executeUpdate();
        
        if (affected == 0) {
            // Version mismatch or entity deleted
            throw new VersionMismatchException(
                "Update failed - version mismatch or entity deleted",
                currentVersion,
                null
            );
        }
        
        // Return updated entity
        return getById(entityType, id, auth);
    }
    
    /**
     * Delete entity
     */
    @Transactional
    public void delete(String entityType, String id, Authentication auth) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        
        // Load entity
        Object entity = findEntityById(schema, id);
        if (entity == null) {
            throw new EntityNotFoundException(entityType, id);
        }
        
        // Check permission
        if (!policyEngine.check(auth, entityType, "delete", entity)) {
            throw new AccessDeniedException("No permission to delete this " + entityType);
        }
        
        // Execute DELETE
        String deleteSql = String.format("DELETE FROM %s WHERE %s = '%s'",
            schema.getTable(), schema.getIdField(), id);
        
        entityManager.createNativeQuery(deleteSql).executeUpdate();
        log.info("Deleted entity: {} id={}", entityType, id);
    }
    
    // Helper methods
    
    private Object findEntityById(EntitySchema schema, String id) {
        String sql = String.format("SELECT * FROM %s WHERE %s = :id",
            schema.getTable(), schema.getIdField());
        
        try {
            return entityManager.createNativeQuery(sql)
                .setParameter("id", UUID.fromString(id))
                .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
    
    private List<Predicate> buildPredicates(
        CriteriaBuilder cb, 
        Root<?> root, 
        Map<String, String> filters,
        EntitySchema schema
    ) {
        List<Predicate> predicates = new ArrayList<>();
        
        for (var entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (key.endsWith("__like")) {
                String field = key.substring(0, key.length() - 6);
                predicates.add(cb.like(root.get(field), value));
            } else if (key.endsWith("__in")) {
                String field = key.substring(0, key.length() - 4);
                String[] values = value.split(",");
                predicates.add(root.get(field).in((Object[]) values));
            } else {
                predicates.add(cb.equal(root.get(key), value));
            }
        }
        
        return predicates;
    }
    
    private Order buildOrder(CriteriaBuilder cb, Root<?> root, String sort) {
        if (sort.startsWith("-")) {
            return cb.desc(root.get(sort.substring(1)));
        } else {
            return cb.asc(root.get(sort));
        }
    }
    
    private List<Selection<?>> buildSelections(
        Root<?> root, 
        Set<String> allowedColumns,
        EntitySchema schema
    ) {
        return allowedColumns.stream()
            .map(col -> root.get(col))
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> mapRowToMap(
        Object[] row, 
        Set<String> allowedColumns,
        EntitySchema schema
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> columns = new ArrayList<>(allowedColumns);
        
        for (int i = 0; i < row.length && i < columns.size(); i++) {
            result.put(columns.get(i), row[i]);
        }
        
        return result;
    }
    
    private Map<String, Object> projectEntityToMap(
        Object entity, 
        Set<String> allowedColumns,
        EntitySchema schema
    ) {
        Map<String, Object> result = new HashMap<>();
        
        for (String col : allowedColumns) {
            try {
                if (entity instanceof Object[] row) {
                    // From native query
                    result.put(col, row[0]); // Simplified
                } else if (entity instanceof Map) {
                    result.put(col, ((Map<?, ?>) entity).get(col));
                }
            } catch (Exception e) {
                log.warn("Failed to extract column {}: {}", col, e.getMessage());
            }
        }
        
        return result;
    }
    
    private String buildInsertSql(EntitySchema schema, Map<String, Object> data) {
        List<String> columns = new ArrayList<>(data.keySet());
        List<String> values = columns.stream()
            .map(col -> formatValue(data.get(col)))
            .collect(Collectors.toList());
        
        return String.format("INSERT INTO %s (%s) VALUES (%s)",
            schema.getTable(),
            String.join(", ", columns),
            String.join(", ", values)
        );
    }
    
    private String buildUpdateSql(
        EntitySchema schema, 
        String id, 
        Map<String, Object> data,
        long expectedVersion
    ) {
        String setClauses = data.entrySet().stream()
            .filter(e -> !e.getKey().equals(schema.getIdField()))
            .filter(e -> !e.getKey().equals(schema.getVersionField()))
            .map(e -> e.getKey() + " = " + formatValue(e.getValue()))
            .collect(Collectors.joining(", "));
        
        return String.format(
            "UPDATE %s SET %s, %s = %s + 1 WHERE %s = '%s' AND %s = %d",
            schema.getTable(),
            setClauses,
            schema.getVersionField(),
            schema.getVersionField(),
            schema.getIdField(),
            id,
            schema.getVersionField(),
            expectedVersion
        );
    }
    
    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String) return "'" + value.toString().replace("'", "''") + "'";
        if (value instanceof Number) return value.toString();
        if (value instanceof UUID) return "'" + value.toString() + "'";
        return "'" + value.toString() + "'";
    }
    
    private Long extractVersion(Object entity, EntitySchema schema) {
        if (schema.getVersionField() == null) return null;
        
        try {
            if (entity instanceof Map) {
                Object version = ((Map<?, ?>) entity).get(schema.getVersionField());
                return version != null ? ((Number) version).longValue() : null;
            }
        } catch (Exception e) {
            log.warn("Failed to extract version: {}", e.getMessage());
        }
        
        return null;
    }
    
    private String getTenantId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("tenant_id");
        }
        return "admin";
    }
    
    private Class<?> getEntityClass(String tableName) {
        // For now, use Object[] for native queries
        return Object.class;
    }
}
