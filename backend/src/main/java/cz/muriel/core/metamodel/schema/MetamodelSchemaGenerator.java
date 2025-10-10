package cz.muriel.core.metamodel.schema;

import cz.muriel.core.metamodel.MetamodelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Generates and applies database schema from Metamodel YAML definitions
 * 
 * ⚠️ EXPERIMENTAL: Pure Metamodel-driven schema management
 * 
 * Features: - CREATE TABLE from YAML - ALTER TABLE for missing columns - CREATE
 * INDEX - CREATE TRIGGER for version auto-increment - CREATE FOREIGN KEY
 * constraints
 */
@Slf4j @Component @RequiredArgsConstructor @ConditionalOnProperty(name = "metamodel.schema.auto-generate", havingValue = "true", matchIfMissing = false)
public class MetamodelSchemaGenerator {

  private final MetamodelRegistry registry;
  private final JdbcTemplate jdbcTemplate;
  private final TypeConversionRegistry conversionRegistry;

  @PostConstruct
  public void generateSchema() {
    log.info("🔨 Starting Metamodel schema generation and validation...");

    try {
      for (EntitySchema schema : registry.getAllSchemas().values()) {
        processEntitySchema(schema);
      }

      log.info("✅ Metamodel schema generation completed successfully");
    } catch (Exception e) {
      log.error("❌ Metamodel schema generation failed: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to generate schema from Metamodel", e);
    }
  }

  /**
   * Detect differences between YAML schema and current DB schema
   */
  public SchemaDiff detectChanges(EntitySchema schema) {
    SchemaDiff diff = new SchemaDiff();
    diff.setEntityType(schema.getEntity());
    diff.setTableName(schema.getTable());

    if (!tableExists(schema.getTable())) {
      log.info("📋 Table {} does not exist, will be created", schema.getTable());
      return diff; // Empty diff, table will be created
    }

    // Get current DB columns
    Map<String, ColumnInfo> dbColumns = getCurrentColumns(schema.getTable());

    // Check each field in YAML
    for (FieldSchema field : schema.getFields()) {
      // Skip relationship fields - they don't map to DB columns
      if (field.getType().equals("manyToMany") || field.getType().equals("manyToOne")
          || field.getType().equals("oneToMany")) {
        continue;
      }

      String columnName = field.getName();
      ColumnInfo dbColumn = dbColumns.get(columnName.toLowerCase()); // Case-insensitive lookup

      if (dbColumn == null) {
        // Column missing in DB
        diff.getColumnChanges().add(createAddColumnChange(field, schema));
      } else {
        // Column exists, check for changes
        detectColumnChanges(diff, field, dbColumn, schema);
      }
    }

    // Check for orphaned columns (in DB but not in YAML)
    for (String dbColumnName : dbColumns.keySet()) {
      boolean existsInYaml = schema.getFields().stream()
          .filter(f -> !f.getType().equals("manyToMany") && !f.getType().equals("manyToOne")
              && !f.getType().equals("oneToMany")) // Exclude relationships
          .anyMatch(f -> f.getName().equalsIgnoreCase(dbColumnName)); // Case-insensitive

      if (!existsInYaml) {
        log.warn("⚠️ Orphaned column detected: {}.{} (exists in DB but not in YAML)",
            schema.getTable(), dbColumnName);
        // Don't auto-drop, too dangerous!
      }
    }

    return diff;
  }

  private Map<String, ColumnInfo> getCurrentColumns(String tableName) {
    String sql = """
        SELECT
          c.column_name,
          c.data_type,
          c.character_maximum_length,
          c.numeric_precision,
          c.numeric_scale,
          c.is_nullable,
          c.column_default,
          CASE WHEN pk.constraint_type = 'PRIMARY KEY' THEN true ELSE false END as is_primary_key,
          fk.foreign_table_name,
          fk.foreign_column_name
        FROM information_schema.columns c
        LEFT JOIN (
          SELECT kcu.column_name, tc.constraint_type
          FROM information_schema.table_constraints tc
          JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
          WHERE tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY'
        ) pk ON c.column_name = pk.column_name
        LEFT JOIN (
          SELECT
            kcu.column_name,
            ccu.table_name AS foreign_table_name,
            ccu.column_name AS foreign_column_name
          FROM information_schema.table_constraints tc
          JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
          JOIN information_schema.constraint_column_usage ccu
            ON tc.constraint_name = ccu.constraint_name
          WHERE tc.table_name = ? AND tc.constraint_type = 'FOREIGN KEY'
        ) fk ON c.column_name = fk.column_name
        WHERE c.table_schema = 'public' AND c.table_name = ?
        ORDER BY c.ordinal_position
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
      ColumnInfo info = new ColumnInfo();
      info.setColumnName(rs.getString("column_name"));
      info.setDataType(rs.getString("data_type"));
      info.setCharacterMaximumLength((Integer) rs.getObject("character_maximum_length"));
      info.setNumericPrecision((Integer) rs.getObject("numeric_precision"));
      info.setNumericScale((Integer) rs.getObject("numeric_scale"));
      info.setNullable("YES".equals(rs.getString("is_nullable")));
      info.setColumnDefault(rs.getString("column_default"));
      info.setPrimaryKey(rs.getBoolean("is_primary_key"));
      info.setForeignKeyTable(rs.getString("foreign_table_name"));
      info.setForeignKeyColumn(rs.getString("foreign_column_name"));
      return info;
    }, tableName, tableName, tableName).stream()
        .collect(java.util.stream.Collectors.toMap(col -> col.getColumnName().toLowerCase(), // Case-insensitive
                                                                                             // lookup
            info -> info));
  }

  private SchemaDiff.ColumnChange createAddColumnChange(FieldSchema field, EntitySchema schema) {
    SchemaDiff.ColumnChange change = new SchemaDiff.ColumnChange();
    change.setType(SchemaDiff.ColumnChange.ChangeType.ADD);
    change.setColumnName(field.getName());
    change.setNewType(mapTypeToPostgres(field));
    change.setRisky(false);
    change.setSql(String.format("ALTER TABLE %s ADD COLUMN %s", schema.getTable(),
        generateColumnDefinition(field, schema).trim()));
    return change;
  }

  private void detectColumnChanges(SchemaDiff diff, FieldSchema field, ColumnInfo dbColumn,
      EntitySchema schema) {

    String expectedType = mapTypeToPostgres(field);
    String actualType = dbColumn.getNormalizedType();

    // Type mismatch?
    if (!typesMatch(expectedType, actualType, field, dbColumn)) {
      SchemaDiff.ColumnChange change = new SchemaDiff.ColumnChange();
      change.setType(SchemaDiff.ColumnChange.ChangeType.ALTER_TYPE);
      change.setColumnName(field.getName());
      change.setOldType(dbColumn.getFullType());
      change.setNewType(expectedType);

      // Check if conversion is supported
      var conversion = conversionRegistry.find(actualType, expectedType);
      if (conversion.isPresent()) {
        change.setRisky(conversion.get().isRisky());
        change.setRiskDescription(conversion.get().getWarning());
        change.setSql(
            conversion.get().generateSql(schema.getTable(), field.getName(), field.getMaxLength()));
      } else {
        change.setRisky(true);
        change.setRiskDescription("No automatic conversion available - manual migration required");
        change.setSql(
            "-- MANUAL MIGRATION REQUIRED: " + dbColumn.getFullType() + " → " + expectedType);
      }

      diff.getColumnChanges().add(change);
    }

    // Nullable mismatch?
    boolean expectedNullable = !Boolean.TRUE.equals(field.getRequired())
        || field.getDefaultValue() != null;
    if (expectedNullable != dbColumn.isNullable()) {
      // SKIP: Cannot change nullable on primary key columns
      if (dbColumn.isPrimaryKey()) {
        log.debug("⏭️ Skipping nullable change on PRIMARY KEY column: {}.{}", schema.getTable(),
            field.getName());
        return;
      }

      SchemaDiff.ColumnChange change = new SchemaDiff.ColumnChange();
      change.setType(SchemaDiff.ColumnChange.ChangeType.ALTER_NULLABLE);
      change.setColumnName(field.getName());
      change.setOldNullable(dbColumn.isNullable());
      change.setNewNullable(expectedNullable);
      change.setRisky(!expectedNullable); // Making NOT NULL is risky if data contains NULLs
      change.setSql(String.format("ALTER TABLE %s ALTER COLUMN %s %s NOT NULL", schema.getTable(),
          field.getName(), expectedNullable ? "DROP" : "SET"));
      diff.getColumnChanges().add(change);
    }
  }

  private boolean typesMatch(String expectedType, String actualType, FieldSchema field,
      ColumnInfo dbColumn) {

    // Exact match
    if (expectedType.equalsIgnoreCase(actualType)) {
      return true;
    }

    // VARCHAR length expansion is OK
    if (expectedType.startsWith("VARCHAR") && actualType.equals("VARCHAR")) {
      Integer expectedLen = field.getMaxLength();
      Integer actualLen = dbColumn.getCharacterMaximumLength();
      if (expectedLen != null && actualLen != null) {
        return expectedLen <= actualLen; // Expanding is OK
      }
    }

    // TEXT encompasses VARCHAR
    if (expectedType.equals("TEXT") && actualType.equals("VARCHAR")) {
      return true;
    }

    // UUID can be stored as TEXT/VARCHAR in old schemas
    if (expectedType.equals("UUID")
        && (actualType.equals("TEXT") || actualType.equals("VARCHAR"))) {
      return true; // Accept but will be flagged for conversion
    }

    return false;
  }

  private void processEntitySchema(EntitySchema schema) {
    log.info("📋 Processing entity: {}", schema.getEntity());

    // 1. Detect changes between YAML and DB
    SchemaDiff diff = detectChanges(schema);

    // 2. Create table if not exists
    if (!tableExists(schema.getTable())) {
      createTable(schema);
    } else {
      // 3. Apply detected changes
      applyChanges(diff, schema);
    }

    // 4. Create indexes
    createIndexes(schema);

    // 5. Create UNIQUE constraints
    createUniqueConstraints(schema);

    // 6. Create version trigger if needed
    if (schema.getVersionField() != null) {
      createVersionTrigger(schema);
    }

    // 7. Create M:N junction tables for manyToMany relationships
    createManyToManyJunctionTables(schema);
  }

  /**
   * Create junction tables for M:N relationships
   */
  private void createManyToManyJunctionTables(EntitySchema schema) {
    for (FieldSchema field : schema.getFields()) {
      if ("manyToMany".equals(field.getType())) {
        String junctionTable = field.getJoinTable();

        if (junctionTable == null || junctionTable.isBlank()) {
          log.warn("⚠️ M:N field '{}' missing joinTable, skipping", field.getName());
          continue;
        }

        if (tableExists(junctionTable)) {
          log.debug("✅ Junction table {} already exists", junctionTable);
          continue;
        }

        String sourceColumn = field.getJoinColumn() != null ? field.getJoinColumn()
            : schema.getIdField();
        String targetColumn = field.getInverseJoinColumn() != null ? field.getInverseJoinColumn()
            : "target_id";

        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
              %s UUID NOT NULL,
              %s UUID NOT NULL,
              created_at TIMESTAMPTZ DEFAULT NOW(),
              PRIMARY KEY (%s, %s)
            )
            """, junctionTable, sourceColumn, targetColumn, sourceColumn, targetColumn);

        try {
          jdbcTemplate.execute(sql);
          log.info("✅ Created M:N junction table: {}", junctionTable);
        } catch (Exception e) {
          log.error("❌ Failed to create junction table {}: {}", junctionTable, e.getMessage());
        }
      }
    }
  }

  /**
   * Apply schema changes detected by detectChanges()
   */
  private void applyChanges(SchemaDiff diff, EntitySchema schema) {
    if (diff.getColumnChanges().isEmpty()) {
      log.debug("✅ No column changes for {}", schema.getTable());
      return;
    }

    log.info("🔄 Applying {} column changes to {}", diff.getColumnChanges().size(),
        schema.getTable());

    for (SchemaDiff.ColumnChange change : diff.getColumnChanges()) {
      if (change.isRisky()) {
        log.warn("⚠️ SKIPPING risky change: {} - {}", change.getSql(), change.getRiskDescription());
        log.warn("   Please apply manually or review carefully");
        continue;
      }

      try {
        log.info("  ↳ {}: {}", change.getType(), change.getColumnName());
        jdbcTemplate.execute(change.getSql());
      } catch (Exception e) {
        log.error("❌ Failed to apply change: {}", change.getSql(), e);
        throw new RuntimeException("Schema change failed", e);
      }
    }
  }

  private boolean tableExists(String tableName) {
    String sql = """
        SELECT EXISTS (
          SELECT FROM information_schema.tables
          WHERE table_schema = 'public'
          AND table_name = ?
        )
        """;
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, tableName));
  }

  private void createTable(EntitySchema schema) {
    log.info("🔨 Creating table: {}", schema.getTable());

    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE IF NOT EXISTS ").append(schema.getTable()).append(" (\n");

    List<String> columnDefs = new ArrayList<>();

    for (FieldSchema field : schema.getFields()) {
      // Skip many-to-many relationships (they need junction tables)
      if (field.getType().equals("manyToMany")) {
        continue;
      }

      columnDefs.add(generateColumnDefinition(field, schema));
    }

    sql.append(String.join(",\n  ", columnDefs));
    sql.append("\n)");

    jdbcTemplate.execute(sql.toString());
    log.info("✅ Table created: {}", schema.getTable());
  }

  private String generateColumnDefinition(FieldSchema field, EntitySchema schema) {
    StringBuilder def = new StringBuilder("  ");
    def.append(field.getName()).append(" ");

    // Map YAML type to PostgreSQL type
    def.append(mapTypeToPostgres(field));

    // Primary key
    if (field.getName().equals(schema.getIdField())) {
      def.append(" PRIMARY KEY");
    }

    // NOT NULL
    if (Boolean.TRUE.equals(field.getRequired()) && field.getDefaultValue() == null) {
      def.append(" NOT NULL");
    }

    // DEFAULT
    if (field.getDefaultValue() != null) {
      def.append(" DEFAULT ").append(formatDefaultValue(field));
    }

    // Foreign key (for manyToOne)
    if (field.getType().equals("manyToOne") && field.getRefField() != null) {
      String targetTable = registry.getSchemaOrThrow(field.getTargetEntity()).getTable();
      def.append(" REFERENCES ").append(targetTable).append("(id)");
    }

    return def.toString();
  }

  private String mapTypeToPostgres(FieldSchema field) {
    return switch (field.getType()) {
    case "uuid" -> "UUID";
    case "string", "email" -> {
      int maxLen = field.getMaxLength() != null ? field.getMaxLength() : 255;
      yield "VARCHAR(" + maxLen + ")";
    }
    case "text" -> "TEXT";
    case "boolean" -> "BOOLEAN";
    case "integer" -> "INTEGER";
    case "long" -> "BIGINT";
    case "timestamp" -> "TIMESTAMPTZ";
    case "date" -> "DATE";
    case "manyToOne" -> "UUID"; // Foreign key
    default -> {
      log.warn("⚠️ Unknown type {}, using TEXT", field.getType());
      yield "TEXT";
    }
    };
  }

  private String formatDefaultValue(FieldSchema field) {
    Object value = field.getDefaultValue();

    if (value == null) {
      return "NULL";
    }

    return switch (field.getType()) {
    case "string", "email", "text" -> "'" + value.toString() + "'";
    case "boolean" -> value.toString().toUpperCase();
    case "integer", "long" -> value.toString();
    case "timestamp" -> "NOW()";
    case "uuid" -> value.equals("gen_random_uuid()") ? "gen_random_uuid()" : "'" + value + "'";
    default -> "'" + value + "'";
    };
  }

  private void createIndexes(EntitySchema schema) {
    log.debug("📑 Creating indexes for: {}", schema.getTable());

    // Index on tenant field
    if (schema.getTenantField() != null) {
      createIndex(schema.getTable(), schema.getTenantField());
    }

    // Index on version field
    if (schema.getVersionField() != null) {
      createIndex(schema.getTable(), schema.getVersionField());
    }

    // Index on foreign keys
    for (FieldSchema field : schema.getFields()) {
      if (field.getType().equals("manyToOne") || field.getName().endsWith("_id")) {
        createIndex(schema.getTable(), field.getName());
      }
    }
  }

  private void createIndex(String tableName, String columnName) {
    String indexName = "idx_" + tableName + "_" + columnName;
    String sql = String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)", indexName, tableName,
        columnName);

    try {
      jdbcTemplate.execute(sql);
      log.debug("✅ Index created: {}", indexName);
    } catch (Exception e) {
      log.warn("⚠️ Failed to create index {}: {}", indexName, e.getMessage());
    }
  }

  /**
   * Create UNIQUE constraints from YAML field definitions
   */
  private void createUniqueConstraints(EntitySchema schema) {
    log.debug("🔒 Creating UNIQUE constraints for: {}", schema.getTable());

    for (FieldSchema field : schema.getFields()) {
      if (Boolean.TRUE.equals(field.getUnique())) {
        createUniqueConstraint(schema.getTable(), field.getName());
      }
    }
  }

  private void createUniqueConstraint(String tableName, String columnName) {
    String constraintName = "uk_" + tableName + "_" + columnName;

    // Check if constraint already exists
    String checkSql = """
        SELECT EXISTS (
          SELECT 1 FROM information_schema.table_constraints
          WHERE table_schema = 'public'
          AND table_name = ?
          AND constraint_name = ?
          AND constraint_type = 'UNIQUE'
        )
        """;

    Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, tableName,
        constraintName);

    if (Boolean.TRUE.equals(exists)) {
      log.debug("✅ UNIQUE constraint already exists: {}", constraintName);
      return;
    }

    String sql = String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)", tableName,
        constraintName, columnName);

    try {
      jdbcTemplate.execute(sql);
      log.debug("✅ UNIQUE constraint created: {}", constraintName);
    } catch (Exception e) {
      log.warn("⚠️ Failed to create UNIQUE constraint {}: {}", constraintName, e.getMessage());
    }
  }

  private void createVersionTrigger(EntitySchema schema) {
    String tableName = schema.getTable();
    String versionField = schema.getVersionField();
    String functionName = "increment_" + tableName.replace(".", "_") + "_version";
    String triggerName = "trigger_" + functionName;

    log.info("⚡ Creating version trigger for: {}", tableName);

    // Create function
    String functionSql = String.format("""
        CREATE OR REPLACE FUNCTION %s()
        RETURNS TRIGGER AS $$
        BEGIN
            NEW.%s = COALESCE(OLD.%s, 0) + 1;
            IF NEW.updated_at IS NOT NULL THEN
                NEW.updated_at = NOW();
            END IF;
            RETURN NEW;
        END;
        $$ LANGUAGE plpgsql;
        """, functionName, versionField, versionField);

    jdbcTemplate.execute(functionSql);

    // Drop old trigger
    String dropTriggerSql = String.format("DROP TRIGGER IF EXISTS %s ON %s", triggerName,
        tableName);
    jdbcTemplate.execute(dropTriggerSql);

    // Create trigger
    String triggerSql = String.format("""
        CREATE TRIGGER %s
            BEFORE UPDATE ON %s
            FOR EACH ROW
            EXECUTE FUNCTION %s();
        """, triggerName, tableName, functionName);

    jdbcTemplate.execute(triggerSql);
    log.info("✅ Version trigger created: {}", triggerName);
  }

  /**
   * Utility method to drop all metamodel-managed tables ⚠️ DANGEROUS - only for
   * development!
   */
  public void dropAllTables() {
    if (!isDevelopmentMode()) {
      throw new IllegalStateException("Cannot drop tables in production!");
    }

    log.warn("🔥 DROPPING ALL METAMODEL TABLES!");

    for (EntitySchema schema : registry.getAllSchemas().values()) {
      String sql = "DROP TABLE IF EXISTS " + schema.getTable() + " CASCADE";
      jdbcTemplate.execute(sql);
      log.warn("🗑️ Dropped table: {}", schema.getTable());
    }
  }

  private boolean isDevelopmentMode() {
    // Check if running in dev environment via Spring profiles
    String activeProfiles = System.getProperty("spring.profiles.active", "");
    return activeProfiles.contains("dev") || activeProfiles.contains("local");
  }
}
