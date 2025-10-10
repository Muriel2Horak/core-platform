package cz.muriel.core.streaming.service;

import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.StreamingEntityConfig;
import cz.muriel.core.streaming.entity.WorkState;
import cz.muriel.core.streaming.repository.WorkStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * 🔒 Work State Service
 * 
 * Provides work state checking for strict reads mode
 */
@Slf4j @Service @ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class WorkStateService {

  private final WorkStateRepository workStateRepository;
  @SuppressWarnings("unused") // Used in constructor to load schemas
  private final MetamodelLoader metamodelLoader;
  private Map<String, EntitySchema> schemas;

  public WorkStateService(WorkStateRepository workStateRepository,
      MetamodelLoader metamodelLoader) {
    this.workStateRepository = workStateRepository;
    this.metamodelLoader = metamodelLoader;
    this.schemas = metamodelLoader.loadSchemas();
  }

  /**
   * Check if entity is being updated (for strict reads mode)
   * 
   * @param entityName Entity name (e.g., "User", "Group")
   * @param entityId Entity ID
   * @return WorkStateInfo with status
   */
  public WorkStateInfo checkWorkState(String entityName, UUID entityId) {
    EntitySchema schema = schemas.get(entityName);
    if (schema == null) {
      return new WorkStateInfo(false, null);
    }

    StreamingEntityConfig streamingConfig = schema.getStreaming();
    if (streamingConfig == null) {
      return new WorkStateInfo(false, null);
    }

    // If strict reads is not enabled, return immediately
    if (!Boolean.TRUE.equals(streamingConfig.getStrictReads())) {
      return new WorkStateInfo(false, null);
    }

    // Check work state
    return workStateRepository.findByEntityAndEntityId(entityName, entityId).map(ws -> {
      boolean isUpdating = "updating".equals(ws.getStatus());
      return new WorkStateInfo(isUpdating, ws);
    }).orElse(new WorkStateInfo(false, null));
  }

  /**
   * Throw 423 Locked if entity is updating in strict reads mode
   */
  public void enforceStrictReads(String entityName, UUID entityId) {
    WorkStateInfo info = checkWorkState(entityName, entityId);
    if (info.isUpdating()) {
      throw new ResponseStatusException(HttpStatus.LOCKED,
          String.format("Entity %s/%s is currently being updated", entityName, entityId));
    }
  }

  /**
   * Work state info DTO
   */
  public static class WorkStateInfo {
    private final boolean updating;
    private final WorkState workState;

    public WorkStateInfo(boolean updating, WorkState workState) {
      this.updating = updating;
      this.workState = workState;
    }

    public boolean isUpdating() {
      return updating;
    }

    public WorkState getWorkState() {
      return workState;
    }
  }
}
