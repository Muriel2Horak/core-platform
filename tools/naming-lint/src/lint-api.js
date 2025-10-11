#!/usr/bin/env node
/**
 * Lint Spring REST controllers for naming conventions
 */

import { glob } from 'glob';
import { readFileSync } from 'fs';
import { resolve, basename } from 'path';
import { isKebabCase, isPlural, pluralize, pascalToKebab } from './utils/casing.js';
import { Reporter } from './utils/reporter.js';

const REPO_ROOT = resolve(process.cwd(), '../..');
const CONTROLLER_GLOB = 'backend/src/main/java/**/api/**/*Controller.java';

async function lintApi() {
  const reporter = new Reporter('REST API Naming Lint');

  const files = await glob(CONTROLLER_GLOB, { cwd: REPO_ROOT });

  if (files.length === 0) {
    console.log('No controller files found');
    return true;
  }

  for (const file of files) {
    reporter.incrementChecked();
    const filePath = resolve(REPO_ROOT, file);
    const fileName = basename(file, '.java');

    try {
      const content = readFileSync(filePath, 'utf-8');

      // Extract @RequestMapping value
      const requestMappingMatch = content.match(/@RequestMapping\s*\(\s*["']([^"']+)["']/);
      if (!requestMappingMatch) {
        reporter.warn(file, 'No @RequestMapping annotation found');
        continue;
      }

      const path = requestMappingMatch[1];

      // 1. Check path is kebab-case
      const pathSegments = path.split('/').filter(s => s && !s.startsWith('{'));
      for (const segment of pathSegments) {
        if (segment === 'api') continue; // Skip 'api' prefix

        if (!isKebabCase(segment)) {
          reporter.error(file, `Path segment "${segment}" in "${path}" must be kebab-case`);
        }
      }

      // 2. Check resource is plural (last segment)
      const resourceSegment = pathSegments[pathSegments.length - 1];
      if (resourceSegment && resourceSegment !== 'api') {
        if (!isPlural(resourceSegment.split('-').pop())) {
          reporter.error(file, `Resource "${resourceSegment}" must be plural (e.g., users, user-directories)`);
        }
      }

      // 3. Check controller name matches path
      // E.g., /api/users → UsersController, /api/user-directories → UserDirectoriesController
      if (fileName.endsWith('Controller')) {
        const entityName = fileName.replace('Controller', '');
        const expectedPath = pascalToKebab(entityName);

        if (resourceSegment && resourceSegment !== expectedPath) {
          reporter.warn(
            file,
            `Controller "${fileName}" maps to "${path}", expected path to contain "${expectedPath}"`
          );
        }
      }

    } catch (err) {
      reporter.error(file, `Failed to parse file: ${err.message}`);
    }
  }

  const success = reporter.print();
  process.exit(success ? 0 : 1);
}

lintApi().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
