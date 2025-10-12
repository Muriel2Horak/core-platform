module.exports = {
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
    'plugin:jsx-a11y/recommended',
  ],
  parserOptions: {
    ecmaFeatures: {
      jsx: true,
    },
    ecmaVersion: 12,
    sourceType: 'module',
  },
  plugins: [
    'react',
    'react-hooks',
    'jsx-a11y',
  ],
  rules: {
    // React pravidla
    'react/react-in-jsx-scope': 'off', // Pro React 17+
    'react/prop-types': 'warn',
    
    // 🚨 ACCESSIBILITY PRAVIDLA - podle vašich principů
    
    // ARIA labely pro custom komponenty
    'jsx-a11y/aria-props': 'error',
    'jsx-a11y/aria-proptypes': 'error',
    'jsx-a11y/aria-unsupported-elements': 'error',
    'jsx-a11y/role-has-required-aria-props': 'error',
    'jsx-a11y/role-supports-aria-props': 'error',
    
    // Klávesová navigace - všechno dosažitelné tabulátorem
    'jsx-a11y/interactive-supports-focus': 'error',
    'jsx-a11y/no-noninteractive-tabindex': 'error',
    'jsx-a11y/tabindex-no-positive': 'error',
    
    // Formuláře - labels povinné
    'jsx-a11y/label-has-associated-control': 'error',
    
    // Obrázky musí mít alt text
    'jsx-a11y/alt-text': 'error',
    'jsx-a11y/img-redundant-alt': 'warn',
    
    // Interaktivní elementy
    'jsx-a11y/click-events-have-key-events': 'error',
    'jsx-a11y/no-static-element-interactions': 'error',
    
    // Headings hierarchie (H1 → H2 → H3 podle našich pravidel)
    'jsx-a11y/heading-has-content': 'error',
    
    // Design System pravidla - vlastní warningy pro lepší adopci
    'no-console': 'warn', // Console.log warningy
    
    // 🔒 ENTITY SDK ENFORCEMENT - Ban direct API calls
    'no-restricted-syntax': [
      'error',
      {
        selector: 'CallExpression[callee.object.name="axios"][callee.property.name=/^(get|post|put|patch|delete)$/] > Literal[value=/\\/api\\/entities/]',
        message: '❌ Direct axios calls to /api/entities/** are forbidden. Use useEntityView/useEntityMutation hooks instead.',
      },
      {
        selector: 'CallExpression[callee.name="fetch"] > Literal[value=/\\/api\\/entities/]',
        message: '❌ Direct fetch calls to /api/entities/** are forbidden. Use useEntityView/useEntityMutation hooks instead.',
      },
      {
        selector: 'CallExpression[callee.object.name="apiClient"][callee.property.name=/^(get|post|put|patch|delete)$/] > TemplateLiteral[quasis.0.value.cooked=/\\/api\\/entities/]',
        message: '❌ Direct apiClient calls to /api/entities/** are forbidden. Use useEntityView/useEntityMutation hooks instead (except inside SDK implementation).',
      },
    ],
  },
  settings: {
    react: {
      version: 'detect',
    },
  },
  
  // Overrides pro specifické soubory
  overrides: [
    {
      // Entity SDK implementation - allow direct API calls
      files: ['**/hooks/useEntityView.js', '**/hooks/useEntity*.js', '**/lib/entity/**/*.js'],
      rules: {
        'no-restricted-syntax': 'off'
      }
    },
    {
      // Design System komponenty - povolujeme přímé MUI importy
      files: ['**/components/DesignSystem.jsx', '**/components/DesignSystem/**/*.jsx'],
      rules: {
        'no-restricted-imports': 'off'
      }
    },
    {
      // Theme soubory - povolujeme všechny CSS vlastnosti
      files: ['**/styles/theme.js', '**/theme/*.js'],
      rules: {
        'no-restricted-syntax': 'off'
      }
    }
  ]
};