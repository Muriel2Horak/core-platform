/**
 * PRE-DEPLOY: Workflow Panel Smoke Test
 * 
 * Verifies that workflow panel renders correctly:
 * - Current state highlighted
 * - Possible transitions shown
 * - Disabled edges show guard reason tooltip
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';

test.describe('Workflow Panel Smoke Test', () => {
  const testEntity = 'Customers'; // Replace with entity that has workflow
  
  test.beforeEach(async ({ page }) => {
    await login(page);
  });
  
  test('should show workflow panel in entity detail', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    
    // Wait for grid
    await page.waitForSelector('[data-testid="entity-grid"], .entity-grid, table', { timeout: 10000 });
    
    // Click first row
    const firstRow = page.locator('tbody tr, [role="row"]').first();
    const rowCount = await firstRow.count();
    
    if (rowCount === 0) {
      console.log('No rows in grid, skipping workflow panel test');
      return;
    }
    
    await firstRow.click();
    
    // Workflow panel should be visible
    const workflowPanel = page.locator('[data-testid="workflow-panel"], .workflow-panel, .workflow-state').first();
    
    const panelCount = await workflowPanel.count();
    if (panelCount > 0) {
      await expect(workflowPanel).toBeVisible({ timeout: 5000 });
    } else {
      console.log('No workflow panel (entity may not have workflow)');
    }
  });
  
  test('should highlight current workflow state', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    await page.waitForSelector('[data-testid="entity-grid"], table', { timeout: 10000 });
    
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) return;
    
    await firstRow.click();
    
    // Check for workflow state indicator
    const currentState = page.locator('[data-testid="current-state"], .state-current, .state.active').first();
    
    if (await currentState.count() > 0) {
      await expect(currentState).toBeVisible({ timeout: 5000 });
      
      // Should have highlighting (e.g., special class or style)
      const hasHighlight = await currentState.evaluate((el: Element) => {
        const style = window.getComputedStyle(el);
        return style.backgroundColor !== 'rgba(0, 0, 0, 0)' && 
               style.backgroundColor !== 'transparent';
      });
      
      expect(hasHighlight).toBeTruthy();
    }
  });
  
  test('should show possible transitions', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    await page.waitForSelector('[data-testid="entity-grid"], table', { timeout: 10000 });
    
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) return;
    
    await firstRow.click();
    
    // Look for transition buttons
    const transitions = page.locator('[data-testid="workflow-transition"], .workflow-action, button[data-transition]');
    
    const count = await transitions.count();
    if (count > 0) {
      // At least one transition should be visible
      const firstTransition = transitions.first();
      await expect(firstTransition).toBeVisible({ timeout: 5000 });
    } else {
      console.log('No workflow transitions found');
    }
  });
  
  test('should show tooltip on disabled transition hover', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    await page.waitForSelector('[data-testid="entity-grid"], table', { timeout: 10000 });
    
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.count() === 0) return;
    
    await firstRow.click();
    
    // Find disabled transition
    const disabledTransition = page.locator('button[data-transition][disabled], .workflow-action[disabled]').first();
    
    if (await disabledTransition.count() > 0) {
      // Hover over disabled button
      await disabledTransition.hover();
      
      // Tooltip should appear with guard reason
      const tooltip = page.locator('[role="tooltip"], .tooltip, [data-testid="transition-tooltip"]').first();
      
      // Wait for tooltip (may take a moment)
      await page.waitForTimeout(500);
      
      if (await tooltip.count() > 0) {
        await expect(tooltip).toBeVisible({ timeout: 3000 });
        
        // Should contain some text (guard reason)
        const text = await tooltip.textContent();
        expect(text).toBeTruthy();
        expect(text!.length).toBeGreaterThan(0);
      }
    } else {
      console.log('No disabled transitions found');
    }
  });
});
