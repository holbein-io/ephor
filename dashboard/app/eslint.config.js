import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';

export default tseslint.config(
  {
    files: ['src/**/*.ts', 'src/**/*.tsx'],
    ignores: ['**/*.config.ts', 'dist/**', 'node_modules/**'],
    extends: [
      ...tseslint.configs.recommended,
    ],
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      '@typescript-eslint/no-unused-vars': 'warn',
      '@typescript-eslint/no-explicit-any': 'off', // Temporarily disable for existing code
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-inferrable-types': 'off',
      '@typescript-eslint/no-unsafe-function-type': 'warn',
      '@typescript-eslint/no-require-imports': 'off',
      'prefer-const': 'warn',
      // React specific rules
      'react-hooks/exhaustive-deps': 'warn',
      'react-hooks/rules-of-hooks': 'error',
      'react-refresh/only-export-components': 'warn',
      // Type sizes come from the scale in index.css, not from pixel literals.
      'no-restricted-syntax': [
        'error',
        {
          selector: 'Literal[value=/text-\\[[0-9.]+px\\]/]',
          message: 'Use a type scale step (text-micro, text-caption, text-xs, text-sm, text-base, text-lg, text-xl, text-2xl, text-3xl, text-4xl) instead of a pixel literal.'
        },
        {
          selector: 'TemplateElement[value.raw=/text-\\[[0-9.]+px\\]/]',
          message: 'Use a type scale step (text-micro, text-caption, text-xs, text-sm, text-base, text-lg, text-xl, text-2xl, text-3xl, text-4xl) instead of a pixel literal.'
        }
      ]
    },
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true
        }
      }
    }
  },
  {
    files: ['**/__tests__/**/*.ts', '**/__tests__/**/*.tsx'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off'
    }
  }
);