package cz.muriel.core.web;

import cz.muriel.core.entities.EntityNotFoundException;
import cz.muriel.core.entities.VersionMismatchException;
import cz.muriel.core.locks.LockConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for metamodel operations
 */
@Slf4j
@RestControllerAdvice
public class MetamodelExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.debug("Entity not found: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "entity_not_found");
        body.put("message", ex.getMessage());
        body.put("entityType", ex.getEntityType());
        body.put("entityId", ex.getEntityId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
    
    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleVersionMismatch(VersionMismatchException ex) {
        log.debug("Version mismatch: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "version_mismatch");
        body.put("message", ex.getMessage());
        body.put("currentVersion", ex.getCurrentVersion());
        
        if (ex.getServerEntity() != null) {
            body.put("serverEntity", ex.getServerEntity());
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    
    @ExceptionHandler(LockConflictException.class)
    public ResponseEntity<Map<String, Object>> handleLockConflict(LockConflictException ex) {
        log.debug("Lock conflict: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "lock_conflict");
        body.put("message", ex.getMessage());
        body.put("existingLock", ex.getExistingLock());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "access_denied");
        body.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Invalid request: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "invalid_request");
        body.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "internal_error");
        body.put("message", "An unexpected error occurred");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
