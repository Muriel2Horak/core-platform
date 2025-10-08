package cz.muriel.core.controller;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FeatureConfig;
import cz.muriel.core.metamodel.schema.MenuItemConfig;
import cz.muriel.core.security.PolicyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UI Capabilities endpoint - generates menu and features from metamodel
 */
@Slf4j
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MetamodelUiCapabilitiesController {
    
    private final MetamodelRegistry registry;
    private final PolicyEngine policyEngine;
    
    @GetMapping("/ui-capabilities")
    public ResponseEntity<UiCapabilities> getUiCapabilities(Authentication auth) {
        log.debug("Getting UI capabilities for user: {}", auth != null ? auth.getName() : "anonymous");
        
        List<String> menu = new ArrayList<>();
        List<String> features = new ArrayList<>();
        
        // Collect from all metamodel schemas
        for (EntitySchema schema : registry.getAllSchemas().values()) {
            
            // Add menu items
            if (schema.getNavigation() != null && schema.getNavigation().getMenu() != null) {
                for (MenuItemConfig item : schema.getNavigation().getMenu()) {
                    if (hasAccess(auth, item.getRequiredRole())) {
                        menu.add(item.getId());
                        log.debug("Added menu item: {} for user: {}", item.getId(), 
                            auth != null ? auth.getName() : "anonymous");
                    }
                }
            }
            
            // Add features
            if (schema.getFeatures() != null) {
                for (FeatureConfig feature : schema.getFeatures()) {
                    if (hasAccess(auth, feature.getRequiredRole())) {
                        features.add(feature.getId());
                        log.debug("Added feature: {} for user: {}", feature.getId(), 
                            auth != null ? auth.getName() : "anonymous");
                    }
                }
            }
        }
        
        // Generate ETag from timestamp (simple cache invalidation)
        String etag = "W/\"" + System.currentTimeMillis() + "\"";
        
        UiCapabilities capabilities = new UiCapabilities(menu, features);
        
        log.info("UI capabilities for {}: menu={}, features={}", 
            auth != null ? auth.getName() : "anonymous", 
            menu.size(), 
            features.size());
        
        return ResponseEntity.ok()
            .eTag(etag)
            .body(capabilities);
    }
    
    private boolean hasAccess(Authentication auth, String requiredRole) {
        if (requiredRole == null || requiredRole.isBlank()) {
            return true; // No role required
        }
        
        if (auth == null) {
            return false;
        }
        
        return policyEngine.hasRole(auth, requiredRole);
    }
    
    /**
     * UI Capabilities response
     */
    public record UiCapabilities(
        List<String> menu,
        List<String> features
    ) {}
}
