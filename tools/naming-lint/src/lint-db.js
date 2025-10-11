#!/usr/bin/env node
/**
 * Lint database migration files for naming conventions
 */

import { glob } from 'glob';
import { readFileSync } from 'fs';
import { resolve, basename } from 'path';
import { Reporter } from './utils/reporter.js';

const REPO_ROOT = resolve(process.cwd(), '../..');
const MIGRATION_GLOB = 'backend/src/main/resources/db/migration/**/*.sql';

// Flyway pattern: V{YYYYMMDDHHMM}__{description}.sql
const MIGRATION_PATTERN = /^V\d{12}__[a-z0-9_]+\.sql$/;

// Tables that don't require tenant_id (system tables)
const TENANT_ID_ALLOWLIST = [
  'flyway_schema_history',
  'schema_version',
  'system_config',
  'databasechangelog',
  'databasechangeloglock'
];

async function lintDb() {
  const reporter = new Reporter('Database Migration Naming Lint');

  const files = await glob(MIGRATION_GLOB, { cwd: REPO_ROOT });

  if (files.length === 0) {
    console.log('No migration files found');
    return true;
  }

  for (const file of files) {
    reporter.incrementChecked();
    const filePath = resolve(REPO_ROOT, file);
    const fileName = basename(file);

    try {
      // 1. Check migration file name pattern
      if (!MIGRATION_PATTERN.test(fileName)) {
        reporter.error(
          file,
          `Migration file name must match pattern: V{YYYYMMDDHHMM}__{description}.sql (e.g., V20251011100000__create_users_table.sql)`
        );
      }

      // 2. Check description is snake_case
      const descMatch = fileName.match(/^V\d{12}__([a-z0-9_]+)\.sql$/);
      if (descMatch) {
        const description = descMatch[1];
        if (!/^[a-z][a-z0-9_]*$/.test(description)) {
          reporter.error(file, `Description "${description}" must be snake_case`);
        }
      }

      // 3. Check SQL content
      const content = readFileSync(filePath, 'utf-8');

      // Extract CREATE TABLE statements
      const createTableMatches = content.matchAll(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-z_][a-z0-9_]*)/gi);
      for (const match of createTableMatches) {
        const tableName = match[1].toLowerCase();

        // Check table name is snake_case
        if (!/^[a-z][a-z0-9_]*$/.test(tableName)) {
          reporter.error(file, `Table name "${tableName}" must be snake_case`);
        }

        // Check tenant_id column exists (unless in allowlist)
        if (!TENANT_ID_ALLOWLIST.includes(tableName)) {
          const tableContentMatch = content.match(
            new RegExp(`CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?${tableName}\\s*\\(([^;]+)\\)`, 'is')
          );

          if (tableContentMatch) {
            const tableContent = tableContentMatch[1];
            if (!/tenant_id/i.test(tableContent)) {
              reporter.error(file, `Table "${tableName}" must have a tenant_id column (RLS requirement)`);
            }
          }
        }
      }

      // 4. Check column names in CREATE TABLE
      const columnMatches = content.matchAll(/^\s+([a-z_][a-z0-9_]*)\s+/gim);
      for (const match of columnMatches) {
        const columnName = match[1];
        if (!/^[a-z][a-z0-9_]*$/.test(columnName)) {
          reporter.warn(file, `Column name "${columnName}" should be snake_case`);
        }
      }

    } catch (err) {
      reporter.error(file, `Failed to read file: ${err.message}`);
    }
  }

  const success = reporter.print();
  process.exit(success ? 0 : 1);
}

lintDb().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
