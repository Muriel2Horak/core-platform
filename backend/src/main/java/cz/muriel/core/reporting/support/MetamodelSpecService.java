package cz.muriel.core.reporting.support;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for retrieving entity specifications from metamodel.
 * 
 * Provides allowed fields, aggregations, and restrictions for reporting queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetamodelSpecService {

    private final MetamodelRegistry metamodelRegistry;

    /**
     * Get entity specification for reporting.
     * 
     * @param entityName Entity name
     * @return EntitySpec with allowed fields and operations
     */
    public EntitySpec getEntitySpec(String entityName) {
        EntitySchema entitySchema = metamodelRegistry.getSchemaOrThrow(entityName);
        
        Set<String> allowedDimensions = new HashSet<>();
        Set<String> allowedMeasures = new HashSet<>();
        Set<String> allowedFilters = new HashSet<>();
        List<EntitySpec.FieldSpec> fieldSpecs = new ArrayList<>();

        for (FieldSchema field : entitySchema.getFields()) {
            String fieldName = field.getName();
            
            // Dimensions: categorical fields and IDs
            if (isDimensionField(field)) {
                allowedDimensions.add(fieldName);
            }
            
            // Measures: numeric fields
            if (isMeasureField(field)) {
                allowedMeasures.add(fieldName);
            }
            
            // Filters: most fields except sensitive ones
            if (isFilterableField(field)) {
                allowedFilters.add(fieldName);
            }

            // Build field spec
            EntitySpec.FieldSpec fieldSpec = EntitySpec.FieldSpec.builder()
                .name(fieldName)
                .type(field.getType())
                .editable(isEditableField(field))
                .filterable(isFilterableField(field))
                .sortable(isSortableField(field))
                .allowedOperators(getAllowedOperators(field))
                .build();
            
            fieldSpecs.add(fieldSpec);
        }

        // Determine time dimension
        String timeDimension = findTimeDimension(entitySchema);
        boolean requiresTimeRange = timeDimension != null;

        return EntitySpec.builder()
            .entityName(entityName)
            .specVersion(getMetamodelVersion())
            .allowedDimensions(allowedDimensions)
            .allowedMeasures(allowedMeasures)
            .allowedFilters(allowedFilters)
            .allowedAggregations(getAllowedAggregations())
            .fields(fieldSpecs)
            .requiresTimeRange(requiresTimeRange)
            .defaultTimeDimension(timeDimension)
            .build();
    }

    /**
     * Validate query against entity spec.
     */
    public void validateQuery(String entityName, 
                               List<String> dimensions, 
                               List<String> measures, 
                               List<String> filters) {
        EntitySpec spec = getEntitySpec(entityName);

        // Validate dimensions
        if (dimensions != null) {
            for (String dim : dimensions) {
                if (!spec.getAllowedDimensions().contains(dim)) {
                    throw new IllegalArgumentException(
                        "Invalid dimension '" + dim + "' for entity '" + entityName + "'");
                }
            }
        }

        // Validate measures
        if (measures != null) {
            for (String measure : measures) {
                if (!spec.getAllowedMeasures().contains(measure)) {
                    throw new IllegalArgumentException(
                        "Invalid measure '" + measure + "' for entity '" + entityName + "'");
                }
            }
        }

        // Validate filters
        if (filters != null) {
            for (String filter : filters) {
                if (!spec.getAllowedFilters().contains(filter)) {
                    throw new IllegalArgumentException(
                        "Invalid filter field '" + filter + "' for entity '" + entityName + "'");
                }
            }
        }
    }

    private boolean isEditableField(FieldSchema field) {
        // Primary keys and generated fields are not editable
        return !Boolean.TRUE.equals(field.getPk()) && 
               !Boolean.TRUE.equals(field.getGenerated());
    }

    private boolean isDimensionField(FieldSchema field) {
        String typeName = field.getType();
        return typeName.equals("string") ||
               typeName.equals("uuid") ||
               typeName.equals("enum") ||
               typeName.equals("boolean") ||
               typeName.equals("email") ||
               field.getName().endsWith("_id") ||
               field.getName().equals("id");
    }

    private boolean isMeasureField(FieldSchema field) {
        String typeName = field.getType();
        return typeName.equals("integer") ||
               typeName.equals("long") ||
               typeName.equals("double") ||
               typeName.equals("decimal");
    }

    private boolean isFilterableField(FieldSchema field) {
        // Exclude sensitive fields
        String name = field.getName().toLowerCase();
        if (name.contains("password") || name.contains("secret") || name.contains("token")) {
            return false;
        }
        return true;
    }

    private boolean isSortableField(FieldSchema field) {
        // Most fields are sortable except TEXT/JSON/BLOB
        String typeName = field.getType();
        return !typeName.equals("text") &&
               !typeName.equals("json") &&
               !typeName.equals("jsonb") &&
               !typeName.equals("blob");
    }

    private List<String> getAllowedOperators(FieldSchema field) {
        String type = field.getType();
        switch (type) {
            case "string":
            case "text":
            case "email":
                return Arrays.asList("eq", "ne", "contains", "startsWith", "endsWith", "in", "notIn");
            case "integer":
            case "long":
            case "double":
            case "decimal":
            case "date":
            case "timestamp":
                return Arrays.asList("eq", "ne", "gt", "gte", "lt", "lte", "in", "notIn", "between");
            case "boolean":
                return Arrays.asList("eq", "ne");
            case "uuid":
                return Arrays.asList("eq", "ne", "in", "notIn");
            case "enum":
                return Arrays.asList("eq", "ne", "in", "notIn");
            default:
                return Arrays.asList("eq", "ne");
        }
    }

    private String findTimeDimension(EntitySchema entitySchema) {
        // Look for created_at, updated_at, or timestamp fields
        for (FieldSchema field : entitySchema.getFields()) {
            String name = field.getName().toLowerCase();
            if (name.equals("created_at") || name.equals("createdat")) {
                return field.getName();
            }
        }
        for (FieldSchema field : entitySchema.getFields()) {
            String name = field.getName().toLowerCase();
            if (name.equals("updated_at") || name.equals("updatedat")) {
                return field.getName();
            }
        }
        for (FieldSchema field : entitySchema.getFields()) {
            if (field.getType().equals("timestamp")) {
                return field.getName();
            }
        }
        return null;
    }

    private Set<String> getAllowedAggregations() {
        return Set.of("count", "sum", "avg", "min", "max", "countDistinct");
    }

    private String getMetamodelVersion() {
        // Return a version identifier - could be based on schema hash or timestamp
        return "v1.0";
    }
}
