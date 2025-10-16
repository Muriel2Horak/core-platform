import { defineConfig, devices } from '@playwright/test';
import { readE2EConfig } from './config/read-config.js';

/**
 * Playwright E2E Configuration
 * 
 * Testy běží proti existujícímu prod-like lokálnímu prostředí.
 * Žádná orchestrace služeb - všechno už musí běžet na https://core-platform.local
 * 
 * Override baseURL: E2E_BASE_URL=https://custom.local pnpm test:e2e
 * Disable TLS check: E2E_IGNORE_TLS=true pnpm test:e2e
 */

const config = readE2EConfig();

export default defineConfig({
  testDir: './specs',
  
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
    ['html', { 
      outputFolder: 'playwright-report',
      open: 'never' // 🔧 FIX: Don't auto-open browser with report
    }],
    ['json', { outputFile: 'playwright-report/results.json' }],
    ['list'],
  ],
  
  // Shared settings
  use: {
    // Base URL z konfigurace
    baseURL: config.baseUrl,
    
    // TLS validation control
    ignoreHTTPSErrors: config.ignoreTLS,
    
    // Headless mode (no browser window)
    headless: true,
    
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
    // PRE-DEPLOY: Fast smoke tests against local dev environment
    {
      name: 'pre',
      testDir: './specs/pre',
      use: { 
        ...devices['Desktop Chrome'],
        baseURL: process.env.PRE_BASE_URL || config.baseUrl,
      },
    },
    
    // POST-DEPLOY: Full E2E tests against deployed environment
    {
      name: 'post',
      testDir: './specs/post',
      use: { 
        ...devices['Desktop Chrome'],
        baseURL: process.env.POST_BASE_URL || config.baseUrl,
      },
    },
  ],
  
  // Output folder
  outputDir: 'test-results/',
  
  // Web server - NIC NESTARTUJEME, prostředí už musí běžet
  // webServer: undefined,
});
