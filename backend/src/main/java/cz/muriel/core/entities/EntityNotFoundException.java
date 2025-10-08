package cz.muriel.core.entities;

/**
 * Exception thrown when entity is not found
 */
public class EntityNotFoundException extends RuntimeException {
    
    private final String entityType;
    private final String entityId;
    
    public EntityNotFoundException(String entityType, String entityId) {
        super(String.format("%s with id %s not found", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
}
