package com.platform.workflow.executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * 🔄 W7: Workflow Executor Configuration
 * 
 * Auto-registers all @Component executors on application startup.
 * 
 * @since 2025-01-14
 */
@Configuration
public class WorkflowExecutorConfig {

    private final WorkflowExecutorRegistry registry;
    private final List<WorkflowExecutor> executors;

    public WorkflowExecutorConfig(
        WorkflowExecutorRegistry registry,
        List<WorkflowExecutor> executors
    ) {
        this.registry = registry;
        this.executors = executors;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void registerExecutors() {
        executors.forEach(executor -> {
            registry.register(executor);
            org.slf4j.LoggerFactory.getLogger(getClass())
                .info("Registered workflow executor: {}", executor.getName());
        });
    }
}
