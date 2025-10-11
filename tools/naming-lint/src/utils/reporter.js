import chalk from 'chalk';

/**
 * Reporter for lint results
 */
export class Reporter {
  constructor(name) {
    this.name = name;
    this.errors = [];
    this.warnings = [];
    this.checked = 0;
  }

  /**
   * Add error
   */
  error(file, message) {
    this.errors.push({ file, message, level: 'error' });
  }

  /**
   * Add warning
   */
  warn(file, message) {
    this.warnings.push({ file, message, level: 'warning' });
  }

  /**
   * Increment checked count
   */
  incrementChecked() {
    this.checked++;
  }

  /**
   * Print report
   */
  print() {
    console.log(chalk.bold.blue(`\n${this.name}`));
    console.log(chalk.gray('='.repeat(this.name.length)));

    if (this.errors.length === 0 && this.warnings.length === 0) {
      console.log(chalk.green(`✓ All ${this.checked} files passed`));
      return true;
    }

    // Print errors
    if (this.errors.length > 0) {
      console.log(chalk.red.bold(`\n✗ ${this.errors.length} error(s):`));
      this.errors.forEach(({ file, message }) => {
        console.log(chalk.red(`  ${file}`));
        console.log(chalk.gray(`    ${message}`));
      });
    }

    // Print warnings
    if (this.warnings.length > 0) {
      console.log(chalk.yellow.bold(`\n⚠ ${this.warnings.length} warning(s):`));
      this.warnings.forEach(({ file, message }) => {
        console.log(chalk.yellow(`  ${file}`));
        console.log(chalk.gray(`    ${message}`));
      });
    }

    console.log(chalk.gray(`\nChecked: ${this.checked} files`));
    return this.errors.length === 0;
  }
}
