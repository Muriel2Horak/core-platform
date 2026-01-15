---
id: S11
epic: EPIC-004-reporting-analytics-infrastructure
title: "Advanced Visualization"
priority: P3
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "30 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-004-reporting-analytics-infrastructure/stories/REP11-advanced-visualization/README.md
    - backlog/EPIC-004-reporting-analytics-infrastructure/README.md
---

# S11: Advanced Visualization

**Status:** 📋 **PLANNED** (Phase 10 - Post-MVP)  
**Priority:** P3 (Competitive Differentiation)  
**Effort:** TBD (~20-30 hodin estimate)  
**Dependencies:** Chart library research (D3.js? Nivo? Custom?)

---

## 🎯 Vision

**Advanced chart types** pro complex data visualization - beyond line/bar/pie charts.

**User Stories:**

1. **Custom Chart Types**  
   "As a product manager, I want Sankey diagram showing user journey (Homepage → Product → Checkout), so I can identify drop-off points."

2. **Geographic Maps**  
   "As a sales director, I want world map colored by revenue per country, so I can see geographic distribution at a glance."

3. **Animation Support**  
   "As a business analyst, I want to animate revenue chart over time (playback 2020-2024), so I can present trends to executives."

---

## 📋 Feature Breakdown (HIGH-LEVEL)

### Feature 1: Custom Chart Types

**Current Support:**
- ✅ Line chart
- ✅ Bar chart (vertical/horizontal)
- ✅ Pie/Donut chart
- ✅ Area chart
- ✅ Scatter plot

**Proposed Additions:**
- ❌ **Sankey diagram** - flow visualization (user journeys, revenue streams)
- ❌ **Treemap** - hierarchical data (product categories, org structure)
- ❌ **Heatmap** - 2D density (correlations, activity patterns)
- ❌ **Network graph** - relationships (customer networks, dependencies)
- ❌ **Funnel chart** - conversion rates (sales pipeline, user onboarding)
- ❌ **Waterfall chart** - cumulative changes (P&L breakdown, variance analysis)

**GAPS:**
- ❌ Chart library selection (Recharts limited, D3.js complex, Nivo balance?)
- ❌ Data transformation (Cube.js → chart-specific format)
- ❌ Performance (large datasets v complex charts?)

---

### Feature 2: Geographic Maps

**Proposed:**

```typescript
// Map widget config
{
  "type": "choropleth_map",  // Colored regions
  "dataSource": "Revenue.total",
  "dimension": "Users.country",
  "colorScale": {
    "type": "sequential",
    "colors": ["#E8F4F8", "#0066CC"],  // Light blue → Dark blue
    "domain": [0, 1000000]  // $0 - $1M
  },
  "mapProvider": "mapbox",  // or "leaflet", "google_maps"
  "projection": "mercator",
  "zoomLevel": 2,
  "center": [0, 20]  // lat, long
}
```

**GAPS:**
- ❌ Geocoding service (country name → coordinates)
- ❌ Map tile provider (Mapbox API key? Self-hosted?)
- ❌ Offline support (embedded maps? CDN?)

---

### Feature 3: Animation Support

**Proposed:**

```typescript
// Animated chart
{
  "type": "animated_line",
  "metric": "Revenue.total",
  "timeDimension": "Revenue.createdAt",
  "animation": {
    "duration": 5000,  // 5 seconds total
    "fps": 30,
    "playbackSpeed": 1.0,  // 1x, 2x, 0.5x
    "loop": false
  },
  "controls": {
    "play": true,
    "pause": true,
    "scrubber": true  // Timeline slider
  }
}
```

**GAPS:**
- ❌ Animation library (Framer Motion? GSAP? CSS animations?)
- ❌ Data fetching (pre-load all frames? stream?)
- ❌ Export support (animated GIF? MP4 video?)

---

## 🛠️ Proposed Task Breakdown

### T1: Custom Chart Library Integration (~8h)

- T1.1: Evaluate chart libraries (D3.js vs. Nivo vs. Recharts extensions)
- T1.2: Implement Sankey diagram
- T1.3: Implement Treemap
- T1.4: Implement Heatmap

### T2: Geographic Maps (~10h)

- T2.1: Select map provider (Mapbox vs. Leaflet)
- T2.2: Geocoding integration
- T2.3: Choropleth map component
- T2.4: Interactive tooltips + zoom

### T3: Animation Engine (~8h)

- T3.1: Animation library integration
- T3.2: Playback controls UI
- T3.3: Data pre-fetching optimization
- T3.4: Export to video (optional)

---

## ⚠️ Critical Decisions Needed

1. **User Research:** Which chart types are most requested?
2. **Competitive Analysis:** What do Tableau/PowerBI offer?
3. **Cost:** Mapbox API pricing? Self-host maps?
4. **Performance:** Can browser handle complex viz with large datasets?

---

**Status:** 📋 **PLANNED** - Needs user research + library evaluation

---

**Last Updated:** 7. listopadu 2025
