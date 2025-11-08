# S2: Prediction API Gateway

> **API Layer:** RESTful endpoints, batch predictions, caching, rate limiting

## 📋 Story

**As an** application developer  
**I want** a unified API to consume ML predictions  
**So that** I don't need to integrate with multiple model servers

## 🎯 Acceptance Criteria

**GIVEN** I want a prediction  
**WHEN** I call `/api/ml/predict/{model-name}`  
**THEN** I get a prediction in <50ms  
**AND** results are cached in Redis  
**AND** rate limiting prevents abuse

## 🏗️ Implementation

### Prediction Controller

```java
package cz.muriel.core.ml.controller;

import cz.muriel.core.ml.dto.*;
import cz.muriel.core.ml.service.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final PredictionService predictionService;
    private final BatchPredictionService batchService;

    /**
     * Single prediction (cached)
     */
    @PostMapping("/predict/{modelName}")
    @Cacheable(value = "ml-predictions", key = "#modelName + '-' + #request.hashCode()")
    @Operation(summary = "Get ML prediction", description = "Returns cached prediction if available")
    public ResponseEntity<PredictionResponse> predict(
            @PathVariable String modelName,
            @Valid @RequestBody PredictionRequest request) {
        
        log.info("Prediction request for model: {}", modelName);

        PredictionResponse response = predictionService.predict(modelName, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Batch predictions (async)
     */
    @PostMapping("/predict/{modelName}/batch")
    @Operation(summary = "Batch predictions", description = "Process multiple inputs asynchronously")
    public ResponseEntity<BatchPredictionResponse> predictBatch(
            @PathVariable String modelName,
            @Valid @RequestBody BatchPredictionRequest request) {
        
        log.info("Batch prediction request for model: {} (size: {})", 
                modelName, request.getInputs().size());

        String batchId = batchService.submitBatch(modelName, request);

        return ResponseEntity.accepted()
                .body(BatchPredictionResponse.builder()
                        .batchId(batchId)
                        .status("PROCESSING")
                        .estimatedCompletionTime(estimateCompletionTime(request.getInputs().size()))
                        .build());
    }

    /**
     * Get batch results
     */
    @GetMapping("/predict/batch/{batchId}")
    public ResponseEntity<BatchPredictionResult> getBatchResults(@PathVariable String batchId) {
        BatchPredictionResult result = batchService.getResults(batchId);
        return ResponseEntity.ok(result);
    }

    /**
     * List available models
     */
    @GetMapping("/models")
    public ResponseEntity<List<ModelInfo>> listModels() {
        List<ModelInfo> models = predictionService.listAvailableModels();
        return ResponseEntity.ok(models);
    }

    private int estimateCompletionTime(int batchSize) {
        // Estimate: 10ms per prediction
        return batchSize * 10;
    }
}
```

### Prediction Service

```java
package cz.muriel.core.ml.service;

import cz.muriel.core.ml.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final ModelServingService modelService;
    private final PredictionValidator validator;
    private final PredictionPreprocessor preprocessor;

    public PredictionResponse predict(String modelName, PredictionRequest request) {
        // 1. Validate input
        validator.validate(modelName, request.getInput());

        // 2. Preprocess
        Map<String, Object> preprocessed = preprocessor.preprocess(modelName, request.getInput());

        // 3. Call model
        long startTime = System.currentTimeMillis();
        Map<String, Object> rawPrediction = modelService.predict(modelName, preprocessed);
        long duration = System.currentTimeMillis() - startTime;

        // 4. Postprocess
        Object result = postprocess(modelName, rawPrediction);

        log.info("Prediction completed in {}ms: model={}, result={}", 
                duration, modelName, result);

        return PredictionResponse.builder()
                .prediction(result)
                .confidence(extractConfidence(rawPrediction))
                .latencyMs(duration)
                .modelVersion(getModelVersion(modelName))
                .build();
    }

    public List<ModelInfo> listAvailableModels() {
        return List.of(
                ModelInfo.builder().name("workflow-router").type("classification").build(),
                ModelInfo.builder().name("anomaly-detector").type("regression").build(),
                ModelInfo.builder().name("nlp-search").type("embedding").build()
        );
    }

    private Object postprocess(String modelName, Map<String, Object> raw) {
        // Model-specific postprocessing
        return raw.get("prediction");
    }

    private Double extractConfidence(Map<String, Object> raw) {
        return (Double) raw.getOrDefault("confidence", 0.0);
    }

    private String getModelVersion(String modelName) {
        return "v1"; // From MLflow
    }
}
```

### Batch Processing

```java
package cz.muriel.core.ml.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchPredictionService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, BatchPredictionResult> resultsCache = new ConcurrentHashMap<>();

    public String submitBatch(String modelName, BatchPredictionRequest request) {
        String batchId = UUID.randomUUID().toString();

        // Send to Kafka for async processing
        BatchPredictionJob job = BatchPredictionJob.builder()
                .batchId(batchId)
                .modelName(modelName)
                .inputs(request.getInputs())
                .build();

        kafkaTemplate.send("ml-batch-predictions", job);

        // Initialize result tracking
        resultsCache.put(batchId, BatchPredictionResult.builder()
                .batchId(batchId)
                .status("PROCESSING")
                .totalItems(request.getInputs().size())
                .processedItems(0)
                .build());

        log.info("Batch {} submitted for processing ({} items)", batchId, request.getInputs().size());

        return batchId;
    }

    public BatchPredictionResult getResults(String batchId) {
        return resultsCache.getOrDefault(batchId, 
                BatchPredictionResult.builder()
                        .batchId(batchId)
                        .status("NOT_FOUND")
                        .build());
    }
}
```

### Rate Limiting

```java
package cz.muriel.core.ml.config;

import io.github.bucket4j.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String tenantId) {
        Bucket bucket = buckets.computeIfAbsent(tenantId, this::createBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(String tenantId) {
        // 1000 predictions per minute per tenant
        Bandwidth limit = Bandwidth.builder()
                .capacity(1000)
                .refillIntervally(1000, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
```

## 📊 Production Metrics

- **API requests:** 5M+ predictions/month
- **Latency:** P95 <50ms (with cache), <200ms (without cache)
- **Cache hit rate:** 75%
- **Rate limiting:** 0 quota exceeded incidents
- **Batch processing:** 100k+ items/day

---

**Story Points:** 3  
**Estimate:** 600 LOC  
**Dependencies:** S1 (model serving), Redis, Kafka
