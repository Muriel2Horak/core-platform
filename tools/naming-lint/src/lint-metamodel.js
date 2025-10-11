#!/usr/bin/env node
/**
 * Lint metamodel JSON files for naming conventions
 */

import { glob } from 'glob';
import { readFileSync } from 'fs';
import { resolve, basename } from 'path';
import { isPascalCase, isCamelCase, isPlural } from './utils/casing.js';
import { Reporter } from './utils/reporter.js';

const REPO_ROOT = resolve(process.cwd(), '../..');
const METAMODEL_GLOB = 'backend/src/main/resources/metamodel/**/*.json';

const REQUIRED_FIELDS = ['id', 'tenantId', 'createdAt', 'updatedAt', 'version'];

async function lintMetamodel() {
  const reporter = new Reporter('Metamodel Naming Lint');

  const files = await glob(METAMODEL_GLOB, { cwd: REPO_ROOT });

  if (files.length === 0) {
    console.log('No metamodel files found');
    return true;
  }

  for (const file of files) {
    reporter.incrementChecked();
    const filePath = resolve(REPO_ROOT, file);
    const fileName = basename(file, '.json');

    try {
      const content = readFileSync(filePath, 'utf-8');
      const model = JSON.parse(content);

      // 1. Check file name is PascalCase singular
      if (!isPascalCase(fileName)) {
        reporter.error(file, `File name "${fileName}" must be PascalCase (e.g., User, UserDirectory)`);
      }

      if (isPlural(fileName)) {
        reporter.error(file, `File name "${fileName}" must be singular (e.g., User, not Users)`);
      }

      // 2. Check entity name matches file name
      if (model.name && model.name !== fileName) {
        reporter.error(file, `Entity name "${model.name}" must match file name "${fileName}"`);
      }

      // 3. Check entity name is PascalCase
      if (model.name && !isPascalCase(model.name)) {
        reporter.error(file, `Entity name "${model.name}" must be PascalCase`);
      }

      // 4. Check required fields exist
      if (model.fields) {
        const fieldNames = model.fields.map(f => f.name);
        for (const required of REQUIRED_FIELDS) {
          if (!fieldNames.includes(required)) {
            reporter.error(file, `Missing required field: "${required}"`);
          }
        }

        // 5. Check all field names are camelCase
        for (const field of model.fields) {
          if (!isCamelCase(field.name)) {
            reporter.error(file, `Field name "${field.name}" must be camelCase`);
          }
        }
      }

      // 6. Check tableName is snake_case plural
      if (model.tableName) {
        const tablePattern = /^[a-z][a-z0-9_]*$/;
        if (!tablePattern.test(model.tableName)) {
          reporter.error(file, `Table name "${model.tableName}" must be snake_case`);
        }

        if (!isPlural(model.tableName.split('_').pop())) {
          reporter.warn(file, `Table name "${model.tableName}" should be plural`);
        }
      }

    } catch (err) {
      reporter.error(file, `Failed to parse JSON: ${err.message}`);
    }
  }

  const success = reporter.print();
  process.exit(success ? 0 : 1);
}

lintMetamodel().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
