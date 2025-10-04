export default [
  {
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      },
      globals: {
        // React
        React: 'readonly',
        JSX: 'readonly',
        // DOM globals
        HTMLElement: 'readonly',
        Event: 'readonly',
        MouseEvent: 'readonly',
        KeyboardEvent: 'readonly',
        document: 'readonly',
        window: 'readonly',
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
        // Node.js
        process: 'readonly',
        global: 'readonly',
        Buffer: 'readonly',
        __dirname: 'readonly',
        __filename: 'readonly',
        module: 'readonly',
        require: 'readonly',
        exports: 'readonly',
      }
    },
    rules: {
      // Basic rules
      'no-unused-vars': 'warn',
      'no-undef': 'error',
      'no-useless-escape': 'error',
      // Development friendly
      'no-console': 'off',
    }
  },
  {
    ignores: ['dist/', 'node_modules/', '*.config.js']
  }
];