# 🐛 Grafana Dashboards Loading Issue - Complete Analysis

## 📊 Diagnostika

### ✅ Co Funguje:
- Routing: `/core-admin/monitoring` ✓
- Stránka se načte ✓  
- 5 tabů zobrazeno ✓
- Nadpis: "Monitoring & Analytics" ✓
- Žádné explicitní React errory ✓

### ❌ Co Nefunguje:
1. **Loading spinner zůstává viditelný** - scene se neinicializuje
2. **Scene container NOT found** - komponenta se neaktivuje  
3. **Console Error (4x)**: `window.grafanaBootData was not set by the time config was initialized`
4. **BFF API calls: 0** - datasource se vůbec nepokouší volat API
5. **Scene logging: ABSENT** - naše nové console.log se neobjevují

## 🔍 Root Cause

**Grafana Scenes library (`@grafana/scenes`) vyžaduje `window.grafanaBootData`** při inicializaci modulu.

### Timing Issue:
```
1. Browser loads HTML
2. @grafana/scenes module initializes (checks window.grafanaBootData) ❌ MISSING
3. React app starts
4. SystemMonitoringScene component tries to initialize 
5. EmbeddedScene fails silently (bootData was missing at step 2)
```

### Evidence:
- Error occurs 4x (každý login + každá page load)
- Chyba přichází PŘED našimi console.log v Scene initialization
- `grafanaBootData` mock v HTML nepomohl (nastaveno příliš pozdě nebo špatně)

## 💡 Možná Řešení

### Řešení 1: Přesunout mock PŘED bundle.js ✅ ZKUSILI
```html
<script>
  window.grafanaBootData = {...};
</script>
<script src="/bundle.js"></script>
```
**Status**: Implementováno, ale nefunguje - chyba přetrvává

### Řešení 2: Lazy Load Grafana Scenes ⭐ DOPORUČENO
Místo statického importu použít dynamic import:

```jsx
// SystemMonitoringScene.jsx
const initializeScene = async () => {
  // Lazy load Grafana Scenes
  const { EmbeddedScene, SceneTimeRange } = await import('@grafana/scenes');
  
  // NOW set bootData (scenes not yet initialized)
  if (!window.grafanaBootData) {
    window.grafanaBootData = {
      settings: {...},
      user: {...}
    };
  }
  
  // Now create scene
  const scene = new EmbeddedScene({...});
};
```

### Řešení 3: Wrapper komponenta s Error Boundary
```jsx
<ErrorBoundary fallback={<FallbackDashboard />}>
  <Suspense fallback={<LoadingSpinner />}>
    <SystemMonitoringScene />
  </Suspense>
</ErrorBoundary>
```

### Řešení 4: Použít Grafana iframe místo Scenes ⚠️  FALLBACK
Vrátit se k původnímu iframe přístupu (funkční, ale méně flexibilní)

## 🎯 Doporučená Akce

**PRIORITY 1**: Implement Lazy Loading (Řešení 2)
1. Convert `@grafana/scenes` imports to dynamic imports
2. Initialize `window.grafanaBootData` v async funkci PŘED vytvořením scene
3. Test

**PRIORITY 2**: Pokud lazy loading nefunguje
1. Check `@grafana/scenes` version compatibility
2. Review Grafana Scenes documentation for standalone usage
3. Consider alternative: Use Grafana iframe with BFF proxy

## ✅ Attempted Solutions

### Solution 1: Dynamic Import ❌ FAILED
- Changed to `const GrafanaScenes = await import('@grafana/scenes')`
- **Problem**: esbuild with `format: 'iife'` doesn't support code splitting
- Dynamic imports get bundled into main bundle.js anyway
- grafanaBootData error still occurs

### Solution 2: Enhanced grafanaBootData Mock ⏳ IN PROGRESS
- Added comprehensive mock in index.html BEFORE bundle.js:
  ```javascript
  window.grafanaBootData = {
    assets: { dark: '', light: '' },
    settings: { appUrl, appSubUrl, theme, buildInfo, bootData: {...} },
    user: { theme, lightTheme, isSignedIn, orgId, regionalFormat },
    navTree: []
  };
  ```
- Added verification console.log to track initialization
- **Issue Found**: Docker volume not syncing - need manual `docker cp`
- **Current Status**: Testing with updated index.html

### Solution 3: Change esbuild format to ESM 🔄 NOT YET TRIED
- Would enable true code splitting with dynamic imports
- **Risk**: Breaking change - need to update all scripts
- **Trade-off**: Better for lazy loading but more complex deployment

## 📝 Next Steps

1. [x] Implement dynamic import ✅ (doesn't work with IIFE)
2. [x] Enhanced grafanaBootData mock in HTML ✅ (deployed)
3. [ ] Test with debug test after fixing Docker volume sync
4. [ ] If still fails: Consider migrating to ESM format
5. [ ] **Alternative**: Fallback to iframe-based Grafana embedding
6. [ ] Document solution in GRAFANA_SCENES_GUIDE.md

## 🔧 Docker Volume Issue

**Problem**: `docker restart` doesn't pick up new `dist/index.html` changes

**Workaround**:
```bash
npm run build
docker cp frontend/dist/index.html core-frontend:/usr/share/nginx/html/index.html
```

**Permanent Fix Needed**: Check docker-compose volume mounts

## 🔗 Related Files

- `frontend/src/components/Grafana/SystemMonitoringScene.jsx` - Main component
- `frontend/src/services/grafanaSceneDataSource.js` - BFF proxy datasource
- `frontend/public/index.html` - Mock grafanaBootData attempt
- `e2e/specs/monitoring/dashboard-loading-debug.spec.ts` - Debug test
- `docs/frontend/GRAFANA_SCENES_GUIDE.md` - Implementation guide

## ⏱️ Timing

- Issue discovered: Oct 16, 2025 18:30
- Debug test created: Oct 16, 2025 18:35
- Root cause identified: Oct 16, 2025 19:15
- Mock attempt: Failed
- Next: Lazy loading implementation
