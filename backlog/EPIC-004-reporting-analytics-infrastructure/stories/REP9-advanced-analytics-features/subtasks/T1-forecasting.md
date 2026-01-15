# T1: ML Forecasting

**Story:** [S9: Advanced Analytics](README.md)  
**Effort:** 20 hours  
**Priority:** P2  
**Dependencies:** None

---

## 📋 OBJECTIVE

Time-series forecasting s Prophet/ARIMA.

---

## 🏗️ IMPLEMENTATION

```python
# analytics/forecasting.py
from prophet import Prophet

def forecast_metric(data):
    model = Prophet()
    model.fit(data)
    future = model.make_future_dataframe(periods=30)
    return model.predict(future)
```

---

## ✅ Acceptance Criteria

- [ ] Forecast endpoint vraci predikci + confidence interval.
- [ ] Model je trenovan z tenant-specific historickych dat.
- [ ] UI zobrazuje predikci jako dashed line + interval band.

---

## ✅ DELIVERABLES

- [ ] Forecasting service
- [ ] API endpoint
- [ ] Visualization

---

**Estimated:** 25 hours
