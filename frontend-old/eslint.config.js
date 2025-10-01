import js from '@eslint/js'
import globals from 'globals'
import reactRefreshPlugin from 'eslint-plugin-react-refresh'
import { FlatCompat } from '@eslint/eslintrc'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import unusedImports from 'eslint-plugin-unused-imports'
import importPlugin from 'eslint-plugin-import'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const compat = new FlatCompat({ baseDirectory: __dirname })

export default [
  {
    ignores: ['dist/', 'dist-ts/', 'build/', 'node_modules/']
  },
  // Načti doporučené konfigurace přes kompatibilní vrstvu (.eslintrc -> flat)
  ...compat.extends('plugin:react/recommended', 'plugin:react-hooks/recommended', 'plugin:import/recommended'),

  // Pravidla pro JS/JSX soubory
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        process: 'readonly',
        global: 'readonly'
      },
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    settings: {
      'import/resolver': {
        node: {
          extensions: ['.js', '.jsx', '.ts', '.tsx'],
          paths: ['src']
        }
      },
      react: { version: 'detect' },
    },
    rules: {
      ...js.configs.recommended.rules,
      'react/react-in-jsx-scope': 'off',
      'react/prop-types': 'off',
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'react/jsx-no-undef': 'error',
      'no-undef': 'error',
      'import/no-unresolved': 'error',
      'import/named': 'error',
      'import/no-named-as-default': 'warn',
      // Prefer plugin rules pro nepoužité importy/vars
      'no-unused-vars': 'off',
      'unused-imports/no-unused-imports': 'warn',
      'unused-imports/no-unused-vars': [
        'warn',
        { vars: 'all', varsIgnorePattern: '^_', args: 'after-used', argsIgnorePattern: '^_' }
      ],
    },
    plugins: {
      'react-refresh': reactRefreshPlugin,
      'unused-imports': unusedImports,
      'import': importPlugin,
    }
  },

  // Speciální pravidla pro test soubory
  {
    files: ['**/*.test.{js,jsx,ts,tsx}', '**/*.spec.{js,jsx,ts,tsx}'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.jest,
        describe: 'readonly',
        test: 'readonly',
        expect: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        beforeAll: 'readonly',
        afterAll: 'readonly'
      }
    },
    rules: {
      'import/no-unresolved': 'off' // V testech můžeme mít volnější pravidla
    }
  }
]
