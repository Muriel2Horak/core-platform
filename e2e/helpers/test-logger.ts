/**
 * 🎨 Test Progress Logger
 * 
 * Beautiful UX helpers for test execution progress with emoji icons and structured output.
 * Use these functions to make your tests more readable and user-friendly.
 * 
 * @example
 * ```typescript
 * import { TestLogger } from '../../helpers/test-logger';
 * 
 * test('my test', async () => {
 *   TestLogger.testStart('My Test', 1, 10);
 *   TestLogger.step('Step 1: Doing something...');
 *   TestLogger.success('Action completed');
 *   TestLogger.testEnd();
 * });
 * ```
 */

const SEPARATOR = '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━';
const DOUBLE_SEPARATOR = '═══════════════════════════════════════════════════';

export class TestLogger {
  /**
   * Log test suite start
   */
  static suiteStart(name: string): void {
    console.log('\n');
    console.log(DOUBLE_SEPARATOR);
    console.log(`🚀  ${name} - STARTING`);
    console.log(DOUBLE_SEPARATOR);
    console.log('');
  }

  /**
   * Log test suite end
   */
  static suiteEnd(name?: string): void {
    console.log('\n');
    console.log(DOUBLE_SEPARATOR);
    console.log(`🏁  ${name || 'TEST SUITE'} - COMPLETE`);
    console.log(DOUBLE_SEPARATOR);
    console.log('');
  }

  /**
   * Log individual test start
   */
  static testStart(name: string, current?: number, total?: number): void {
    const counter = current && total ? `${current}/${total}` : '';
    console.log(`\n📝 TEST ${counter}: ${name}`);
    console.log(SEPARATOR);
    console.log('');
  }

  /**
   * Log test end with result
   */
  static testEnd(passed: boolean = true): void {
    if (passed) {
      console.log('\n✅ TEST PASSED - All assertions successful!\n');
    } else {
      console.log('\n❌ TEST FAILED - See errors above\n');
    }
  }

  /**
   * Log a step in the test
   */
  static step(message: string, stepNum?: number): void {
    const prefix = stepNum ? `Step ${stepNum}:` : '';
    console.log(`\n🔧 ${prefix} ${message}`);
  }

  /**
   * Log successful action
   */
  static success(message: string, indent: boolean = true): void {
    const prefix = indent ? '   ✓ ' : '✓ ';
    console.log(`${prefix}${message}`);
  }

  /**
   * Log info message
   */
  static info(message: string, indent: boolean = true): void {
    const prefix = indent ? '   ℹ️  ' : 'ℹ️  ';
    console.log(`${prefix}${message}`);
  }

  /**
   * Log warning
   */
  static warn(message: string, indent: boolean = true): void {
    const prefix = indent ? '   ⚠️  ' : '⚠️  ';
    console.log(`${prefix}${message}`);
  }

  /**
   * Log error
   */
  static error(message: string, indent: boolean = true): void {
    const prefix = indent ? '   ❌ ' : '❌ ';
    console.log(`${prefix}${message}`);
  }

  /**
   * Log action being performed
   */
  static action(message: string): void {
    console.log(`\n🚀 ${message}`);
  }

  /**
   * Log verification being performed
   */
  static verify(message: string): void {
    console.log(`\n🧪 ${message}`);
  }

  /**
   * Log search/query operation
   */
  static search(message: string): void {
    console.log(`\n🔍 ${message}`);
  }

  /**
   * Log cleanup operation
   */
  static cleanup(message: string): void {
    console.log(`\n🧹 ${message}`);
  }

  /**
   * Log setup operation
   */
  static setup(message: string): void {
    console.log(`\n🔧 ${message}`);
  }

  /**
   * Log data/result
   */
  static data(label: string, value: any): void {
    console.log(`   📊 ${label}: ${JSON.stringify(value, null, 2)}`);
  }

  /**
   * Log tree structure (for hierarchical data)
   */
  static tree(items: Array<{ label: string; value: any; isLast?: boolean }>): void {
    items.forEach((item, index) => {
      const isLast = item.isLast ?? index === items.length - 1;
      const prefix = isLast ? '   └─ ' : '   ├─ ';
      console.log(`${prefix}${item.label}: ${item.value}`);
    });
  }

  /**
   * Log progress dots (for waiting)
   */
  static async progressDots(durationMs: number, dotIntervalMs: number = 500): Promise<void> {
    const iterations = Math.floor(durationMs / dotIntervalMs);
    for (let i = 0; i < iterations; i++) {
      process.stdout.write('.');
      await new Promise(resolve => setTimeout(resolve, dotIntervalMs));
    }
    console.log(' Done!');
  }

  /**
   * Log separator
   */
  static separator(): void {
    console.log(SEPARATOR);
  }

  /**
   * Log double separator
   */
  static doubleSeparator(): void {
    console.log(DOUBLE_SEPARATOR);
  }
}

// Convenience export for direct destructuring
export const {
  suiteStart,
  suiteEnd,
  testStart,
  testEnd,
  step,
  success,
  info,
  warn,
  error,
  action,
  verify,
  search,
  cleanup,
  setup,
  data,
  tree,
  progressDots,
  separator,
  doubleSeparator,
} = TestLogger;
