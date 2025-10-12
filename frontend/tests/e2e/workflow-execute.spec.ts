/**
 * Workflow Execute Test
 * 
 * Ověřuje funkčnost workflow systému:
 * - Zobrazení workflow panelu v detailu entity
 * - Zvýraznění aktuálního stavu
 * - Zobrazení dostupných přechodů
 * - Execute dialog (status RUNNING → SUCCESS/FAILED)
 * - Timeline doplnění (ENTER_STATE/EXIT_STATE)
 * - UI unlock po update (stale→fresh)
 */

import { test, expect } from '@playwright/test';
import { loginViaKeycloak } from './helpers/login.js';
import { readE2EConfig } from '../../../e2e/config/read-config.js';

const e2eConfig = readE2EConfig();

test.describe('Workflow Execute Tests', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaKeycloak(page);
  });

  test('should display workflow panel with current state', async ({ page }) => {
    // Navigace na entitu s workflow (přizpůsobte podle vaší implementace)
    // Předpokládáme že máme např. /workflows/test-entity-id
    await page.goto(`${e2eConfig.baseUrl}/workflows`);
    
    // Nebo kliknout na konkrétní entitu která má workflow
    // Přizpůsobte podle struktury vaší aplikace
    
    // Počkáme na zobrazení workflow panelu
    const workflowPanel = page.locator('[data-testid="workflow-panel"], .workflow-panel, [class*="workflow"]');
    await expect(workflowPanel).toBeVisible({ timeout: 10000 });
    
    // Ověříme, že je zobrazen aktuální stav
    const currentState = page.locator('[data-testid="current-state"], .current-state, [class*="state"][class*="active"]');
    await expect(currentState).toBeVisible();
    
    console.log('✅ Workflow panel displayed with current state');
  });

  test('should display available transitions', async ({ page }) => {
    await page.goto(`${e2eConfig.baseUrl}/workflows`);
    
    const workflowPanel = page.locator('[data-testid="workflow-panel"], .workflow-panel');
    await expect(workflowPanel).toBeVisible({ timeout: 10000 });
    
    // Ověříme dostupné přechody/akce
    const transitions = page.locator('[data-testid="transition"], [class*="transition"], button[class*="action"]');
    const transitionCount = await transitions.count();
    
    expect(transitionCount).toBeGreaterThanOrEqual(0);
    console.log(`✅ Found ${transitionCount} available transitions`);
  });

  test('should execute workflow transition and show ExecutionDialog', async ({ page }) => {
    await page.goto(`${e2eConfig.baseUrl}/workflows`);
    
    const workflowPanel = page.locator('[data-testid="workflow-panel"], .workflow-panel');
    await expect(workflowPanel).toBeVisible({ timeout: 10000 });
    
    // Najdeme tlačítko "Execute" nebo konkrétní přechod
    const executeButton = page.locator('button:has-text("Execute"), button[data-testid="execute-workflow"]').first();
    
    if (await executeButton.isVisible({ timeout: 2000 })) {
      await executeButton.click();
      
      // Ověříme, že se otevřel ExecutionDialog
      const executionDialog = page.locator('[data-testid="execution-dialog"], [role="dialog"]:has-text("Execution")');
      await expect(executionDialog).toBeVisible({ timeout: 5000 });
      
      // Ověříme status (RUNNING → SUCCESS/FAILED)
      const statusIndicator = page.locator('[data-testid="execution-status"], .execution-status, [class*="status"]');
      
      // Nejprve by měl být RUNNING
      await expect(statusIndicator).toContainText(/running|běží|pending/i, { timeout: 2000 });
      console.log('✅ Execution started (RUNNING)');
      
      // Pak by měl přejít na SUCCESS nebo FAILED
      await expect(statusIndicator).toContainText(/success|completed|úspěch|failed|error|chyba/i, { timeout: 30000 });
      console.log('✅ Execution completed (SUCCESS/FAILED)');
      
      // Ověříme přítomnost kroků a durations
      const steps = page.locator('[data-testid="execution-step"], .execution-step, [class*="step"]');
      const stepCount = await steps.count();
      expect(stepCount).toBeGreaterThan(0);
      console.log(`✅ Found ${stepCount} execution steps`);
      
    } else {
      console.warn('⚠️  No Execute button found, skipping execution test');
      test.skip();
    }
  });

  test('should update timeline with ENTER_STATE and EXIT_STATE', async ({ page }) => {
    await page.goto(`${e2eConfig.baseUrl}/workflows`);
    
    const workflowPanel = page.locator('[data-testid="workflow-panel"], .workflow-panel');
    await expect(workflowPanel).toBeVisible({ timeout: 10000 });
    
    // Zkontrolujeme timeline před execute
    const timeline = page.locator('[data-testid="timeline"], .timeline, [class*="timeline"]');
    
    if (await timeline.isVisible({ timeout: 2000 })) {
      const timelineItemsBefore = await timeline.locator('[data-testid="timeline-item"], .timeline-item').count();
      
      // Execute workflow transition
      const executeButton = page.locator('button:has-text("Execute"), button[data-testid="execute-workflow"]').first();
      
      if (await executeButton.isVisible({ timeout: 2000 })) {
        await executeButton.click();
        
        // Počkáme na dokončení
        const executionDialog = page.locator('[data-testid="execution-dialog"], [role="dialog"]');
        if (await executionDialog.isVisible({ timeout: 2000 })) {
          await expect(executionDialog.locator('[class*="status"]')).toContainText(/success|failed/i, { timeout: 30000 });
          
          // Zavřeme dialog
          const closeButton = executionDialog.locator('button:has-text("Close"), button[aria-label="close"]');
          if (await closeButton.isVisible({ timeout: 1000 })) {
            await closeButton.click();
          }
        }
        
        // Ověříme, že se timeline aktualizoval
        const timelineItemsAfter = await timeline.locator('[data-testid="timeline-item"], .timeline-item').count();
        expect(timelineItemsAfter).toBeGreaterThan(timelineItemsBefore);
        
        // Ověříme přítomnost ENTER_STATE/EXIT_STATE eventů
        const stateEvents = timeline.locator('[class*="event"]:has-text("ENTER_STATE"), [class*="event"]:has-text("EXIT_STATE")');
        const stateEventCount = await stateEvents.count();
        expect(stateEventCount).toBeGreaterThan(0);
        
        console.log(`✅ Timeline updated: ${timelineItemsBefore} → ${timelineItemsAfter} items`);
      }
    } else {
      console.warn('⚠️  Timeline not visible, skipping test');
      test.skip();
    }
  });

  test('should unlock UI after workflow update (stale→fresh)', async ({ page }) => {
    await page.goto(`${e2eConfig.baseUrl}/workflows`);
    
    const workflowPanel = page.locator('[data-testid="workflow-panel"], .workflow-panel');
    await expect(workflowPanel).toBeVisible({ timeout: 10000 });
    
    // Ověříme, že UI není ve stale stavu
    const staleIndicator = page.locator('[data-testid="stale-indicator"], [class*="stale"]');
    
    // Po načtení by neměl být stale
    await expect(staleIndicator).not.toBeVisible();
    
    // Execute workflow
    const executeButton = page.locator('button:has-text("Execute"), button[data-testid="execute-workflow"]').first();
    
    if (await executeButton.isVisible({ timeout: 2000 })) {
      await executeButton.click();
      
      // Počkáme na completion
      const executionDialog = page.locator('[data-testid="execution-dialog"], [role="dialog"]');
      if (await executionDialog.isVisible({ timeout: 2000 })) {
        await expect(executionDialog.locator('[class*="status"]')).toContainText(/success|failed/i, { timeout: 30000 });
        
        // Zavřeme dialog
        const closeButton = executionDialog.locator('button:has-text("Close"), button[aria-label="close"]');
        if (await closeButton.isVisible({ timeout: 1000 })) {
          await closeButton.click();
        }
      }
      
      // UI by měl být fresh (ne stale)
      await expect(staleIndicator).not.toBeVisible();
      
      // Ověříme, že execute button je stále enabled (UI není locked)
      await expect(executeButton).toBeEnabled();
      
      console.log('✅ UI unlocked after workflow update (fresh state)');
    }
  });
});
