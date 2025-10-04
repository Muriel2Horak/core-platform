const js = require('@eslint/js');
const globals = require('globals');
const tsParser = require('@typescript-eslint/parser');
const tsPlugin = require('@typescript-eslint/eslint-plugin');

module.exports = [
  // Ignore patterns
  {
    ignores: [
      'dist/**',
      'node_modules/**', 
      '*.config.js',
      '*.config.cjs',
      'public/**',
      'coverage/**'
    ]
  },

  // JavaScript and JSX files
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      },
      globals: {
        ...globals.browser,
        ...globals.node,
        // React globals
        React: 'readonly',
        JSX: 'readonly',
        // DOM globals
        HTMLElement: 'readonly',
        Event: 'readonly',
        MouseEvent: 'readonly',
        KeyboardEvent: 'readonly',
        // Browser APIs
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        fetch: 'readonly',
        navigator: 'readonly',
        Intl: 'readonly',
        CustomEvent: 'readonly',
        URLSearchParams: 'readonly',
        alert: 'readonly',
        console: 'readonly',
        // Timing functions
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
      }
    },
    rules: {
      ...js.configs.recommended.rules,
      
      // 🚧 DEVELOPMENT MODE: Dočasně vypnuto pro prepared code
      'no-unused-vars': 'off', // Vypnuto - máme připravený kód pro budoucí fáze
      'no-undef': 'error', 
      'no-useless-escape': 'error',
      'no-console': 'off',
    }
  },

  // TypeScript files
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2022,
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      },
      globals: {
        ...globals.browser,
        ...globals.node,
        // React globals
        React: 'readonly',
        JSX: 'readonly',
        // DOM globals  
        HTMLElement: 'readonly',
        Event: 'readonly',
        MouseEvent: 'readonly',
        KeyboardEvent: 'readonly',
        Element: 'readonly',
        Document: 'readonly',
        Window: 'readonly',
        // Browser APIs
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        fetch: 'readonly',
        navigator: 'readonly',
        Intl: 'readonly',
        CustomEvent: 'readonly',
        URLSearchParams: 'readonly',
        alert: 'readonly',
        console: 'readonly',
        // Timing functions
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
      }
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
    },
    rules: {
      ...js.configs.recommended.rules,
      
      // Disable JS rules that conflict with TS
      'no-unused-vars': 'off',
      'no-undef': 'off', // TypeScript handles this
      
      // 🚧 DEVELOPMENT MODE: Relaxed TypeScript rules
      '@typescript-eslint/no-unused-vars': 'off', // Vypnuto - připravený kód
      '@typescript-eslint/no-explicit-any': 'warn', // Pouze varování
      
      // Other Rules
      'no-useless-escape': 'error',
      'no-console': 'off',
    }
  }
];