package cz.muriel.core.metamodel.schema;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of supported PostgreSQL type conversions
 * 
 * Defines which type changes are: - SAFE: No data loss, automatic - RISKY:
 * Possible data loss, requires confirmation - FORBIDDEN: Not supported, manual
 * migration required
 */
@Slf4j @Component
public class TypeConversionRegistry {

  private final Map<TypePair, TypeConversion> conversions = new HashMap<>();

  @PostConstruct
  public void init() {
    registerSafeConversions();
    registerRiskyConversions();
  }

  private void registerSafeConversions() {
    // String expansions
    register("VARCHAR", "TEXT",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE TEXT"));

    register("CHAR", "VARCHAR",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE VARCHAR({maxLength})"));

    // Number expansions
    register("SMALLINT", "INTEGER",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE INTEGER"));

    register("INTEGER", "BIGINT",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE BIGINT"));

    register("REAL", "DOUBLE PRECISION",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE DOUBLE PRECISION"));

    // Time expansions
    register("DATE", "TIMESTAMP",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE TIMESTAMPTZ"));

    register("TIMESTAMP", "TIMESTAMPTZ",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE TIMESTAMPTZ"));

    // JSON
    register("JSON", "JSONB", TypeConversion
        .safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE JSONB USING {column}::JSONB"));

    // VARCHAR expansion (same type, bigger length)
    register("VARCHAR(small)", "VARCHAR(large)",
        TypeConversion.safe("ALTER TABLE {table} ALTER COLUMN {column} TYPE VARCHAR({maxLength})"));
  }

  private void registerRiskyConversions() {
    // String truncation
    register("TEXT", "VARCHAR", TypeConversion.risky(
        "ALTER TABLE {table} ALTER COLUMN {column} TYPE VARCHAR({maxLength}) USING LEFT({column}, {maxLength})",
        "Data will be truncated to {maxLength} characters"));

    register("VARCHAR(large)", "VARCHAR(small)", TypeConversion.risky(
        "ALTER TABLE {table} ALTER COLUMN {column} TYPE VARCHAR({maxLength}) USING LEFT({column}, {maxLength})",
        "Data will be truncated to {maxLength} characters"));

    // Number narrowing
    register("BIGINT", "INTEGER", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE INTEGER
        USING CASE
          WHEN {column} BETWEEN -2147483648 AND 2147483647 THEN {column}::INTEGER
          ELSE NULL
        END
        """, "Values out of INTEGER range (-2147483648 to 2147483647) will become NULL"));

    register("INTEGER", "SMALLINT", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE SMALLINT
        USING CASE
          WHEN {column} BETWEEN -32768 AND 32767 THEN {column}::SMALLINT
          ELSE NULL
        END
        """, "Values out of SMALLINT range (-32768 to 32767) will become NULL"));

    // Time narrowing
    register("TIMESTAMPTZ", "TIMESTAMP",
        TypeConversion.risky(
            "ALTER TABLE {table} ALTER COLUMN {column} TYPE TIMESTAMP USING {column}::TIMESTAMP",
            "Timezone information will be lost"));

    register("TIMESTAMP", "DATE",
        TypeConversion.risky(
            "ALTER TABLE {table} ALTER COLUMN {column} TYPE DATE USING {column}::DATE",
            "Time information will be lost"));

    // Text to structured
    register("TEXT", "JSON", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE JSON
        USING CASE
          WHEN {column} ~ '^[\\[\\{]' THEN {column}::JSON
          ELSE NULL
        END
        """, "Invalid JSON values will become NULL"));

    register("VARCHAR", "JSON", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE JSON
        USING CASE
          WHEN {column} ~ '^[\\[\\{]' THEN {column}::JSON
          ELSE NULL
        END
        """, "Invalid JSON values will become NULL"));

    register("TEXT", "UUID", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE UUID
        USING CASE
          WHEN {column} ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
          THEN {column}::UUID
          ELSE NULL
        END
        """, "Invalid UUID values will become NULL"));

    register("VARCHAR", "UUID", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE UUID
        USING CASE
          WHEN {column} ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
          THEN {column}::UUID
          ELSE NULL
        END
        """, "Invalid UUID values will become NULL"));

    // Text to number
    register("TEXT", "INTEGER", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE INTEGER
        USING CASE
          WHEN {column} ~ '^-?[0-9]+$' THEN {column}::INTEGER
          ELSE NULL
        END
        """, "Non-numeric values will become NULL"));

    register("VARCHAR", "INTEGER", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE INTEGER
        USING CASE
          WHEN {column} ~ '^-?[0-9]+$' THEN {column}::INTEGER
          ELSE NULL
        END
        """, "Non-numeric values will become NULL"));

    // Text to boolean
    register("TEXT", "BOOLEAN", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE BOOLEAN
        USING CASE
          WHEN LOWER({column}) IN ('true', 't', 'yes', 'y', '1') THEN TRUE
          WHEN LOWER({column}) IN ('false', 'f', 'no', 'n', '0') THEN FALSE
          ELSE NULL
        END
        """, "Unrecognized values will become NULL"));

    register("VARCHAR", "BOOLEAN", TypeConversion.risky("""
        ALTER TABLE {table} ALTER COLUMN {column} TYPE BOOLEAN
        USING CASE
          WHEN LOWER({column}) IN ('true', 't', 'yes', 'y', '1') THEN TRUE
          WHEN LOWER({column}) IN ('false', 'f', 'no', 'n', '0') THEN FALSE
          ELSE NULL
        END
        """, "Unrecognized values will become NULL"));
  }

  public void register(String fromType, String toType, TypeConversion conversion) {
    conversions.put(new TypePair(fromType, toType), conversion);
    log.debug("Registered conversion: {} → {} ({})", fromType, toType,
        conversion.isRisky() ? "RISKY" : "SAFE");
  }

  public Optional<TypeConversion> find(String fromType, String toType) {
    // Exact match
    TypeConversion conversion = conversions.get(new TypePair(fromType, toType));
    if (conversion != null) {
      return Optional.of(conversion);
    }

    // Normalize types and try again
    String normalizedFrom = normalizeType(fromType);
    String normalizedTo = normalizeType(toType);

    conversion = conversions.get(new TypePair(normalizedFrom, normalizedTo));
    return Optional.ofNullable(conversion);
  }

  private String normalizeType(String type) {
    type = type.toUpperCase().trim();

    // Remove length specifiers for generic matching
    if (type.startsWith("VARCHAR")) {
      return "VARCHAR";
    }
    if (type.startsWith("CHAR(")) {
      return "CHAR";
    }

    return type;
  }

  /**
   * Represents a pair of types for conversion lookup
   */
  @Data
  private static class TypePair {
    private final String fromType;
    private final String toType;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof TypePair))
        return false;
      TypePair that = (TypePair) o;
      return fromType.equalsIgnoreCase(that.fromType) && toType.equalsIgnoreCase(that.toType);
    }

    @Override
    public int hashCode() {
      return (fromType.toUpperCase() + "→" + toType.toUpperCase()).hashCode();
    }
  }

  /**
   * Represents a type conversion strategy
   */
  @Data
  public static class TypeConversion {
    private final String sqlTemplate;
    private final boolean risky;
    private final String warning;

    public static TypeConversion safe(String sqlTemplate) {
      return new TypeConversion(sqlTemplate, false, null);
    }

    public static TypeConversion risky(String sqlTemplate, String warning) {
      return new TypeConversion(sqlTemplate, true, warning);
    }

    public String generateSql(String table, String column, Integer maxLength) {
      return sqlTemplate.replace("{table}", table).replace("{column}", column)
          .replace("{maxLength}", maxLength != null ? maxLength.toString() : "255");
    }
  }
}
