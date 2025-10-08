package cz.muriel.core.entities;

import lombok.Getter;
import java.util.Map;

/**
 * Exception thrown when optimistic lock version mismatch occurs
 */
@Getter
public class VersionMismatchException extends RuntimeException {
    
    private final long currentVersion;
    private final Map<String, Object> serverEntity;
    
    public VersionMismatchException(String message, long currentVersion, Map<String, Object> serverEntity) {
        super(message);
        this.currentVersion = currentVersion;
        this.serverEntity = serverEntity;
    }
}
