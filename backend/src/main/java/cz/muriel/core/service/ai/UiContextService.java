package cz.muriel.core.service.ai;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UI Context Service for AI agents
 * 
 * Provides UI metadata (screens, fields, widgets) without actual data.
 * META_ONLY mode - strictly metadata, no values.
 * 
 * @since 2025-10-14
 */
@Slf4j @Service @RequiredArgsConstructor
public class UiContextService {

  private final MetamodelRegistry metamodelRegistry;

  /**
   * Get UI context for route
   * 
   * @param routeId Route identifier (e.g., "users.detail", "proposals.review")
   * @return UI context (screen, fields, widgets, validations)
   */
  public Map<String, Object> getCurrentView(String routeId) {
    log.debug("Getting UI context for route: {}", routeId);

    // Parse route to extract entity and view kind
    RouteInfo routeInfo = parseRoute(routeId);

    EntitySchema schema = metamodelRegistry.getSchema(routeInfo.entityName).orElseThrow(
        () -> new IllegalArgumentException("Entity not found: " + routeInfo.entityName));

    return buildUiContext(schema, routeInfo);
  }

  /**
   * Build UI context from entity schema and route info
   */
  private Map<String, Object> buildUiContext(EntitySchema schema, RouteInfo routeInfo) {
    Map<String, Object> context = new LinkedHashMap<>();

    // Screen metadata
    Map<String, Object> screen = new LinkedHashMap<>();
    screen.put("title", formatTitle(schema.getEntity(), routeInfo.viewKind));
    screen.put("routeId", routeInfo.originalRoute);
    screen.put("entity", schema.getEntity());
    screen.put("viewKind", routeInfo.viewKind);

    // Determine widgets based on view kind
    List<String> widgets = determineWidgets(routeInfo.viewKind);
    screen.put("widgets", widgets);

    context.put("screen", screen);

    // Fields (metadata only)
    List<Map<String, Object>> fields = schema.getFields().stream().map(this::serializeFieldMetadata)
        .collect(Collectors.toList());
    context.put("fields", fields);

    // Validations
    List<Map<String, Object>> validations = schema.getFields().stream()
        .filter(f -> Boolean.TRUE.equals(f.getRequired()) || f.getMaxLength() != null)
        .map(this::serializeValidation).collect(Collectors.toList());
    context.put("validations", validations);

    return context;
  }

  /**
   * Serialize field metadata for AI context (META_ONLY)
   */
  private Map<String, Object> serializeFieldMetadata(FieldSchema field) {
    Map<String, Object> result = new LinkedHashMap<>();

    result.put("name", field.getName());
    result.put("type", field.getType());
    result.put("label", formatLabel(field.getName()));

    if (Boolean.TRUE.equals(field.getRequired())) {
      result.put("required", true);
    }
    if (Boolean.TRUE.equals(field.getPii())) {
      result.put("pii", true);
    }
    if (Boolean.TRUE.equals(field.getHelpSafe())) {
      result.put("helpSafe", true);
    }
    if (field.getMask() != null) {
      result.put("maskPattern", field.getMask());
    }
    if (field.getMaxLength() != null) {
      result.put("maxLength", field.getMaxLength());
    }

    return result;
  }

  /**
   * Serialize validation rule
   */
  private Map<String, Object> serializeValidation(FieldSchema field) {
    Map<String, Object> result = new LinkedHashMap<>();

    result.put("field", field.getName());

    if (Boolean.TRUE.equals(field.getRequired())) {
      result.put("rule", "required");
      result.put("message", formatLabel(field.getName()) + " is required");
    } else if (field.getMaxLength() != null) {
      result.put("rule", "maxLength");
      result.put("value", field.getMaxLength());
      result.put("message", formatLabel(field.getName()) + " must be less than "
          + field.getMaxLength() + " characters");
    }

    return result;
  }

  /**
   * Parse route ID into components
   */
  private RouteInfo parseRoute(String routeId) {
    if (routeId == null || routeId.isEmpty()) {
      throw new IllegalArgumentException("Route ID cannot be empty");
    }

    String[] parts = routeId.split("\\.");
    String entityPart = parts[0];
    String viewKind = parts.length > 1 ? parts[1] : "list";

    // Convert plural to singular and capitalize
    String entityName = singularize(entityPart);
    entityName = Character.toUpperCase(entityName.charAt(0)) + entityName.substring(1);

    RouteInfo info = new RouteInfo();
    info.originalRoute = routeId;
    info.entityName = entityName;
    info.viewKind = viewKind;

    return info;
  }

  /**
   * Determine widgets for view kind
   */
  private List<String> determineWidgets(String viewKind) {
    return switch (viewKind) {
    case "list" -> List.of("table", "filter", "pagination", "actions");
    case "detail" -> List.of("form", "actions", "relatedEntities");
    case "edit", "create" -> List.of("form", "validation", "actions");
    case "wizard" -> List.of("stepper", "form", "validation", "actions");
    default -> List.of("unknown");
    };
  }

  /**
   * Format field name as label
   */
  private String formatLabel(String fieldName) {
    return Arrays.stream(fieldName.split("_"))
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  /**
   * Format entity and view kind as title
   */
  private String formatTitle(String entity, String viewKind) {
    String action = switch (viewKind) {
    case "list" -> "List";
    case "detail" -> "Detail";
    case "edit" -> "Edit";
    case "create" -> "Create";
    case "wizard" -> "Wizard";
    default -> "";
    };

    return entity + " " + action;
  }

  /**
   * Simple singularization (removes trailing 's')
   */
  private String singularize(String plural) {
    if (plural.endsWith("s")) {
      return plural.substring(0, plural.length() - 1);
    }
    return plural;
  }

  /**
   * Route info holder
   */
  private static class RouteInfo {
    String originalRoute;
    String entityName;
    String viewKind;
  }
}
