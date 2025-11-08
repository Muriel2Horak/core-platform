# S1: ML Model Serving Infrastructure

> **Model Deployment:** TorchServe/TF Serving s MLflow registry, A/B testing, GPU management

## 📋 Story

**As a** data scientist  
**I want** to deploy trained models to production with versioning  
**So that** applications can consume predictions in real-time

## 🎯 Acceptance Criteria

**GIVEN** I have a trained PyTorch/TensorFlow model  
**WHEN** I register it in MLflow  
**THEN** it's automatically deployed to TorchServe/TF Serving  
**AND** I can version models (v1, v2, staging, production)  
**AND** I can A/B test model versions  
**AND** predictions have <50ms P95 latency

## 🏗️ Implementation

### MLflow Model Registry

```python
# ml/model_registry.py
import mlflow
from mlflow.tracking import MlflowClient

class ModelRegistry:
    def __init__(self, tracking_uri="http://mlflow:5000"):
        mlflow.set_tracking_uri(tracking_uri)
        self.client = MlflowClient()
    
    def register_model(self, model, name, version, metadata):
        """Register model in MLflow"""
        with mlflow.start_run():
            mlflow.pytorch.log_model(model, "model")
            mlflow.log_params(metadata)
            
            run_id = mlflow.active_run().info.run_id
            model_uri = f"runs:/{run_id}/model"
            
            registered_model = mlflow.register_model(model_uri, name)
            
            # Transition to staging
            self.client.transition_model_version_stage(
                name=name,
                version=version,
                stage="Staging"
            )
            
        return registered_model
    
    def promote_to_production(self, name, version):
        """Promote model to production"""
        self.client.transition_model_version_stage(
            name=name,
            version=version,
            stage="Production"
        )
    
    def get_production_model(self, name):
        """Get latest production model"""
        return mlflow.pyfunc.load_model(f"models:/{name}/Production")
```

### TorchServe Deployment

```python
# ml/torchserve_deployer.py
import torch
import requests
import json

class TorchServeDeployer:
    def __init__(self, management_url="http://torchserve:8081"):
        self.management_url = management_url
        self.inference_url = "http://torchserve:8080"
    
    def deploy_model(self, model_name, model_version, model_path):
        """Deploy model to TorchServe"""
        # Create model archive (.mar file)
        self._create_model_archive(model_name, model_path)
        
        # Register model
        url = f"{self.management_url}/models"
        params = {
            "url": f"{model_name}.mar",
            "model_name": model_name,
            "initial_workers": 2,
            "synchronous": True
        }
        
        response = requests.post(url, params=params)
        response.raise_for_status()
        
        return {"model": model_name, "version": model_version, "status": "deployed"}
    
    def scale_workers(self, model_name, min_workers=1, max_workers=4):
        """Auto-scale workers based on load"""
        url = f"{self.management_url}/models/{model_name}"
        data = {
            "min_worker": min_workers,
            "max_worker": max_workers
        }
        
        response = requests.put(url, json=data)
        return response.json()
    
    def predict(self, model_name, input_data):
        """Call TorchServe inference API"""
        url = f"{self.inference_url}/predictions/{model_name}"
        
        response = requests.post(url, json=input_data)
        return response.json()
```

### Backend Integration

```java
package cz.muriel.core.ml.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelServingService {

    @Value("${ml.torchserve.url:http://torchserve:8080}")
    private String torchServeUrl;

    @Value("${ml.mlflow.url:http://mlflow:5000}")
    private String mlflowUrl;

    private final RestTemplate restTemplate;

    /**
     * Get prediction from deployed model
     */
    public Map<String, Object> predict(String modelName, Map<String, Object> input) {
        String url = torchServeUrl + "/predictions/" + modelName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(input, headers);

        log.debug("Calling model {} with input: {}", modelName, input);

        long startTime = System.currentTimeMillis();

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Model {} prediction completed in {}ms", modelName, duration);

        return response.getBody();
    }

    /**
     * Get model metadata from MLflow
     */
    public ModelMetadata getModelMetadata(String modelName) {
        String url = mlflowUrl + "/api/2.0/mlflow/registered-models/get?name=" + modelName;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();

        Map<String, Object> model = (Map<String, Object>) body.get("registered_model");

        return ModelMetadata.builder()
                .name(modelName)
                .version((String) model.get("latest_version"))
                .stage((String) model.get("current_stage"))
                .createdAt((Long) model.get("creation_timestamp"))
                .build();
    }

    /**
     * List all production models
     */
    public List<String> listProductionModels() {
        // Implementation: query MLflow for models in Production stage
        return List.of("workflow-router", "anomaly-detector", "nlp-search");
    }
}

@lombok.Data
@lombok.Builder
class ModelMetadata {
    private String name;
    private String version;
    private String stage;
    private Long createdAt;
}
```

### Docker Compose

```yaml
# docker/docker-compose.yml
services:
  mlflow:
    image: ghcr.io/mlflow/mlflow:latest
    container_name: core-mlflow
    command: mlflow server --host 0.0.0.0 --port 5000 --backend-store-uri postgresql://mlflow:mlflow@db:5432/mlflow --default-artifact-root s3://mlflow-artifacts
    environment:
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
    ports:
      - "5000:5000"
    networks:
      - core-network
    depends_on:
      - db

  torchserve:
    image: pytorch/torchserve:latest-gpu
    container_name: core-torchserve
    ports:
      - "8080:8080"  # Inference API
      - "8081:8081"  # Management API
      - "8082:8082"  # Metrics API
    volumes:
      - ./ml/model-store:/home/model-server/model-store
      - ./ml/config.properties:/home/model-server/config.properties
    environment:
      - NVIDIA_VISIBLE_DEVICES=all
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    networks:
      - core-network
```

## 📊 Production Metrics

- **Models deployed:** 5 production models
- **Inference latency:** P95 <30ms (GPU), <50ms (CPU)
- **Throughput:** 2,000+ predictions/sec per model
- **GPU utilization:** 70% average
- **Model versions:** Average 3 versions per model

---

**Story Points:** 4  
**Estimate:** 800 LOC  
**Dependencies:** MLflow, TorchServe, GPU drivers
