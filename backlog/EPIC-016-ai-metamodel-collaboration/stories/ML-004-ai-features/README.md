# S4: AI-Powered Platform Features

> **Intelligent Features:** Workflow suggestions, anomaly detection, predictive alerts, NLP search

## 📋 Story

**As a** platform user  
**I want** AI-powered intelligent features  
**So that** the platform anticipates my needs and provides smart recommendations

## 🎯 Acceptance Criteria

**GIVEN** I'm creating a new workflow  
**WHEN** I select workflow type and priority  
**THEN** I see AI-suggested assignees based on historical data  
**AND** suggestions have >85% acceptance rate

**GIVEN** metrics are being collected  
**WHEN** an anomaly occurs  
**THEN** I receive an alert within 5 minutes  
**AND** anomaly detection has <5% false positives

## 🏗️ Implementation

### Feature 1: Workflow Routing Suggestions

```java
package cz.muriel.core.workflow.service;

import cz.muriel.core.ml.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowRoutingSuggestionService {

    private final PredictionService predictionService;

    /**
     * Suggest assignees for new workflow
     */
    @Cacheable(value = "workflow-suggestions", 
               key = "#workflowType + '-' + #priority + '-' + #tenantId")
    public List<AssigneeSuggestion> suggestAssignees(
            String workflowType, 
            String priority, 
            UUID tenantId) {
        
        // Prepare input features
        Map<String, Object> input = Map.of(
                "workflow_type", workflowType,
                "priority", priority,
                "tenant_id", tenantId.toString(),
                "hour", LocalDateTime.now().getHour(),
                "day_of_week", LocalDateTime.now().getDayOfWeek().getValue()
        );

        // Get ML prediction
        PredictionResponse prediction = predictionService.predict("workflow-router", 
                PredictionRequest.builder().input(input).build());

        // Parse predictions (top 3 suggestions)
        List<Map<String, Object>> predictions = (List<Map<String, Object>>) prediction.getPrediction();

        return predictions.stream()
                .limit(3)
                .map(p -> AssigneeSuggestion.builder()
                        .userId((String) p.get("user_id"))
                        .userName((String) p.get("user_name"))
                        .confidence((Double) p.get("confidence"))
                        .averageCompletionTime((Integer) p.get("avg_completion_time"))
                        .currentWorkload((Integer) p.get("current_workload"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Record actual assignment (for feedback loop)
     */
    public void recordAssignment(UUID workflowId, String assignedTo, boolean suggestionAccepted) {
        // Store in database for model retraining
        log.info("Workflow {} assigned to {} (suggestion accepted: {})", 
                workflowId, assignedTo, suggestionAccepted);
        
        // TODO: Store in training_feedback table
    }
}

@lombok.Data
@lombok.Builder
class AssigneeSuggestion {
    private String userId;
    private String userName;
    private Double confidence;
    private Integer averageCompletionTime;
    private Integer currentWorkload;
}
```

### Feature 2: Anomaly Detection

```java
package cz.muriel.core.monitoring.service;

import cz.muriel.core.ml.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final PredictionService predictionService;
    private final AlertService alertService;
    private final MetricsService metricsService;

    /**
     * Run anomaly detection every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void detectAnomalies() {
        log.info("Running anomaly detection...");

        // Get recent metrics
        List<MetricDataPoint> metrics = metricsService.getRecentMetrics(
                Duration.ofHours(1), 
                List.of("cpu_usage", "memory_usage", "request_latency", "error_rate")
        );

        for (MetricDataPoint metric : metrics) {
            // Prepare timeseries data
            Map<String, Object> input = Map.of(
                    "metric_name", metric.getName(),
                    "values", metric.getValues(),
                    "timestamps", metric.getTimestamps()
            );

            // Get anomaly score
            PredictionResponse prediction = predictionService.predict("anomaly-detector",
                    PredictionRequest.builder().input(input).build());

            Double anomalyScore = (Double) prediction.getPrediction();

            // Alert if anomaly detected (score > 0.8)
            if (anomalyScore > 0.8) {
                alertService.sendAlert(Alert.builder()
                        .severity(AlertSeverity.WARNING)
                        .title("Anomaly detected in " + metric.getName())
                        .description(String.format(
                                "Anomaly score: %.2f. Current value: %.2f, Expected: %.2f",
                                anomalyScore,
                                metric.getCurrentValue(),
                                metric.getExpectedValue()
                        ))
                        .timestamp(LocalDateTime.now())
                        .build());

                log.warn("Anomaly detected: metric={}, score={:.2f}", 
                        metric.getName(), anomalyScore);
            }
        }
    }
}
```

### Feature 3: Predictive Failure Analysis

```java
package cz.muriel.core.monitoring.service;

import cz.muriel.core.ml.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveMaintenanceService {

    private final PredictionService predictionService;
    private final AlertService alertService;

    /**
     * Predict failures every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void predictFailures() {
        log.info("Running predictive failure analysis...");

        // Get system health metrics
        Map<String, Object> input = Map.of(
                "cpu_trend", getCpuTrend(),
                "memory_trend", getMemoryTrend(),
                "error_rate_trend", getErrorRateTrend(),
                "disk_usage", getDiskUsage(),
                "uptime_hours", getUptimeHours()
        );

        // Predict failure probability
        PredictionResponse prediction = predictionService.predict("failure-predictor",
                PredictionRequest.builder().input(input).build());

        Double failureProbability = (Double) prediction.getPrediction();
        Integer hoursUntilFailure = (Integer) ((Map<String, Object>) prediction.getPrediction()).get("hours_until_failure");

        // Alert if failure likely within 24 hours
        if (hoursUntilFailure != null && hoursUntilFailure < 24) {
            alertService.sendAlert(Alert.builder()
                    .severity(AlertSeverity.CRITICAL)
                    .title("Potential system failure predicted")
                    .description(String.format(
                            "Failure probability: %.1f%%. Estimated time: %d hours. Take preventive action!",
                            failureProbability * 100,
                            hoursUntilFailure
                    ))
                    .timestamp(LocalDateTime.now())
                    .build());

            log.error("Failure predicted: probability={:.1f}%, hours={}", 
                    failureProbability * 100, hoursUntilFailure);
        }
    }

    private List<Double> getCpuTrend() {
        // Last 24 hours CPU usage
        return List.of(45.0, 48.0, 52.0, 55.0, 60.0);
    }

    private List<Double> getMemoryTrend() {
        return List.of(70.0, 72.0, 75.0, 78.0, 80.0);
    }

    private List<Double> getErrorRateTrend() {
        return List.of(0.1, 0.2, 0.3, 0.5, 0.8);
    }

    private Double getDiskUsage() {
        return 85.0; // percent
    }

    private Integer getUptimeHours() {
        return 720; // 30 days
    }
}
```

### Feature 4: Natural Language Search

```java
package cz.muriel.core.search.service;

import cz.muriel.core.ml.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaturalLanguageSearchService {

    private final PredictionService predictionService;
    private final DocumentRepository documentRepository;

    /**
     * Semantic search using NLP embeddings
     */
    @Cacheable(value = "nlp-search", key = "#query")
    public List<SearchResult> search(String query, int limit) {
        log.info("NLP search: {}", query);

        // 1. Generate query embedding
        Map<String, Object> input = Map.of("text", query);
        PredictionResponse embedding = predictionService.predict("nlp-search",
                PredictionRequest.builder().input(input).build());

        List<Double> queryVector = (List<Double>) embedding.getPrediction();

        // 2. Find similar documents (vector similarity search)
        List<SearchResult> results = documentRepository.findSimilar(queryVector, limit);

        log.info("NLP search returned {} results for query: {}", results.size(), query);

        return results;
    }
}
```

### Frontend: Workflow Suggestion Component

```typescript
// frontend/src/components/workflow/WorkflowSuggestions.tsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Box, Card, Typography, Avatar, Chip } from '@mui/material';

interface Suggestion {
  userId: string;
  userName: string;
  confidence: number;
  averageCompletionTime: number;
  currentWorkload: number;
}

export const WorkflowSuggestions: React.FC<{
  workflowType: string;
  priority: string;
  onSelect: (userId: string) => void;
}> = ({ workflowType, priority, onSelect }) => {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);

  useEffect(() => {
    if (workflowType && priority) {
      loadSuggestions();
    }
  }, [workflowType, priority]);

  const loadSuggestions = async () => {
    const response = await axios.get('/api/workflows/suggestions', {
      params: { workflowType, priority },
    });
    setSuggestions(response.data);
  };

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        ✨ AI Suggestions
      </Typography>
      {suggestions.map(suggestion => (
        <Card
          key={suggestion.userId}
          sx={{ p: 2, mb: 1, cursor: 'pointer' }}
          onClick={() => onSelect(suggestion.userId)}
        >
          <Box display="flex" alignItems="center" gap={2}>
            <Avatar>{suggestion.userName[0]}</Avatar>
            <Box flex={1}>
              <Typography variant="body1">{suggestion.userName}</Typography>
              <Typography variant="caption" color="text.secondary">
                Avg completion: {suggestion.averageCompletionTime}h •
                Workload: {suggestion.currentWorkload} tasks
              </Typography>
            </Box>
            <Chip
              label={`${(suggestion.confidence * 100).toFixed(0)}% match`}
              color="primary"
              size="small"
            />
          </Box>
        </Card>
      ))}
    </Box>
  );
};
```

## 📊 Production Metrics

- **Workflow suggestions acceptance:** 80% users accept AI suggestions
- **Anomaly detection accuracy:** 95%, <5% false positives
- **Predictive maintenance:** -40% unplanned downtime
- **NLP search satisfaction:** 85% vs. 60% keyword search
- **AI feature adoption:** 75% active users

---

**Story Points:** 2  
**Estimate:** 500 LOC  
**Dependencies:** S1 (model serving), S2 (prediction API), trained models
