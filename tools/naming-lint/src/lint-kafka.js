#!/usr/bin/env node
/**
 * Lint Kafka topic names for naming conventions
 */

import { glob } from 'glob';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { isKebabCase } from './utils/casing.js';
import { Reporter } from './utils/reporter.js';

const REPO_ROOT = resolve(process.cwd(), '../..');

// Search Java/TS files for topic definitions
const SOURCE_GLOBS = [
  'backend/src/main/java/**/*.java',
  'backend/src/main/resources/**/*.{yml,yaml,properties}',
  'frontend/src/**/*.{ts,tsx}'
];

// Pattern: product.context.entity.event
const TOPIC_PATTERN = /^[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*){2,}(-retry|-dlq)?$/;

// Extract topic names from code
function extractTopics(content) {
  const topics = new Set();

  // Java: @KafkaListener(topics = "core.user.created")
  const javaMatches = content.matchAll(/@KafkaListener\s*\([^)]*topics\s*=\s*["']([^"'#]+)["']/g);
  for (const match of javaMatches) {
    topics.add(match[1]);
  }

  // Java: kafkaTemplate.send("core.user.created", ...)
  const sendMatches = content.matchAll(/\.send\s*\(\s*["']([^"'#]+)["']/g);
  for (const match of sendMatches) {
    if (match[1].includes('.')) topics.add(match[1]);
  }

  // YAML: topic: core.user.created
  const yamlMatches = content.matchAll(/topic:\s*["']?([a-z][a-z0-9.-]+)["']?/g);
  for (const match of yamlMatches) {
    if (match[1].includes('.') && !match[1].includes('$')) topics.add(match[1]);
  }

  // Properties: kafka.topic.user-created=core.user.created
  const propsMatches = content.matchAll(/kafka\.topic\.[^=]+=\s*([a-z][a-z0-9.-]+)/g);
  for (const match of propsMatches) {
    if (match[1].includes('.') && !match[1].includes('$')) topics.add(match[1]);
  }

  return Array.from(topics);
}

async function lintKafka() {
  const reporter = new Reporter('Kafka Topic Naming Lint');

  const allTopics = new Set();

  for (const globPattern of SOURCE_GLOBS) {
    const files = await glob(globPattern, { cwd: REPO_ROOT });

    for (const file of files) {
      const filePath = resolve(REPO_ROOT, file);

      try {
        const content = readFileSync(filePath, 'utf-8');
        const topics = extractTopics(content);

        topics.forEach(topic => allTopics.add(JSON.stringify({ topic, file })));
      } catch (err) {
        // Skip unreadable files
      }
    }
  }

  const topicsWithFiles = Array.from(allTopics).map(s => JSON.parse(s));

  if (topicsWithFiles.length === 0) {
    console.log('No Kafka topics found');
    return true;
  }

  for (const { topic, file } of topicsWithFiles) {
    reporter.incrementChecked();

    // 1. Check topic matches pattern: product.context.entity.event(-retry|-dlq)?
    if (!TOPIC_PATTERN.test(topic)) {
      reporter.error(
        file,
        `Topic "${topic}" must follow pattern: product.context.entity.event (kebab-case, dot-separated)`
      );
      continue;
    }

    // 2. Check each segment is kebab-case
    const segments = topic.replace(/-retry$/, '').replace(/-dlq$/, '').split('.');
    for (const segment of segments) {
      if (!isKebabCase(segment)) {
        reporter.error(file, `Topic segment "${segment}" in "${topic}" must be kebab-case`);
      }
    }

    // 3. Check minimum 3 segments (product.context.entity.event)
    if (segments.length < 3) {
      reporter.warn(file, `Topic "${topic}" should have at least 3 segments (product.context.entity)`);
    }
  }

  const success = reporter.print();
  process.exit(success ? 0 : 1);
}

lintKafka().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
