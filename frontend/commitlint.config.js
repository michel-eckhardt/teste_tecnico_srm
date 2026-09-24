/**
 * Conventional Commits for the whole repository (enforced by the commit-msg hook and in CI).
 * Scopes are free-form but kebab-case, e.g. `feat(pricing): ...`, `fix(api-docs): ...`.
 */
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-case': [2, 'always', 'kebab-case'],
  },
};
