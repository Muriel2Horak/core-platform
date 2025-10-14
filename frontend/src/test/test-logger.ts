/**
 * 🎨 Test Progress Logger for Vitest
 * 
 * Beautiful UX helpers for Vitest unit test execution with emoji icons and structured output.
 * Use these functions to make your frontend unit tests more readable and user-friendly.
 * 
 * @example
 * ```typescript
 * import { TestLogger } from './test-logger';
 * 
 * describe('My Component', () => {
 *   beforeAll(() => TestLogger.suiteStart('MY COMPONENT TESTS'));
 *   afterAll(() => TestLogger.suiteEnd('MY COMPONENT TESTS'));
 *   
 *   it('renders correctly', () => {
 *     TestLogger.testStart('Renders Correctly', 1, 5);
 *     TestLogger.step('Rendering component...');
 *     // ... render logic ...
 *     TestLogger.success('Component rendered');
 *     TestLogger.testEnd();
 *   });
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
   * Log render operation
   */
  static render(message: string): void {
    console.log(`\n🎨 ${message}`);
  }

  /**
   * Log verification being performed
   */
  static verify(message: string): void {
    console.log(`\n🧪 ${message}`);
  }

  /**
   * Log mock setup
   */
  static mock(message: string): void {
    console.log(`\n🎭 ${message}`);
  }

  /**
   * Log user interaction
   */
  static interact(message: string): void {
    console.log(`\n👆 ${message}`);
  }

  /**
   * Log wait/async operation
   */
  static wait(message: string): void {
    console.log(`\n⏳ ${message}`);
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
  render,
  verify,
  mock,
  interact,
  wait,
  cleanup,
  setup,
  data,
  tree,
  separator,
  doubleSeparator,
} = TestLogger;
