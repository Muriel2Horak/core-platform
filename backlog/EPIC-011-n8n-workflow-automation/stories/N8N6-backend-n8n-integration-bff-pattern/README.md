# S6: Backend n8n Integration (BFF Pattern)

> **Backend Integration:** n8n REST API client, role-based proxy, workflow monitoring endpoints

## 📋 Story

**As a** platform user  
**I want** to view n8n workflow status in the main application UI  
**So that** I can monitor automation processes without leaving the application

**As a** platform administrator  
**I want** role-based access to n8n workflows  
**So that** regular users can monitor but only admins can edit workflows

## 🎯 Acceptance Criteria

**GIVEN** user with `n8n-users` role  
**WHEN** accessing workflow dashboard  
**THEN** sees list of workflows with status, success rate, recent executions  
**AND** can monitor real-time workflow executions  
**AND** CANNOT access n8n admin UI

**GIVEN** user with `n8n-admins` role  
**WHEN** accessing workflow dashboard  
**THEN** sees all monitoring data as regular user  
**AND** sees "Open in n8n" link for each workflow  
**AND** can activate/deactivate workflows via API  
**AND** can access n8n admin UI at `/n8n/*`

**GIVEN** workflow execution in progress  
**WHEN** dashboard polls for updates  
**THEN** status updates within 5 seconds  
**AND** shows real-time progress (running/success/error)

## 🏗️ Implementation

### 1. Backend n8n API Client

```java
// backend/src/main/java/cz/muriel/core/n8n/client/N8nApiClient.java

package cz.muriel.core.n8n.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * n8n REST API client for workflow management and monitoring.
 * 
 * API Documentation: https://docs.n8n.io/api/
 * 
 * Supported Operations:
 * - GET /workflows - List all workflows
 * - GET /workflows/:id - Get workflow details
 * - POST /workflows/:id/activate - Activate workflow
 * - POST /workflows/:id/deactivate - Deactivate workflow
 * - GET /workflows/:id/executions - Get execution history
 * - GET /executions/:id - Get execution details
 * - DELETE /executions/:id - Delete execution record
 */
@Slf4j
@Service
public class N8nApiClient {
    
    private final WebClient webClient;
    
    public N8nApiClient(
        @Value("${n8n.api.url}") String n8nUrl,
        @Value("${n8n.api.key}") String apiKey,
        @Value("${n8n.api.timeout:10}") int timeoutSeconds
    ) {
        this.webClient = WebClient.builder()
            .baseUrl(n8nUrl)  // http://n8n:5678
            .defaultHeader("X-N8N-API-KEY", apiKey)
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Content-Type", "application/json")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(16 * 1024 * 1024))  // 16MB buffer
            .build();
        
        log.info("n8n API client initialized: baseUrl={}", n8nUrl);
    }
    
    /**
     * Get all workflows.
     * 
     * @return List of workflows with metadata
     */
    public List<N8nWorkflow> getWorkflows() {
        log.debug("Fetching all workflows from n8n API");
        
        return webClient.get()
            .uri("/workflows")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<N8nApiResponse<List<N8nWorkflow>>>() {})
            .map(N8nApiResponse::getData)
            .timeout(Duration.ofSeconds(10))
            .doOnError(error -> log.error("Failed to fetch workflows", error))
            .onErrorReturn(List.of())
            .block();
    }
    
    /**
     * Get workflow by ID.
     * 
     * @param id Workflow ID
     * @return Workflow details including nodes and connections
     */
    public Optional<N8nWorkflow> getWorkflow(String id) {
        log.debug("Fetching workflow: id={}", id);
        
        return webClient.get()
            .uri("/workflows/{id}", id)
            .retrieve()
            .bodyToMono(N8nWorkflow.class)
            .timeout(Duration.ofSeconds(10))
            .doOnError(error -> log.error("Failed to fetch workflow: id={}", id, error))
            .blockOptional();
    }
    
    /**
     * Get workflow executions.
     * 
     * @param workflowId Workflow ID
     * @param limit Max number of executions to return
     * @param status Filter by status (running, success, error, waiting)
     * @return List of executions
     */
    public List<N8nExecution> getExecutions(String workflowId, int limit, String status) {
        log.debug("Fetching executions: workflowId={}, limit={}, status={}", 
            workflowId, limit, status);
        
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/workflows/{workflowId}/executions")
                .queryParam("limit", limit)
                .queryParamIfPresent("status", Optional.ofNullable(status))
                .build(workflowId))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<N8nApiResponse<List<N8nExecution>>>() {})
            .map(N8nApiResponse::getData)
            .timeout(Duration.ofSeconds(10))
            .doOnError(error -> log.error("Failed to fetch executions: workflowId={}", 
                workflowId, error))
            .onErrorReturn(List.of())
            .block();
    }
    
    /**
     * Get execution details.
     * 
     * @param executionId Execution ID
     * @return Execution details with run data
     */
    public Optional<N8nExecution> getExecution(String executionId) {
        log.debug("Fetching execution: id={}", executionId);
        
        return webClient.get()
            .uri("/executions/{id}", executionId)
            .retrieve()
            .bodyToMono(N8nExecution.class)
            .timeout(Duration.ofSeconds(10))
            .doOnError(error -> log.error("Failed to fetch execution: id={}", 
                executionId, error))
            .blockOptional();
    }
    
    /**
     * Activate workflow.
     * 
     * @param id Workflow ID
     * @return true if activation successful
     */
    public boolean activateWorkflow(String id) {
        log.info("Activating workflow: id={}", id);
        
        try {
            webClient.post()
                .uri("/workflows/{id}/activate", id)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
            
            log.info("Workflow activated successfully: id={}", id);
            return true;
        } catch (Exception e) {
            log.error("Failed to activate workflow: id={}", id, e);
            return false;
        }
    }
    
    /**
     * Deactivate workflow.
     * 
     * @param id Workflow ID
     * @return true if deactivation successful
     */
    public boolean deactivateWorkflow(String id) {
        log.info("Deactivating workflow: id={}", id);
        
        try {
            webClient.post()
                .uri("/workflows/{id}/deactivate", id)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
            
            log.info("Workflow deactivated successfully: id={}", id);
            return true;
        } catch (Exception e) {
            log.error("Failed to deactivate workflow: id={}", id, e);
            return false;
        }
    }
    
    /**
     * Delete execution record.
     * 
     * @param executionId Execution ID
     * @return true if deletion successful
     */
    public boolean deleteExecution(String executionId) {
        log.info("Deleting execution: id={}", executionId);
        
        try {
            webClient.delete()
                .uri("/executions/{id}", executionId)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
            
            log.info("Execution deleted successfully: id={}", executionId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete execution: id={}", executionId, e);
            return false;
        }
    }
}
```

### 2. n8n Domain Models

```java
// backend/src/main/java/cz/muriel/core/n8n/domain/N8nWorkflow.java

package cz.muriel.core.n8n.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class N8nWorkflow {
    private String id;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<N8nTag> tags;
    private List<N8nNode> nodes;
    private Map<String, List<Map<String, Object>>> connections;
    private Map<String, Object> settings;
    private Map<String, Object> staticData;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class N8nTag {
    private String id;
    private String name;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class N8nNode {
    private String name;
    private String type;
    private Map<String, Integer> position;
    private Map<String, Object> parameters;
    private String credentials;  // Credential ID (not actual credentials)
}
```

```java
// backend/src/main/java/cz/muriel/core/n8n/domain/N8nExecution.java

package cz.muriel.core.n8n.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class N8nExecution {
    private String id;
    private String workflowId;
    private String workflowName;
    private String status;  // running, success, error, waiting
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private String mode;  // trigger, webhook, manual
    private Map<String, Object> data;
    private N8nExecutionError error;
    private boolean finished;
    private Long waitTill;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class N8nExecutionError {
    private String message;
    private String stack;
    private String name;
}
```

```java
// backend/src/main/java/cz/muriel/core/n8n/domain/N8nApiResponse.java

package cz.muriel.core.n8n.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class N8nApiResponse<T> {
    private T data;
}
```

### 3. BFF Proxy Controller

```java
// backend/src/main/java/cz/muriel/core/n8n/N8nProxyController.java

package cz.muriel.core.n8n;

import cz.muriel.core.audit.AuditService;
import cz.muriel.core.n8n.client.N8nApiClient;
import cz.muriel.core.n8n.domain.N8nExecution;
import cz.muriel.core.n8n.domain.N8nWorkflow;
import cz.muriel.core.n8n.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BFF (Backend-for-Frontend) controller for n8n workflow monitoring.
 * 
 * Security:
 * - All endpoints require authentication (JWT token)
 * - Read-only endpoints: n8n-users, n8n-admins
 * - Write endpoints: n8n-admins only
 * 
 * Caching:
 * - Workflows list cached for 5 minutes
 * - Executions NOT cached (real-time data)
 */
@Slf4j
@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class N8nProxyController {
    
    private final N8nApiClient n8nClient;
    private final CacheManager cacheManager;
    private final AuditService auditService;
    
    /**
     * Get all workflows (summary view).
     * 
     * Security: n8n-users, n8n-admins
     * Caching: 5 minutes
     * 
     * @return List of workflow summaries
     */
    @GetMapping("/workflows")
    @PreAuthorize("hasAnyRole('n8n-users', 'n8n-admins')")
    @Cacheable(value = "n8n-workflows", key = "'all'", unless = "#result == null")
    public ResponseEntity<List<WorkflowSummaryDTO>> getWorkflows(
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Fetching workflows for user: {}", jwt.getSubject());
        
        List<N8nWorkflow> workflows = n8nClient.getWorkflows();
        
        List<WorkflowSummaryDTO> summaries = workflows.stream()
            .map(w -> WorkflowSummaryDTO.builder()
                .id(w.getId())
                .name(w.getName())
                .active(w.isActive())
                .tags(w.getTags() != null ? 
                    w.getTags().stream()
                        .map(t -> new TagDTO(t.getId(), t.getName()))
                        .collect(Collectors.toList()) : 
                    List.of())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build())
            .collect(Collectors.toList());
        
        log.info("Returning {} workflows", summaries.size());
        return ResponseEntity.ok(summaries);
    }
    
    /**
     * Get workflow details.
     * 
     * Security: n8n-admins only (contains sensitive data)
     * 
     * @param id Workflow ID
     * @return Workflow details (sanitized - no credentials)
     */
    @GetMapping("/workflows/{id}")
    @PreAuthorize("hasRole('n8n-admins')")
    public ResponseEntity<WorkflowDetailDTO> getWorkflow(
        @PathVariable String id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Fetching workflow detail: id={}, user={}", id, jwt.getSubject());
        
        return n8nClient.getWorkflow(id)
            .map(workflow -> {
                // Sanitize nodes (remove credential IDs)
                List<NodeDTO> sanitizedNodes = workflow.getNodes().stream()
                    .map(node -> NodeDTO.builder()
                        .name(node.getName())
                        .type(node.getType())
                        .position(node.getPosition())
                        // ❌ SKIP: credentials, parameters (may contain secrets)
                        .build())
                    .collect(Collectors.toList());
                
                WorkflowDetailDTO detail = WorkflowDetailDTO.builder()
                    .id(workflow.getId())
                    .name(workflow.getName())
                    .active(workflow.isActive())
                    .nodes(sanitizedNodes)
                    .connections(workflow.getConnections())
                    .createdAt(workflow.getCreatedAt())
                    .updatedAt(workflow.getUpdatedAt())
                    .build();
                
                return ResponseEntity.ok(detail);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get workflow executions.
     * 
     * Security: n8n-users, n8n-admins
     * No caching: Real-time data
     * 
     * @param workflowId Workflow ID
     * @param limit Max results (default: 50)
     * @param status Filter by status (optional)
     * @return List of executions
     */
    @GetMapping("/workflows/{workflowId}/executions")
    @PreAuthorize("hasAnyRole('n8n-users', 'n8n-admins')")
    public ResponseEntity<List<ExecutionDTO>> getExecutions(
        @PathVariable String workflowId,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) String status,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.debug("Fetching executions: workflowId={}, limit={}, status={}, user={}", 
            workflowId, limit, status, jwt.getSubject());
        
        List<N8nExecution> executions = n8nClient.getExecutions(
            workflowId, 
            Math.min(limit, 100),  // Cap at 100
            status
        );
        
        List<ExecutionDTO> dtos = executions.stream()
            .map(this::toExecutionDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get execution details.
     * 
     * Security: n8n-users, n8n-admins
     * 
     * @param executionId Execution ID
     * @return Execution details
     */
    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasAnyRole('n8n-users', 'n8n-admins')")
    public ResponseEntity<ExecutionDetailDTO> getExecution(
        @PathVariable String executionId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.debug("Fetching execution: id={}, user={}", executionId, jwt.getSubject());
        
        return n8nClient.getExecution(executionId)
            .map(execution -> {
                ExecutionDetailDTO detail = ExecutionDetailDTO.builder()
                    .id(execution.getId())
                    .workflowId(execution.getWorkflowId())
                    .workflowName(execution.getWorkflowName())
                    .status(execution.getStatus())
                    .startedAt(execution.getStartedAt())
                    .stoppedAt(execution.getStoppedAt())
                    .duration(calculateDuration(execution))
                    .mode(execution.getMode())
                    .finished(execution.isFinished())
                    .errorMessage(execution.getError() != null ? 
                        execution.getError().getMessage() : null)
                    .build();
                
                return ResponseEntity.ok(detail);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Activate workflow.
     * 
     * Security: n8n-admins only
     * Side effect: Clears workflow cache
     * 
     * @param id Workflow ID
     * @return 200 OK or 500 on error
     */
    @PostMapping("/workflows/{id}/activate")
    @PreAuthorize("hasRole('n8n-admins')")
    @CacheEvict(value = "n8n-workflows", allEntries = true)
    public ResponseEntity<Void> activateWorkflow(
        @PathVariable String id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Activating workflow: id={}, user={}", id, jwt.getSubject());
        
        boolean success = n8nClient.activateWorkflow(id);
        
        if (success) {
            auditService.log(
                "WORKFLOW_ACTIVATED",
                jwt.getSubject(),
                Map.of("workflowId", id)
            );
            
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Deactivate workflow.
     * 
     * Security: n8n-admins only
     * Side effect: Clears workflow cache
     * 
     * @param id Workflow ID
     * @return 200 OK or 500 on error
     */
    @PostMapping("/workflows/{id}/deactivate")
    @PreAuthorize("hasRole('n8n-admins')")
    @CacheEvict(value = "n8n-workflows", allEntries = true)
    public ResponseEntity<Void> deactivateWorkflow(
        @PathVariable String id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Deactivating workflow: id={}, user={}", id, jwt.getSubject());
        
        boolean success = n8nClient.deactivateWorkflow(id);
        
        if (success) {
            auditService.log(
                "WORKFLOW_DEACTIVATED",
                jwt.getSubject(),
                Map.of("workflowId", id)
            );
            
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete execution record.
     * 
     * Security: n8n-admins only
     * 
     * @param executionId Execution ID
     * @return 200 OK or 500 on error
     */
    @DeleteMapping("/executions/{executionId}")
    @PreAuthorize("hasRole('n8n-admins')")
    public ResponseEntity<Void> deleteExecution(
        @PathVariable String executionId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Deleting execution: id={}, user={}", executionId, jwt.getSubject());
        
        boolean success = n8nClient.deleteExecution(executionId);
        
        if (success) {
            auditService.log(
                "EXECUTION_DELETED",
                jwt.getSubject(),
                Map.of("executionId", executionId)
            );
            
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper: Convert to DTO
    private ExecutionDTO toExecutionDTO(N8nExecution execution) {
        return ExecutionDTO.builder()
            .id(execution.getId())
            .workflowId(execution.getWorkflowId())
            .workflowName(execution.getWorkflowName())
            .status(execution.getStatus())
            .startedAt(execution.getStartedAt())
            .stoppedAt(execution.getStoppedAt())
            .duration(calculateDuration(execution))
            .mode(execution.getMode())
            .build();
    }
    
    // Helper: Calculate execution duration
    private Long calculateDuration(N8nExecution execution) {
        if (execution.getStartedAt() == null) {
            return null;
        }
        
        LocalDateTime endTime = execution.getStoppedAt() != null ? 
            execution.getStoppedAt() : 
            LocalDateTime.now();
        
        return Duration.between(execution.getStartedAt(), endTime).toMillis();
    }
}
```

### 4. DTOs

```java
// backend/src/main/java/cz/muriel/core/n8n/dto/WorkflowSummaryDTO.java

package cz.muriel.core.n8n.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkflowSummaryDTO {
    private String id;
    private String name;
    private boolean active;
    private List<TagDTO> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
class TagDTO {
    private String id;
    private String name;
}
```

```java
// backend/src/main/java/cz/muriel/core/n8n/dto/ExecutionDTO.java

package cz.muriel.core.n8n.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionDTO {
    private String id;
    private String workflowId;
    private String workflowName;
    private String status;  // running, success, error, waiting
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private Long duration;  // milliseconds
    private String mode;    // trigger, webhook, manual
}
```

```java
// backend/src/main/java/cz/muriel/core/n8n/dto/ExecutionDetailDTO.java

package cz.muriel.core.n8n.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionDetailDTO {
    private String id;
    private String workflowId;
    private String workflowName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private Long duration;
    private String mode;
    private boolean finished;
    private String errorMessage;
}
```

```java
// backend/src/main/java/cz/muriel/core/n8n/dto/WorkflowDetailDTO.java

package cz.muriel.core.n8n.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowDetailDTO {
    private String id;
    private String name;
    private boolean active;
    private List<NodeDTO> nodes;
    private Map<String, List<Map<String, Object>>> connections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
class NodeDTO {
    private String name;
    private String type;
    private Map<String, Integer> position;
}
```

### 5. Configuration

```yaml
# backend/src/main/resources/application.yml

n8n:
  api:
    url: ${N8N_API_URL:http://n8n:5678}
    key: ${N8N_API_KEY}  # Generated API key from n8n settings
    timeout: 10  # seconds

spring:
  cache:
    cache-names:
      - n8n-workflows
    caffeine:
      spec: maximumSize=100,expireAfterWrite=5m
```

### 6. Frontend React Component

```typescript
// frontend/src/components/n8n/WorkflowDashboard.tsx

import React, { useEffect, useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { apiClient } from '@/api/client';
import { 
  WorkflowSummary, 
  Execution, 
  ExecutionStatus 
} from '@/types/n8n';

export const WorkflowDashboard: React.FC = () => {
  const { user, hasRole } = useAuth();
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);
  const [executions, setExecutions] = useState<Record<string, Execution[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadWorkflows();
    
    // Poll executions every 5 seconds
    const interval = setInterval(() => {
      refreshExecutions();
    }, 5000);
    
    return () => clearInterval(interval);
  }, []);

  const loadWorkflows = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get<WorkflowSummary[]>('/api/n8n/workflows');
      setWorkflows(response.data);
      
      // Load recent executions for each workflow
      response.data.forEach((workflow) => {
        loadExecutions(workflow.id);
      });
    } catch (err) {
      setError('Failed to load workflows');
      console.error('Failed to load workflows:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadExecutions = async (workflowId: string) => {
    try {
      const response = await apiClient.get<Execution[]>(
        `/api/n8n/workflows/${workflowId}/executions?limit=10`
      );
      setExecutions(prev => ({
        ...prev,
        [workflowId]: response.data
      }));
    } catch (err) {
      console.error(`Failed to load executions for ${workflowId}:`, err);
    }
  };

  const refreshExecutions = () => {
    workflows.forEach(workflow => {
      loadExecutions(workflow.id);
    });
  };

  const toggleWorkflow = async (workflowId: string, activate: boolean) => {
    try {
      const endpoint = activate ? 'activate' : 'deactivate';
      await apiClient.post(`/api/n8n/workflows/${workflowId}/${endpoint}`);
      
      // Refresh workflows list
      loadWorkflows();
    } catch (err) {
      console.error(`Failed to ${activate ? 'activate' : 'deactivate'} workflow:`, err);
    }
  };

  const getStatusColor = (status: ExecutionStatus) => {
    switch (status) {
      case 'running': return 'text-blue-600 bg-blue-100';
      case 'success': return 'text-green-600 bg-green-100';
      case 'error': return 'text-red-600 bg-red-100';
      case 'waiting': return 'text-yellow-600 bg-yellow-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const getSuccessRate = (workflowId: string) => {
    const execs = executions[workflowId] || [];
    if (execs.length === 0) return 0;
    
    const successful = execs.filter(e => e.status === 'success').length;
    return Math.round((successful / execs.length) * 100);
  };

  const formatDuration = (ms: number) => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading workflows...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <p className="text-red-800">{error}</p>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          Workflow Monitoring
        </h1>
        <div className="text-sm text-gray-500">
          Auto-refresh: 5s • {workflows.length} workflows
        </div>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {workflows.map(workflow => {
          const recentExecs = executions[workflow.id] || [];
          const runningCount = recentExecs.filter(e => e.status === 'running').length;
          const errorCount = recentExecs.filter(e => e.status === 'error').length;
          const successRate = getSuccessRate(workflow.id);
          
          return (
            <div key={workflow.id} className="bg-white rounded-lg shadow hover:shadow-md transition-shadow">
              <div className="p-6">
                {/* Workflow Header */}
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900">
                      {workflow.name}
                    </h3>
                    <div className="flex items-center gap-2 mt-2">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        workflow.active 
                          ? 'bg-green-100 text-green-700' 
                          : 'bg-gray-100 text-gray-700'
                      }`}>
                        {workflow.active ? 'Active' : 'Inactive'}
                      </span>
                      {workflow.tags.map(tag => (
                        <span 
                          key={tag.id} 
                          className="px-2 py-1 rounded text-xs bg-blue-100 text-blue-700"
                        >
                          {tag.name}
                        </span>
                      ))}
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    {hasRole('n8n-admins') && (
                      <>
                        <button
                          onClick={() => toggleWorkflow(workflow.id, !workflow.active)}
                          className="px-3 py-1 text-sm bg-gray-100 hover:bg-gray-200 rounded"
                        >
                          {workflow.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <a
                          href={`https://core-platform.local/n8n/workflow/${workflow.id}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:text-blue-800 text-sm"
                        >
                          Open in n8n →
                        </a>
                      </>
                    )}
                  </div>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-3 gap-4 mb-4">
                  <div className="text-center p-3 bg-gray-50 rounded">
                    <div className="text-2xl font-bold text-gray-900">
                      {successRate}%
                    </div>
                    <div className="text-xs text-gray-600 mt-1">Success Rate</div>
                  </div>
                  
                  <div className="text-center p-3 bg-gray-50 rounded">
                    <div className="text-2xl font-bold text-gray-900">
                      {recentExecs.length}
                    </div>
                    <div className="text-xs text-gray-600 mt-1">Recent Runs</div>
                  </div>
                  
                  <div className="text-center p-3 bg-gray-50 rounded">
                    <div className="text-2xl font-bold text-blue-600">
                      {runningCount}
                    </div>
                    <div className="text-xs text-gray-600 mt-1">Running</div>
                  </div>
                </div>

                {/* Recent Executions */}
                <div>
                  <h4 className="text-sm font-medium text-gray-700 mb-2">
                    Recent Executions
                  </h4>
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {recentExecs.slice(0, 5).map(exec => (
                      <div 
                        key={exec.id} 
                        className="flex items-center justify-between p-2 bg-gray-50 rounded text-sm hover:bg-gray-100"
                      >
                        <div className="flex items-center gap-2 flex-1">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${
                            getStatusColor(exec.status)
                          }`}>
                            {exec.status}
                          </span>
                          <span className="text-gray-600 text-xs">
                            {new Date(exec.startedAt).toLocaleString('cs-CZ', {
                              month: 'short',
                              day: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit'
                            })}
                          </span>
                        </div>
                        
                        {exec.duration && (
                          <span className="text-gray-500 text-xs">
                            {formatDuration(exec.duration)}
                          </span>
                        )}
                      </div>
                    ))}
                    
                    {recentExecs.length === 0 && (
                      <div className="text-center py-8 text-gray-500 text-sm">
                        No executions yet
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      
      {workflows.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-500 mb-4">No workflows found</div>
          {hasRole('n8n-admins') && (
            <a
              href="https://core-platform.local/n8n/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:text-blue-800"
            >
              Create your first workflow in n8n →
            </a>
          )}
        </div>
      )}
    </div>
  );
};
```

### 7. TypeScript Types

```typescript
// frontend/src/types/n8n.ts

export interface WorkflowSummary {
  id: string;
  name: string;
  active: boolean;
  tags: Tag[];
  createdAt: string;
  updatedAt: string;
}

export interface Tag {
  id: string;
  name: string;
}

export type ExecutionStatus = 'running' | 'success' | 'error' | 'waiting';

export interface Execution {
  id: string;
  workflowId: string;
  workflowName: string;
  status: ExecutionStatus;
  startedAt: string;
  stoppedAt?: string;
  duration?: number;  // milliseconds
  mode: 'trigger' | 'webhook' | 'manual';
}

export interface ExecutionDetail extends Execution {
  finished: boolean;
  errorMessage?: string;
}
```

## 📊 Success Metrics

- **API Response Time:** < 200ms (cached workflows), < 500ms (executions)
- **Cache Hit Rate:** > 80% for workflows list
- **Frontend Poll Frequency:** 5 seconds for real-time updates
- **Error Rate:** < 0.1% for API proxy calls
- **Audit Coverage:** 100% of admin actions logged

## 🔒 Security Considerations

1. **API Key Storage:** n8n API key in environment variable (not committed)
2. **Credential Sanitization:** Node credentials NEVER exposed in API responses
3. **Role-Based Access:** JWT token validation + Spring Security @PreAuthorize
4. **Audit Logging:** All admin actions (activate/deactivate/delete) logged
5. **Rate Limiting:** Nginx rate limiting for /api/n8n/* endpoints (100 req/min)

## 🎯 Implementation Tasks

### Task 1: Backend n8n API Client

**File:** `backend/src/main/java/cz/muriel/core/n8n/client/N8nApiClient.java`
**Estimate:** 3 hours
**Dependencies:** WebClient, n8n running
**Acceptance:**

- [ ] WebClient configured with n8n base URL and API key
- [ ] Methods: getWorkflows(), getWorkflow(id), getExecutions(), activateWorkflow()
- [ ] Error handling with logging
- [ ] Timeout configuration (10s)
- [ ] Unit tests with MockWebServer

### Task 2: n8n Domain Models

**File:** `backend/src/main/java/cz/muriel/core/n8n/domain/*.java`
**Estimate:** 1 hour
**Dependencies:** Jackson
**Acceptance:**

- [ ] N8nWorkflow.java with @JsonIgnoreProperties
- [ ] N8nExecution.java with status enum
- [ ] N8nNode.java (sanitized - no credentials)
- [ ] N8nApiResponse wrapper
- [ ] Lombok @Data annotations

### Task 3: BFF Proxy Controller

**File:** `backend/src/main/java/cz/muriel/core/n8n/N8nProxyController.java`
**Estimate:** 4 hours
**Dependencies:** Task 1, Task 2, Spring Security
**Acceptance:**

- [ ] GET /api/n8n/workflows (@PreAuthorize n8n-users, n8n-admins)
- [ ] GET /api/n8n/workflows/{id}/executions
- [ ] POST /api/n8n/workflows/{id}/activate (admins only)
- [ ] POST /api/n8n/workflows/{id}/deactivate (admins only)
- [ ] @Cacheable for workflows (5 min TTL)
- [ ] AuditService.log() for admin actions
- [ ] Integration tests with @WebMvcTest

### Task 4: DTOs & Mapping

**File:** `backend/src/main/java/cz/muriel/core/n8n/dto/*.java`
**Estimate:** 2 hours
**Dependencies:** Task 2
**Acceptance:**

- [ ] WorkflowSummaryDTO (no sensitive data)
- [ ] ExecutionDTO (status, duration)
- [ ] ExecutionDetailDTO (error message)
- [ ] WorkflowDetailDTO (sanitized nodes)
- [ ] Mapping methods in controller

### Task 5: Cache Configuration

**File:** `backend/src/main/resources/application.yml`
**Estimate:** 30 minutes
**Dependencies:** Spring Cache
**Acceptance:**

- [ ] Caffeine cache config (5 min TTL, 100 max entries)
- [ ] Cache name: n8n-workflows
- [ ] @CacheEvict on activate/deactivate

### Task 6: Frontend Dashboard Component

**File:** `frontend/src/components/n8n/WorkflowDashboard.tsx`
**Estimate:** 5 hours
**Dependencies:** React, apiClient, useAuth
**Acceptance:**

- [ ] Grid layout (2 columns on desktop)
- [ ] Workflow cards with stats (success rate, running count)
- [ ] Real-time execution list (last 5)
- [ ] Auto-refresh every 5 seconds
- [ ] "Open in n8n" link (admins only)
- [ ] Activate/Deactivate buttons (admins only)
- [ ] Loading states, error handling
- [ ] Responsive design (mobile-friendly)

### Task 7: TypeScript Types

**File:** `frontend/src/types/n8n.ts`
**Estimate:** 30 minutes
**Dependencies:** None
**Acceptance:**

- [ ] WorkflowSummary interface
- [ ] Execution interface
- [ ] ExecutionStatus type
- [ ] Tag interface

### Task 8: API Client Integration

**File:** `frontend/src/api/client.ts`
**Estimate:** 1 hour
**Dependencies:** axios, useAuth
**Acceptance:**

- [ ] GET /api/n8n/workflows endpoint
- [ ] GET /api/n8n/workflows/:id/executions endpoint
- [ ] POST /api/n8n/workflows/:id/activate endpoint
- [ ] Authorization header from localStorage JWT
- [ ] Error interceptor

### Task 9: Integration Tests

**File:** `backend/src/test/java/cz/muriel/core/n8n/N8nProxyControllerIntegrationTest.java`
**Estimate:** 3 hours
**Dependencies:** Task 3, @SpringBootTest
**Acceptance:**

- [ ] Test GET /api/n8n/workflows (with mocked n8n API)
- [ ] Test role-based access (users vs admins)
- [ ] Test cache behavior (hit/miss)
- [ ] Test activate/deactivate with audit logging
- [ ] MockMvc + WireMock for n8n API

### Task 10: E2E Tests

**File:** `e2e/specs/n8n/workflow-dashboard.spec.ts`
**Estimate:** 2 hours
**Dependencies:** Playwright, n8n running
**Acceptance:**

- [ ] Login as user → see dashboard (no admin links)
- [ ] Login as admin → see "Open in n8n" links
- [ ] Verify workflow card shows correct stats
- [ ] Verify auto-refresh updates executions
- [ ] Test activate/deactivate workflow

### Task 11: Documentation

**File:** `docs/n8n-integration.md`
**Estimate:** 1 hour
**Dependencies:** All tasks
**Acceptance:**

- [ ] Architecture diagram (Frontend → BFF → n8n)
- [ ] API endpoint documentation
- [ ] Role permissions table
- [ ] Setup instructions (n8n API key generation)
- [ ] Troubleshooting guide

## 📦 Total Effort Estimate

**Total:** ~23 hours (~3 days)

**Breakdown:**

- Backend (Tasks 1-5): 10.5 hours
- Frontend (Tasks 6-8): 6.5 hours
- Testing (Tasks 9-10): 5 hours
- Documentation (Task 11): 1 hour

## 🚀 Deployment Checklist

- [ ] Generate n8n API key in n8n settings UI
- [ ] Add `N8N_API_KEY` to `.env` file
- [ ] Add `N8N_API_URL=http://n8n:5678` to `.env`
- [ ] Create Keycloak groups: `n8n-users`, `n8n-admins`
- [ ] Assign users to appropriate groups
- [ ] Deploy backend changes (`make rebuild-backend`)
- [ ] Deploy frontend changes (`make rebuild-frontend`)
- [ ] Run integration tests (`make test-backend-full`)
- [ ] Run E2E tests (`make test-e2e`)
- [ ] Verify dashboard loads workflows
- [ ] Verify real-time execution updates (5s poll)
- [ ] Test admin activate/deactivate buttons
- [ ] Verify audit logs in database

## 🔗 Dependencies

- **S5: n8n Platform Integration** - n8n must be deployed and accessible
- **Backend:** Spring Boot 3.x, WebClient, Spring Security, Caffeine Cache
- **Frontend:** React 18, TypeScript, Axios, TailwindCSS
- **n8n:** Community Edition v1.x with API enabled
