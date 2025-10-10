package cz.muriel.core.streaming;

import cz.muriel.core.config.StreamingConfig;
import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.StreamingEntityConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 📊 Topic Ensurer
 * 
 * Ensures Kafka topics exist on application startup based on metamodel configuration.
 * Creates topics for:
 * - entity.inflight.{entity} - pre-event notifications (short retention)
 * - entity.events.{entity} - final events (compacted, long retention)
 * - command.{priority}.{entity} - command queues per priority level
 * - dlq.* - dead letter queues
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class TopicEnsurer {

    private final StreamingConfig streamingConfig;
    private final MetamodelLoader metamodelLoader;
    private final AdminClient adminClient;

    public TopicEnsurer(
            StreamingConfig streamingConfig,
            MetamodelLoader metamodelLoader,
            AdminClient adminClient) {
        this.streamingConfig = streamingConfig;
        this.metamodelLoader = metamodelLoader;
        this.adminClient = adminClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureTopics() {
        log.info("🔍 Checking Kafka topics...");

        try {
            GlobalMetamodelConfig globalConfig = metamodelLoader.loadGlobalConfig();
            
            if (!globalConfig.getStreaming().isEnabled()) {
                log.info("⏭️ Streaming disabled in metamodel, skipping topic creation");
                return;
            }

            Map<String, EntitySchema> schemas = metamodelLoader.loadSchemas();
            List<NewTopic> topicsToCreate = new ArrayList<>();

            // Create DLQ topics
            String topicPrefix = streamingConfig.getTopic().getPrefix();
            topicsToCreate.add(createTopic(
                topicPrefix + "." + globalConfig.getStreaming().getDlqTopic(),
                1, // DLQ doesn't need partitions
                globalConfig.getStreaming().getDefaultReplicationFactor()
            ));
            topicsToCreate.add(createTopic(
                topicPrefix + "." + globalConfig.getStreaming().getOutboxDlqTopic(),
                1,
                globalConfig.getStreaming().getDefaultReplicationFactor()
            ));

            // Create topics for each entity
            for (EntitySchema schema : schemas.values()) {
                StreamingEntityConfig entityConfig = schema.getStreaming();
                
                // Skip if entity streaming is explicitly disabled
                if (entityConfig != null && Boolean.FALSE.equals(entityConfig.getEnabled())) {
                    log.info("⏭️ Streaming disabled for entity {}, skipping", schema.getEntity());
                    continue;
                }

                // Use entity config or fall back to global defaults
                int partitions = entityConfig != null && entityConfig.getPartitions() != null 
                    ? entityConfig.getPartitions() 
                    : globalConfig.getStreaming().getDefaultPartitions();
                    
                int replicationFactor = entityConfig != null && entityConfig.getReplicationFactor() != null
                    ? entityConfig.getReplicationFactor()
                    : globalConfig.getStreaming().getDefaultReplicationFactor();

                String entityName = schema.getEntity().toLowerCase();

                // 1. Inflight topic (short retention, non-compacted)
                NewTopic inflightTopic = createTopic(
                    topicPrefix + ".entity.inflight." + entityName,
                    partitions,
                    replicationFactor
                );
                int inflightRetentionMs = (entityConfig != null && entityConfig.getInflightRetentionMinutes() != null
                    ? entityConfig.getInflightRetentionMinutes()
                    : globalConfig.getStreaming().getDefaultInflightRetentionMinutes()) * 60 * 1000;
                inflightTopic.configs(Map.of(
                    "retention.ms", String.valueOf(inflightRetentionMs),
                    "cleanup.policy", "delete"
                ));
                topicsToCreate.add(inflightTopic);

                // 2. Events topic (long retention, compacted)
                NewTopic eventsTopic = createTopic(
                    topicPrefix + ".entity.events." + entityName,
                    partitions,
                    replicationFactor
                );
                int eventRetentionMs = (entityConfig != null && entityConfig.getRetentionHours() != null
                    ? entityConfig.getRetentionHours()
                    : globalConfig.getStreaming().getDefaultRetentionHours()) * 3600 * 1000;
                eventsTopic.configs(Map.of(
                    "retention.ms", String.valueOf(eventRetentionMs),
                    "cleanup.policy", "compact,delete",
                    "min.compaction.lag.ms", "3600000" // 1 hour
                ));
                topicsToCreate.add(eventsTopic);

                // 3. Command topics per priority
                int cmdRetentionMs = (entityConfig != null && entityConfig.getCommandRetentionHours() != null
                    ? entityConfig.getCommandRetentionHours()
                    : globalConfig.getStreaming().getDefaultCommandRetentionHours()) * 3600 * 1000;
                    
                for (String priority : Arrays.asList("critical", "high", "normal", "bulk")) {
                    NewTopic commandTopic = createTopic(
                        topicPrefix + ".command." + priority + "." + entityName,
                        partitions,
                        replicationFactor
                    );
                    commandTopic.configs(Map.of(
                        "retention.ms", String.valueOf(cmdRetentionMs),
                        "cleanup.policy", "delete"
                    ));
                    topicsToCreate.add(commandTopic);
                }

                log.info("📝 Prepared topics for entity: {}", schema.getEntity());
            }

            // Create topics
            adminClient.createTopics(topicsToCreate).all().get(30, TimeUnit.SECONDS);
            log.info("✅ Successfully ensured {} Kafka topics", topicsToCreate.size());

        } catch (Exception e) {
            log.error("❌ Failed to ensure Kafka topics", e);
            throw new RuntimeException("Failed to ensure Kafka topics", e);
        }
    }

    private NewTopic createTopic(String name, int partitions, int replicationFactor) {
        return new NewTopic(name, partitions, (short) replicationFactor);
    }
}
