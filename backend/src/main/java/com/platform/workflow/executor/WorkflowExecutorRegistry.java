package com.platform.workflow.executor;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔄 W7: Workflow Executor Registry
 * 
 * Central registry for all workflow executors. Supports dynamic executor
 * registration and lookup.
 * 
 * @since 2025-01-14
 */
@Component
public class WorkflowExecutorRegistry {

  private final Map<String, WorkflowExecutor> executorsByName = new ConcurrentHashMap<>();
  private final Map<String, List<WorkflowExecutor>> executorsByAction = new ConcurrentHashMap<>();

  /**
   * Register an executor (called automatically by Spring for @Component
   * executors)
   */
  public void register(WorkflowExecutor executor) {
    executorsByName.put(executor.getName(), executor);
    // Executors declare which actions they support via supports() method
  }

  /**
   * Find executor by name
   */
  public Optional<WorkflowExecutor> getByName(String name) {
    return Optional.ofNullable(executorsByName.get(name));
  }

  /**
   * Find all executors that support a given action
   */
  public List<WorkflowExecutor> findExecutorsForAction(String actionCode) {
    return executorsByAction.computeIfAbsent(actionCode,
        action -> executorsByName.values().stream().filter(ex -> ex.supports(action)).toList());
  }

  /**
   * Get all registered executors
   */
  public Collection<WorkflowExecutor> getAllExecutors() {
    return Collections.unmodifiableCollection(executorsByName.values());
  }

  /**
   * Check if any executor supports the action
   */
  public boolean hasExecutorForAction(String actionCode) {
    return !findExecutorsForAction(actionCode).isEmpty();
  }

  /**
   * Get executor count
   */
  public int size() {
    return executorsByName.size();
  }

  /**
   * Clear all executors (for testing)
   */
  public void clear() {
    executorsByName.clear();
    executorsByAction.clear();
  }
}
