# EPIC-010: ML Platform & Intelligent Features

> **Machine Learning Infrastructure:** Model serving, prediction APIs, training pipelines, AI-powered features

## 🎯 Epic Goal

Integrovat ML/AI capabilities do core-platform pro intelligent automation a predictions:
- Serving machine learning models (TensorFlow, PyTorch, ONNX)
- Real-time prediction APIs s low latency (<50ms)
- Automated training pipelines s hyperparameter tuning
- AI-powered workflow suggestions a anomaly detection

## 📊 Epic Scope

### In Scope ✅

- Model serving (TorchServe, TensorFlow Serving)
- Model registry (MLflow)
- Prediction APIs (REST, batch)
- Training pipelines (Airflow)
- Hyperparameter tuning (Optuna)
- AI features (suggestions, anomaly detection, NLP search)

### Out of Scope ❌

- Custom model development (data science handled separately)
- AutoML platform (fáze 2)
- Federated learning (fáze 3)

## 👥 Stakeholders

- **Data Science Team** - Deploy models
- **Business Users** - AI predictions
- **Platform Engineers** - ML infrastructure

## 📅 Timeline

- **Start:** 7. listopadu 2025
- **Target:** 21. listopadu 2025 (2 týdny)

## 🎁 Business Value

- **Workflow automation:** 30% reduction v manual decisions
- **Anomaly detection:** 95% accuracy, -60% false positives
- **Predictive maintenance:** -40% downtime
- **Cost savings:** $230,000/rok

## 📋 User Stories

1. **[S1: ML Model Serving](stories/S1.md)** - TorchServe, MLflow, GPU (4 SP, ~800 LOC)
2. **[S2: Prediction API Gateway](stories/S2.md)** - REST APIs, batch, caching (3 SP, ~600 LOC)
3. **[S3: Training Pipeline](stories/S3.md)** - Airflow, Optuna, DVC (3 SP, ~700 LOC)
4. **[S4: AI Features](stories/S4.md)** - Suggestions, anomaly detection, NLP (2 SP, ~500 LOC)

## 📈 Success Metrics

- Prediction latency: P95 <50ms
- Throughput: 1,000+ predictions/sec
- Models deployed: 10+ production
- Monthly predictions: 5M+

---

**Epic Owner:** ML Platform Team  
**Created:** 7. listopadu 2025  
**Status:** ⏳ In Progress (0/4 stories)
