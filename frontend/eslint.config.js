// @ts-check
import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import jsxA11y from 'eslint-plugin-jsx-a11y-x';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import { defineConfig, globalIgnores } from 'eslint/config';
import globals from 'globals';
import tseslint from 'typescript-eslint';

/**
 * Architecture boundaries (see README "Arquitetura"):
 *   app -> pages -> features -> shared
 * - shared never imports features, pages or app;
 * - a feature is consumed only through its public API (`@/features/<name>`, i.e. its index.ts);
 * - presentational components (`features/<name>/components`, `shared/ui`) receive data through props:
 *   they never fetch, so they cannot import the HTTP layer, TanStack Query or the feature hooks.
 */
const upperLayers = [
  { group: ['@/app', '@/app/*'], message: 'Somente o bootstrap (src/app) conhece a aplicação.' },
  {
    group: ['@/pages', '@/pages/*'],
    message: 'Páginas só são referenciadas pelas rotas (src/app).',
  },
];
const featureInternals = {
  group: ['@/features/*/*'],
  message: 'Importe a feature pela sua API pública: "@/features/<nome>" (index.ts).',
};
const dataAccess = [
  {
    group: [
      '@tanstack/react-query',
      'openapi-fetch',
      '@/shared/api/http',
      '@/shared/api/query-keys',
    ],
    message: 'Componentes de apresentação não buscam dados: recebem tudo por props.',
  },
  {
    group: ['../api', '../api/*', '../hooks', '../hooks/*', '../containers', '../containers/*'],
    message: 'Componentes de apresentação recebem dados por props (a ligação fica em containers/).',
  },
];

export default defineConfig([
  globalIgnores(['dist', 'coverage', 'node_modules', 'src/shared/api/schema.d.ts']),

  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.strictTypeChecked,
      tseslint.configs.stylisticTypeChecked,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
      jsxA11y.configs.recommended,
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      '@typescript-eslint/consistent-type-imports': ['error', { fixStyle: 'inline-type-imports' }],
      '@typescript-eslint/no-import-type-side-effects': 'error',
      '@typescript-eslint/restrict-template-expressions': ['error', { allowNumber: true }],
      '@typescript-eslint/no-confusing-void-expression': ['error', { ignoreArrowShorthand: true }],
      // react-hook-form's handleSubmit returns a promise-returning handler: fine for JSX attributes.
      '@typescript-eslint/no-misused-promises': [
        'error',
        { checksVoidReturn: { attributes: false } },
      ],
      '@typescript-eslint/switch-exhaustiveness-check': [
        'error',
        { considerDefaultExhaustiveForUnions: true },
      ],
      eqeqeq: ['error', 'always', { null: 'ignore' }],
      'no-console': ['error', { allow: ['warn', 'error'] }],
    },
  },

  // --- import boundaries -------------------------------------------------------------------------
  {
    files: ['src/shared/**/*.{ts,tsx}'],
    ignores: ['src/shared/ui/**'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            ...upperLayers,
            { group: ['@/features', '@/features/*'], message: 'shared não depende de features.' },
          ],
        },
      ],
    },
  },
  {
    files: ['src/shared/ui/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            ...upperLayers,
            { group: ['@/features', '@/features/*'], message: 'shared não depende de features.' },
            ...dataAccess,
          ],
        },
      ],
    },
  },
  {
    files: ['src/features/**/*.{ts,tsx}'],
    ignores: ['src/features/*/components/**'],
    rules: {
      'no-restricted-imports': ['error', { patterns: [...upperLayers, featureInternals] }],
    },
  },
  {
    files: ['src/features/*/components/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        { patterns: [...upperLayers, featureInternals, ...dataAccess] },
      ],
    },
  },
  {
    files: ['src/pages/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/app', '@/app/*'],
              message: 'Somente o bootstrap (src/app) conhece a aplicação.',
            },
            featureInternals,
          ],
        },
      ],
    },
  },

  // --- tests and tooling -------------------------------------------------------------------------
  {
    files: ['**/*.test.{ts,tsx}', 'src/test/**/*.{ts,tsx}'],
    rules: {
      'react-refresh/only-export-components': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
    },
  },
  {
    files: ['*.config.{js,cjs,ts}', 'eslint.config.js'],
    extends: [js.configs.recommended],
    languageOptions: { globals: globals.node },
  },

  prettier,
]);
