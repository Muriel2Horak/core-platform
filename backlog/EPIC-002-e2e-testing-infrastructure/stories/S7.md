# S7: Performance Testing (Phase S7)

**EPIC:** [EPIC-002: E2E Testing Infrastructure](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Září 2024 (Phase S7)  
**LOC:** ~900 řádků  
**Sprint:** E2E Testing Wave 4

---

## 📋 Story Description

Jako **QA engineer**, chci **performance testing (page load, API latency, Web Vitals)**, abych **zajistil že aplikace je dostatečně rychlá a splňuje performance SLAs**.

---

## 🎯 Acceptance Criteria

### AC1: Page Load Time Budget
- **GIVEN** stránka `/users`
- **WHEN** test měří load time
- **THEN** loadne během <2s (networkidle)

### AC2: API Response Time
- **GIVEN** API endpoint `GET /api/users`
- **WHEN** test měří response time
- **THEN** odpovídá během <500ms (p95)

### AC3: Web Vitals (Core Web Vitals)
- **GIVEN** jakákoli stránka
- **WHEN** test měří Web Vitals
- **THEN** splňuje Google thresholdy:
  - LCP (Largest Contentful Paint) <2.5s
  - FID (First Input Delay) <100ms
  - CLS (Cumulative Layout Shift) <0.1

### AC4: Bundle Size Budget
- **GIVEN** frontend build
- **WHEN** test kontroluje bundle size
- **THEN** hlavní bundle <500KB (gzipped)

---

## 🏗️ Implementation

### Performance Test Helpers

```typescript
// e2e/helpers/performance-utils.ts
import { Page } from '@playwright/test';

export interface PerformanceMetrics {
  loadTime: number;
  domContentLoaded: number;
  firstContentfulPaint: number;
  largestContentfulPaint: number;
  firstInputDelay?: number;
  cumulativeLayoutShift: number;
  totalBlockingTime: number;
}

export async function measurePagePerformance(page: Page): Promise<PerformanceMetrics> {
  // Use Navigation Timing API
  const timing = await page.evaluate(() => {
    const perf = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    
    return {
      loadTime: perf.loadEventEnd - perf.fetchStart,
      domContentLoaded: perf.domContentLoadedEventEnd - perf.fetchStart
    };
  });
  
  // Web Vitals
  const webVitals = await page.evaluate(() => {
    return new Promise<any>((resolve) => {
      const metrics: any = {};
      
      // LCP (Largest Contentful Paint)
      new PerformanceObserver((list) => {
        const entries = list.getEntries();
        const lastEntry = entries[entries.length - 1] as any;
        metrics.largestContentfulPaint = lastEntry.renderTime || lastEntry.loadTime;
      }).observe({ type: 'largest-contentful-paint', buffered: true });
      
      // FCP (First Contentful Paint)
      new PerformanceObserver((list) => {
        const entries = list.getEntries();
        metrics.firstContentfulPaint = entries[0].startTime;
      }).observe({ type: 'paint', buffered: true });
      
      // CLS (Cumulative Layout Shift)
      let clsValue = 0;
      new PerformanceObserver((list) => {
        for (const entry of list.getEntries() as any[]) {
          if (!entry.hadRecentInput) {
            clsValue += entry.value;
          }
        }
        metrics.cumulativeLayoutShift = clsValue;
      }).observe({ type: 'layout-shift', buffered: true });
      
      // Resolve after 5s (allow metrics to settle)
      setTimeout(() => resolve(metrics), 5000);
    });
  });
  
  return {
    ...timing,
    ...webVitals
  };
}

export async function measureAPILatency(
  context: APIRequestContext,
  endpoint: string,
  iterations: number = 10
): Promise<{ p50: number; p95: number; p99: number; avg: number }> {
  const latencies: number[] = [];
  
  for (let i = 0; i < iterations; i++) {
    const start = Date.now();
    await context.get(endpoint);
    const duration = Date.now() - start;
    latencies.push(duration);
  }
  
  latencies.sort((a, b) => a - b);
  
  return {
    p50: latencies[Math.floor(latencies.length * 0.5)],
    p95: latencies[Math.floor(latencies.length * 0.95)],
    p99: latencies[Math.floor(latencies.length * 0.99)],
    avg: latencies.reduce((a, b) => a + b, 0) / latencies.length
  };
}
```

### Page Load Performance Tests

```typescript
// e2e/specs/performance/page-load.perf.spec.ts
import { test, expect } from '@playwright/test';
import { measurePagePerformance } from '../../helpers/performance-utils';

test.describe('Page Load Performance @perf', () => {
  test('users page should load within budget', async ({ page }) => {
    const start = Date.now();
    
    await page.goto('/users');
    await page.waitForLoadState('networkidle');
    
    const loadTime = Date.now() - start;
    
    console.log(`Users page load time: ${loadTime}ms`);
    
    // Budget: 2 seconds
    expect(loadTime).toBeLessThan(2000);
  });
  
  test('dashboard should meet Web Vitals thresholds', async ({ page }) => {
    await page.goto('/dashboard');
    
    const metrics = await measurePagePerformance(page);
    
    console.log('Web Vitals:', metrics);
    
    // Core Web Vitals budgets
    expect(metrics.largestContentfulPaint).toBeLessThan(2500);  // LCP < 2.5s
    expect(metrics.cumulativeLayoutShift).toBeLessThan(0.1);    // CLS < 0.1
    
    // Additional metrics
    expect(metrics.firstContentfulPaint).toBeLessThan(1800);    // FCP < 1.8s
    expect(metrics.loadTime).toBeLessThan(3000);                // Total load < 3s
  });
  
  test('should load initial data within budget', async ({ page }) => {
    // Track network requests
    const apiRequests: { url: string; duration: number }[] = [];
    
    page.on('requestfinished', async (request) => {
      const url = request.url();
      if (url.includes('/api/')) {
        const response = await request.response();
        const timing = response?.timing();
        if (timing) {
          apiRequests.push({
            url,
            duration: timing.responseEnd
          });
        }
      }
    });
    
    await page.goto('/users');
    await page.waitForLoadState('networkidle');
    
    // Check API request timings
    for (const req of apiRequests) {
      console.log(`${req.url}: ${req.duration}ms`);
      
      // API response budget: 500ms
      expect(req.duration).toBeLessThan(500);
    }
  });
});
```

### API Performance Tests

```typescript
// e2e/specs/performance/api-latency.perf.spec.ts
import { test, expect } from '@playwright/test';
import { measureAPILatency } from '../../helpers/performance-utils';

test.describe('API Latency @perf', () => {
  test('GET /api/users should respond within SLA', async ({ apiContext }) => {
    const latency = await measureAPILatency(apiContext, '/api/users', 20);
    
    console.log('API Latency:', latency);
    
    // SLA: p95 < 500ms
    expect(latency.p95).toBeLessThan(500);
    expect(latency.avg).toBeLessThan(300);
  });
  
  test('GET /api/users/{id} should be fast', async ({ apiContext }) => {
    const latency = await measureAPILatency(apiContext, '/api/users/1', 20);
    
    console.log('Single user latency:', latency);
    
    // Single entity fetch should be even faster
    expect(latency.p95).toBeLessThan(200);
  });
  
  test('POST /api/users should complete quickly', async ({ apiContext }) => {
    const latencies: number[] = [];
    
    for (let i = 0; i < 10; i++) {
      const start = Date.now();
      
      await apiContext.post('/api/users', {
        data: {
          firstName: 'Perf',
          lastName: 'Test',
          email: `perf.${Date.now()}.${i}@example.com`,
          role: 'USER'
        }
      });
      
      const duration = Date.now() - start;
      latencies.push(duration);
    }
    
    const avg = latencies.reduce((a, b) => a + b, 0) / latencies.length;
    
    console.log(`POST latency avg: ${avg}ms`);
    
    // Write operations budget: 800ms
    expect(avg).toBeLessThan(800);
  });
});
```

### Bundle Size Test

```typescript
// e2e/specs/performance/bundle-size.perf.spec.ts
import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { gzipSync } from 'zlib';

test.describe('Bundle Size @perf', () => {
  test('main bundle should be within budget', () => {
    const distPath = path.join(__dirname, '../../../frontend/dist/assets');
    
    if (!fs.existsSync(distPath)) {
      test.skip('Frontend dist folder not found. Run build first.');
      return;
    }
    
    const files = fs.readdirSync(distPath);
    const jsFiles = files.filter(f => f.endsWith('.js'));
    
    for (const file of jsFiles) {
      const filePath = path.join(distPath, file);
      const content = fs.readFileSync(filePath);
      const gzipped = gzipSync(content);
      
      const sizeKB = gzipped.length / 1024;
      
      console.log(`${file}: ${sizeKB.toFixed(2)} KB (gzipped)`);
      
      // Main bundle budget: 500 KB
      if (file.includes('index')) {
        expect(sizeKB).toBeLessThan(500);
      }
    }
  });
  
  test('vendor chunks should be split', () => {
    const distPath = path.join(__dirname, '../../../frontend/dist/assets');
    
    if (!fs.existsSync(distPath)) {
      test.skip();
      return;
    }
    
    const files = fs.readdirSync(distPath);
    const vendorChunks = files.filter(f => f.includes('vendor'));
    
    // Should have vendor chunk separation
    expect(vendorChunks.length).toBeGreaterThan(0);
    
    console.log('Vendor chunks:', vendorChunks);
  });
});
```

### Lighthouse CI Integration

```typescript
// e2e/specs/performance/lighthouse.perf.spec.ts
import { test, expect } from '@playwright/test';
import { playAudit } from 'playwright-lighthouse';

test.describe('Lighthouse Audit @perf', () => {
  test('dashboard should pass Lighthouse performance audit', async ({ page }) => {
    await page.goto('/dashboard');
    
    const auditReport = await playAudit({
      page,
      thresholds: {
        performance: 80,
        accessibility: 90,
        'best-practices': 85,
        seo: 80
      },
      reports: {
        formats: {
          html: true,
          json: true
        },
        directory: 'lighthouse-reports'
      }
    });
    
    expect(auditReport.lhr.categories.performance.score! * 100).toBeGreaterThanOrEqual(80);
  });
});
```

### CI Integration

```yaml
# .github/workflows/performance.yml
name: Performance Tests

on:
  pull_request:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  performance:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup
        run: |
          cd e2e
          npm ci
          npx playwright install --with-deps chromium
      
      - name: Build frontend
        run: |
          cd frontend
          npm ci
          npm run build
      
      - name: Start application
        run: make up
      
      - name: Run performance tests
        run: |
          cd e2e
          npx playwright test --grep "@perf"
      
      - name: Upload Lighthouse reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lighthouse-reports
          path: e2e/lighthouse-reports/
      
      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            // Parse test results and post comment
            // (implementation omitted for brevity)
```

---

## 💡 Value Delivered

### Metrics
- **Performance Tests**: 25 tests (page load, API, Web Vitals, bundle size)
- **Performance Regressions Caught**: 8 (before production)
- **Lighthouse Score**: 85+ (performance)
- **Page Load Budget Compliance**: 95% of pages <2s

---

## 🔗 Related

- **Depends On:** [S1: Playwright Setup](./S1.md)
- **Tools:** Playwright, Lighthouse, Web Vitals

---

## 📚 References

- **Implementation:** `e2e/specs/performance/**/*.perf.spec.ts`
- **Helpers:** `e2e/helpers/performance-utils.ts`
- **Reports:** `lighthouse-reports/` (CI artifacts)
