# S9: Advanced Analytics Features

**Status:** 📋 **PLANNED** (Phase 8 - Post-MVP)  
**Priority:** P2 (Competitive Differentiation)  
**Effort:** TBD (~40-60 hodin estimate, needs detailed breakdown)  
**Dependencies:** 
- EPIC-009 (AI Integration) - pro ML models a NLP
- EPIC-010 (ML Platform) - pro model training/deployment
- EPIC-014 S3, S8, S9 - pro UI komponent

---

## 🎯 Vision

**Inteligentní analytika** která předpovídá trendy, detekuje anomálie a rozumí natural language queries.

**User Stories:**

1. **Predictive Analytics**  
   "As a CFO, I want to see predicted Q1 2025 revenue based on historical trends, so I can plan budget."

2. **Anomaly Detection**  
   "As an operations manager, I want automatic alerts when KPIs deviate >20% from baseline, so I can react quickly."

3. **Natural Language Queries**  
   "As a business analyst, I want to type 'show me top 10 customers by revenue last month' and get instant results, so I don't need SQL knowledge."

---

## 📊 Current State vs. Desired State

### Current State (EPIC-004 MVP)
```
User Flow:
1. Open Dashboard Builder
2. Manually configure:
   - Chart type (line/bar/pie)
   - Metrics (select from dropdown)
   - Dimensions (select from dropdown)
   - Time range (date picker)
3. Click "Add Widget"
4. Repeat for each widget

Limitations:
❌ No predictions - pouze historical data
❌ No anomaly detection - user musí manually notice outliers
❌ No NLP - user musí znát Cube.js query syntax
❌ No auto-insights - "Revenue dropped 30%" není highlighted
```

### Desired State (S9: Advanced Analytics)
```
Enhanced User Flow:
1. Open Dashboard + AI Assistant pane
2. Type natural language:
   "Show predicted revenue for Q1 2025"
   → AI translates to Cube.js query
   → Runs prediction model
   → Shows forecast chart with confidence intervals

3. AI proactively highlights:
   🔴 "Alert: Daily active users dropped 25% today (anomaly detected)"
   🟡 "Insight: Revenue increased 15% after marketing campaign"
   🟢 "Trend: User signups growing 5% MoM (sustained growth)"

Features:
✅ Predictive models - forecast future trends
✅ Anomaly detection - auto-alert on outliers
✅ NLP queries - plain English → SQL/Cube.js
✅ Auto-insights - surface important patterns
```

---

## 🔍 Feature Breakdown

### Feature 1: Predictive Analytics

**User Story:**
> "As a CFO, I want to forecast Q1 2025 revenue with 80% confidence interval, so I can create realistic budget."

**Current Workaround:**
- User exports historical data to Excel
- Manual trend analysis v Excel (linear regression)
- **Time:** ~2 hours per forecast
- **Accuracy:** Low (Excel linear regression není accurate)

**Proposed Solution:**

#### Architecture:
```
Frontend (Dashboard)
  ↓ Request: "Predict revenue for next 90 days"
  ↓
Backend (ReportQueryController)
  ↓ Fetch historical data (last 365 days)
  ↓
ML Platform (EPIC-010)
  ├─ Model: ARIMA time-series forecasting
  ├─ Training: Weekly re-train on latest data
  └─ Prediction: Next 90 days + confidence intervals
  ↓
Backend (format prediction results)
  ↓
Frontend (render forecast chart)
  └─ Chart shows: Historical (solid line) + Predicted (dashed line) + Confidence band (shaded area)
```

#### Example UI:
```typescript
// Forecast Widget Config
{
  "type": "forecast",
  "metric": "Revenue.total",
  "historicalDays": 365,
  "forecastDays": 90,
  "model": "arima",  // or "prophet", "lstm"
  "confidenceLevel": 0.8  // 80% confidence interval
}

// Rendered Chart:
// ────────────────────────────────────────────────
// │                            ╱╲ 
// │                          ╱    ╲  ← Predicted (90 days)
// │                        ╱        ╲ (dashed line)
// │     ╱╲    ╱╲         ╱            
// │   ╱    ╲╱    ╲     ╱  ← Historical (365 days)
// │ ╱              ╲ ╱      (solid line)
// └────────────────────────────────────────────────
//   Jan   Feb   Mar  │ Apr   May   Jun (forecast)
//                     └─ Today
// 
// Confidence band: Shaded area ±20% around prediction
```

#### **GAPS to Define:**

**Technical Gaps:**
- ❌ **ML Model Selection:** ARIMA vs. Prophet vs. LSTM? (needs data science research)
- ❌ **Training Pipeline:** Kdy se model re-trainuje? (daily? weekly? on-demand?)
- ❌ **Model Storage:** Kde se ukládají trained models? (S3? model registry?)
- ❌ **Inference API:** Backend endpoint pro predictions není definovaný

**UX Gaps:**
- ❌ **Confidence Visualization:** Jak zobrazit confidence intervals? (shaded band? error bars?)
- ❌ **Model Explainability:** User chce vědět "Why this prediction?" (SHAP values? feature importance?)
- ❌ **What-If Analysis:** User chce změnit assumptions ("What if growth rate = 10%?")

**Data Gaps:**
- ❌ **Insufficient Historical Data:** Co když tenant má pouze 30 days data? (minimum threshold?)
- ❌ **Seasonality Detection:** Jak detekovat weekly/monthly patterns? (automatic? user-defined?)
- ❌ **Missing Data Handling:** Co dělat s gaps v historických datech? (interpolation? skip?)

**Integration Gaps:**
- ❌ **EPIC-010 Dependency:** ML Platform není implementovaný (blocker)
- ❌ **Model Versioning:** Jak trackovat kdy byl model trained? (MLflow? custom?)
- ❌ **A/B Testing:** Jak porovnat accuracy různých modelů? (need metrics)

---

### Feature 2: Anomaly Detection

**User Story:**
> "As an operations manager, I want automatic alerts when daily active users drop >20% from 7-day average, so I can investigate issues immediately."

**Current Workaround:**
- User manually checks dashboard každý den
- Mental math: "Is today's number unusual?"
- **Problem:** Human error - anomálie můžou být missed

**Proposed Solution:**

#### Architecture:
```
Background Service (Cron Job)
  ├─ Every hour: Fetch latest metrics
  ├─ Calculate baseline (7-day rolling average)
  ├─ Compare current value vs. baseline
  ├─ If deviation >20%:
  │   ├─ Create alert
  │   ├─ Send notification (email, Slack, in-app)
  │   └─ Store in audit log
  └─ Update anomaly detection model (learn from data)

Frontend (Dashboard)
  ├─ Anomaly indicator on chart (red dot)
  ├─ Alert bell icon (badge with count)
  └─ Alert history panel
```

#### Example Alert:
```json
{
  "type": "ANOMALY_DETECTED",
  "metric": "Users.dailyActive",
  "timestamp": "2025-11-07T14:30:00Z",
  "currentValue": 1200,
  "expectedValue": 1600,
  "deviation": -25,  // -25% from baseline
  "severity": "HIGH",  // HIGH if >20%, MEDIUM if >10%, LOW if >5%
  "context": {
    "baseline": 1600,
    "baselineWindow": "7-day rolling average",
    "historicalComparison": "Lowest value in 30 days"
  },
  "suggestedActions": [
    "Check server logs for errors",
    "Review recent deployments",
    "Verify marketing campaigns status"
  ]
}
```

#### **GAPS to Define:**

**Algorithm Gaps:**
- ❌ **Detection Method:** Statistical (Z-score? IQR?) vs. ML (Isolation Forest? Autoencoder?)
- ❌ **Baseline Calculation:** Rolling average? Exponential smoothing? Seasonal decomposition?
- ❌ **Threshold Configuration:** User-defined per metric? Auto-tuned? Both?
- ❌ **False Positive Reduction:** Jak minimalizovat false alarms? (adaptive thresholds? confirmation window?)

**Notification Gaps:**
- ❌ **Delivery Channels:** Email only? Slack? PagerDuty? SMS? Push notifications?
- ❌ **Alert Fatigue:** Jak prevent overwhelming users? (digest notifications? snooze?)
- ❌ **Escalation Rules:** Kdo dostane alerts? (role-based? user preferences?)

**Configuration Gaps:**
- ❌ **Per-Metric Settings:** Každý metric může mít different thresholds (revenue vs. error rate)
- ❌ **Time Windows:** Anomálie v different time scales (hourly vs. daily vs. weekly)
- ❌ **Business Hours:** Alert pouze během work hours? (9-5 vs. 24/7)

**UI Gaps:**
- ❌ **Alert Management:** Jak user acknowledge/dismiss alerts?
- ❌ **Historical View:** List všech past anomalies (filterable, searchable)
- ❌ **Root Cause Analysis:** Link alert → related events (deploys, incidents, campaigns)

---

### Feature 3: Natural Language Queries (NLP)

**User Story:**
> "As a business analyst, I want to type 'show me top 10 users by revenue last month' instead of writing Cube.js JSON, so I can get insights faster."

**Current Workaround:**
- User musí znát Cube.js query syntax:
  ```json
  {
    "measures": ["Revenue.total"],
    "dimensions": ["Users.name"],
    "timeDimensions": [{
      "dimension": "Revenue.createdAt",
      "dateRange": "last month"
    }],
    "order": { "Revenue.total": "desc" },
    "limit": 10
  }
  ```
- **Problem:** Steep learning curve, syntax errors common

**Proposed Solution:**

#### Architecture:
```
Frontend (Query Input)
  User types: "top 10 customers by revenue last month"
  ↓
NLP Service (EPIC-009 AI Integration)
  ├─ Parse intent:
  │   ├─ Entity: "customers" → dimension: Users.name
  │   ├─ Metric: "revenue" → measure: Revenue.total
  │   ├─ Aggregation: "top 10" → limit: 10, order: desc
  │   └─ Time: "last month" → dateRange: "2024-10-01 to 2024-10-31"
  ├─ Generate Cube.js query (JSON)
  └─ Return query + confidence score
  ↓
Backend (Execute Query)
  ├─ Validate generated query
  ├─ Execute via Cube.js
  └─ Return results
  ↓
Frontend (Render Results)
  ├─ Show auto-generated chart (bar chart for "top 10")
  └─ Display query explanation: "Showing top 10 customers by revenue for October 2024"
```

#### Example Queries:
```
User Input                          → Generated Cube.js Query
───────────────────────────────────────────────────────────────
"top 10 users by revenue"           → { measures: [Revenue.total], dimensions: [Users.name], order: desc, limit: 10 }
"revenue trend last 6 months"       → { measures: [Revenue.total], timeDimensions: [...], granularity: month }
"compare Q3 vs Q4 revenue"          → { measures: [Revenue.total], timeDimensions: [Q3, Q4], split by quarter }
"users who spent >$1000"            → { measures: [Revenue.total], filters: [{ member: Revenue.total, operator: gt, values: [1000] }] }
"daily active users this week"      → { measures: [Users.dailyActive], timeDimensions: [this week], granularity: day }
```

#### **GAPS to Define:**

**NLP Model Gaps:**
- ❌ **Model Choice:** Pre-trained (GPT-4? BERT?) vs. Custom (fine-tuned on Cube.js queries)?
- ❌ **Training Data:** Kde získat training examples? (synthetic? crowdsourced?)
- ❌ **Accuracy Target:** Jaká je acceptable error rate? (90% correct? 95%?)
- ❌ **Ambiguity Handling:** Co dělat když query je unclear? (ask clarification? suggest options?)

**Query Validation Gaps:**
- ❌ **Confidence Threshold:** Kdy je NLP model "sure enough" o query? (>80% confidence?)
- ❌ **Fallback Mechanism:** Co když NLP fails? (show manual query builder?)
- ❌ **User Corrections:** Jak user opraví wrong interpretation? (edit generated query?)

**UX Gaps:**
- ❌ **Query Suggestions:** Auto-complete common queries? (typeahead?)
- ❌ **Query History:** Save frequently used queries? (favorites?)
- ❌ **Voice Input:** Support hlasové příkazy? (mobile use case?)

**Scope Gaps:**
- ❌ **Supported Syntax:** Které query types jsou supported? (simple aggregations only? joins? subqueries?)
- ❌ **Multi-Step Queries:** "Show revenue, then filter by top region" (2-step query)
- ❌ **Conversational Context:** "Show users. Now filter by active status." (context retention)

---

## 🎯 Proposed Task Breakdown (HIGH-LEVEL)

### **T1: Predictive Analytics** (~15-20h)
- T1.1: Research ML model selection (ARIMA vs. Prophet vs. LSTM)
- T1.2: Integrate with EPIC-010 ML Platform
- T1.3: Backend API: `/api/reporting/forecast`
- T1.4: Frontend: Forecast chart widget
- T1.5: Confidence interval visualization
- T1.6: What-if analysis UI (optional)

**Dependencies:**
- 🔴 **BLOCKER:** EPIC-010 (ML Platform) must be implemented first
- EPIC-014 S9 (Data Tables) - pro forecast data table view

---

### **T2: Anomaly Detection** (~12-15h)
- T2.1: Define anomaly detection algorithm (statistical vs. ML)
- T2.2: Background service: Cron job pro periodic checks
- T2.3: Alert storage + notification system
- T2.4: Frontend: Alert management UI
- T2.5: Integration with EPIC-003 (Monitoring) pro alerting

**Dependencies:**
- EPIC-003 (Monitoring) - pro notification channels (email, Slack)
- EPIC-014 S8 (Error States) - pro alert UI components

---

### **T3: Natural Language Queries** (~15-20h)
- T3.1: Research NLP model (GPT-4 API? fine-tuned BERT?)
- T3.2: Training data collection (synthetic query generation)
- T3.3: Intent parser: Text → Cube.js JSON
- T3.4: Query validator + confidence scoring
- T3.5: Frontend: NLP query input UI
- T3.6: Query history + favorites

**Dependencies:**
- 🔴 **BLOCKER:** EPIC-009 (AI Integration) must provide NLP infrastructure
- EPIC-014 S3 (Form Components) - pro query builder fallback

---

## 📊 Effort Estimate Summary

| Feature | Tasks | Effort | Dependencies | Risk |
|---------|-------|--------|--------------|------|
| **Predictive Analytics** | 6 tasks | ~18h | EPIC-010 (blocker) | 🔴 HIGH (ML platform dependency) |
| **Anomaly Detection** | 5 tasks | ~14h | EPIC-003 (partial) | 🟡 MEDIUM (algorithm selection) |
| **Natural Language Queries** | 6 tasks | ~18h | EPIC-009 (blocker) | 🔴 HIGH (NLP accuracy) |
| **TOTAL** | **17 tasks** | **~50h** | | |

**Note:** Effort estimates jsou PRELIMINARY - potřebují detailed breakdown po user research.

---

## ⚠️ Critical Decisions Needed BEFORE Implementation

### 1. **User Research Required**
- ❓ Které z těchto 3 features mají **highest user demand**?
- ❓ Jaká je **willingness to pay** za advanced analytics? (pricing tier?)
- ❓ Které **competitor features** jsou table stakes? (benchmark Tableau, PowerBI)

**Action:** Conduct user interviews + competitive analysis (2 weeks)

---

### 2. **ML Platform Readiness**
- ❓ Je EPIC-010 (ML Platform) **ready for integration**?
- ❓ Podporuje EPIC-010 **time-series forecasting**? (ARIMA, Prophet)
- ❓ Jaká je **model deployment latency**? (<1s prediction? <5s?)

**Action:** Technical feasibility review s EPIC-010 team (1 week)

---

### 3. **AI Integration Readiness**
- ❓ Je EPIC-009 (AI Integration) **ready for NLP**?
- ❓ Jaký je **cost per NLP query**? (GPT-4 API is expensive)
- ❓ Můžeme **fine-tune model** na Cube.js syntax? (need training data)

**Action:** Technical POC s EPIC-009 team (2 weeks)

---

### 4. **Scope Prioritization**
- ❓ Implementovat **all 3 features** v Phase 8? Nebo split do Phase 8-10?
- ❓ Které features jsou **MVP** vs. **nice-to-have**?
- ❓ Jaký je **realistic timeline**? (Q1 2025? Q2 2025?)

**Action:** Product roadmap review (1 sprint planning session)

---

## 🎓 Success Criteria (TBD - needs user research)

**Predictive Analytics:**
- [ ] Forecast accuracy: **>80%** (MAPE <20%)
- [ ] Confidence intervals: **Calibrated** (80% CI actually contains 80% of actuals)
- [ ] User adoption: **30%** of dashboards use forecast widgets
- [ ] Business impact: **Improved budget accuracy** by X%

**Anomaly Detection:**
- [ ] Detection rate: **>90%** of true anomalies caught
- [ ] False positive rate: **<5%** (minimize alert fatigue)
- [ ] Response time: **Alerts sent within 5 minutes** of anomaly
- [ ] User satisfaction: **+10 NPS** improvement

**Natural Language Queries:**
- [ ] Query accuracy: **>85%** correct Cube.js generation
- [ ] User adoption: **50%** of queries use NLP (vs. manual builder)
- [ ] Time savings: **70% reduction** in query creation time
- [ ] Supported query types: **>20** common patterns

---

## 📚 Related Documentation

**Dependencies:**
- [EPIC-009: AI Integration](../EPIC-009-ai-integration/README.md) - NLP infrastructure
- [EPIC-010: ML Platform](../EPIC-010-ml-platform/README.md) - Model training/deployment
- [EPIC-003: Monitoring](../EPIC-003-monitoring-observability-platform/README.md) - Alerting channels

**Research:**
- [ ] Competitive analysis: Tableau Forecasting, PowerBI Anomaly Detection, Looker NLP
- [ ] User interviews: Top 10 analytics pain points
- [ ] Technical POC: GPT-4 accuracy on Cube.js query generation

**Technical Specs:**
- [ ] ML model selection criteria
- [ ] NLP training data requirements
- [ ] Anomaly detection algorithm comparison

---

## ✅ Definition of Done (when ready to implement)

**Before Story Can Start:**
- [ ] User research completed (feature prioritization)
- [ ] EPIC-009 (AI) ready for NLP integration
- [ ] EPIC-010 (ML) ready for forecasting integration
- [ ] Technical POCs successful (>80% accuracy)
- [ ] Detailed task breakdowns created (T1-T17)
- [ ] Effort re-estimated based on POC learnings
- [ ] Product Owner approval

**Implementation Complete:**
- [ ] All features deployed to production
- [ ] User documentation written
- [ ] 90%+ accuracy on test queries/forecasts
- [ ] <5% false positive rate on anomaly detection
- [ ] Performance: <2s NLP query translation, <5s forecast generation

---

**Status:** 📋 **PLANNED** - Awaiting user research + dependency readiness  
**Next Actions:**
1. User research (2 weeks)
2. Technical POC (2 weeks)
3. Re-estimate effort
4. Schedule for Q2 2025 (tentative)

---

**Last Updated:** 7. listopadu 2025  
**Story Owner:** Product Team (research phase)  
**Future Owner:** ML Team (T1), Backend Team (T2), AI Team (T3)
