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
    
    // Optional: přidat Firefox/Safari později
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
  ],
  
  // Output folder
  outputDir: 'test-results/',
  
  // Web server - NIC NESTARTUJEME, prostředí už musí běžet
  // webServer: undefined,
});
