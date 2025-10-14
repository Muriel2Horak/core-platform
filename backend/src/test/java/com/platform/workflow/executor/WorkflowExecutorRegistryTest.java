package com.platform.workflow.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * 🧪 W7: WorkflowExecutorRegistry Unit Tests
 * 
 * Tests executor registration, lookup, and action matching.
 * 
 * @since 2025-01-14
 */
class WorkflowExecutorRegistryTest {

    private WorkflowExecutorRegistry registry;
    private TestExecutor testExecutor;

    @BeforeEach
    void setUp() {
        registry = new WorkflowExecutorRegistry();
        testExecutor = new TestExecutor();
        registry.register(testExecutor);
    }

    @Test
    void shouldRegisterExecutor() {
        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.getByName("test-executor")).isPresent();
    }

    @Test
    void shouldFindExecutorByName() {
        var executor = registry.getByName("test-executor");
        
        assertThat(executor).isPresent();
        assertThat(executor.get()).isEqualTo(testExecutor);
    }

    @Test
    void shouldReturnEmptyForUnknownName() {
        var executor = registry.getByName("unknown");
        
        assertThat(executor).isEmpty();
    }

    @Test
    void shouldFindExecutorsForAction() {
        var executors = registry.findExecutorsForAction("test-action");
        
        assertThat(executors).hasSize(1);
        assertThat(executors.get(0)).isEqualTo(testExecutor);
    }

    @Test
    void shouldReturnEmptyListForUnsupportedAction() {
        var executors = registry.findExecutorsForAction("unsupported-action");
        
        assertThat(executors).isEmpty();
    }

    @Test
    void shouldCheckIfHasExecutorForAction() {
        assertThat(registry.hasExecutorForAction("test-action")).isTrue();
        assertThat(registry.hasExecutorForAction("unsupported")).isFalse();
    }

    @Test
    void shouldGetAllExecutors() {
        var allExecutors = registry.getAllExecutors();
        
        assertThat(allExecutors).hasSize(1);
        assertThat(allExecutors).contains(testExecutor);
    }

    @Test
    void shouldRegisterMultipleExecutors() {
        var executor2 = new TestExecutor("executor-2", "action-2");
        registry.register(executor2);
        
        assertThat(registry.size()).isEqualTo(2);
        assertThat(registry.getAllExecutors()).containsExactlyInAnyOrder(testExecutor, executor2);
    }

    @Test
    void shouldClearAllExecutors() {
        registry.clear();
        
        assertThat(registry.size()).isZero();
        assertThat(registry.getAllExecutors()).isEmpty();
    }

    // Test executor implementation
    static class TestExecutor implements WorkflowExecutor {
        private final String name;
        private final String supportedAction;

        TestExecutor() {
            this("test-executor", "test-action");
        }

        TestExecutor(String name, String supportedAction) {
            this.name = name;
            this.supportedAction = supportedAction;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public CompletableFuture<Map<String, Object>> execute(
            String entityType, String entityId, String actionCode, Map<String, Object> context
        ) {
            return CompletableFuture.completedFuture(Map.of("result", "ok"));
        }

        @Override
        public CompletableFuture<Void> compensate(
            String entityType, String entityId, String actionCode, Map<String, Object> originalContext
        ) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean supports(String actionCode) {
            return supportedAction.equals(actionCode);
        }
    }
}
