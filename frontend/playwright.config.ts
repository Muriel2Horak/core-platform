import { defineConfig, devices } from '@playwright/test';
import { readE2EConfig } from '../e2e/config/read-config.js';

/**
 * Playwright Configuration
 * 
 * Tests run against existing prod-like local environment (https://core-platform.local)
 * 
 * Usage:
 *   npm run test:e2e              # Run all E2E tests
 *   npm run test:e2e:headed       # Run with browser UI
 *   npm run test:e2e:ui           # Open Playwright UI
 */

const config = readE2EConfig();

export default defineConfig({
  testDir: './tests/e2e',
  
  // Timeout pro jeden test
  timeout: 60 * 1000,
  
  // Timeout pro expect assertions
  expect: {
    timeout: 10 * 1000,
  },
  
  // Paralelizace testů
  fullyParallel: true,
  
  // Fail build pokud je test.only
  forbidOnly: !!process.env.CI,
  
  // Retry na CI
  retries: process.env.CI ? 2 : 0,
  
  // Workers
  workers: process.env.CI ? 1 : undefined,
  
  // Reporting
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'playwright-report/results.json' }],
    ['list'],
  ],
  
  // Shared settings
  use: {
    // Base URL z konfigurace
    baseURL: config.baseUrl,
    
    // TLS validation control
    ignoreHTTPSErrors: config.ignoreTLS,
    
    // Trace on first retry
    trace: 'on-first-retry',
    
    // Screenshot on failure
    screenshot: 'only-on-failure',
    
    // Video on failure
    video: 'retain-on-failure',
    
    // Action timeout
    actionTimeout: 15 * 1000,
    
    // Navigation timeout
    navigationTimeout: 30 * 1000,
  },
  
  // Test projects
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  
  // Output folder
  outputDir: 'test-results/',
});

