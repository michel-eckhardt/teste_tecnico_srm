/**
 * Conventional Commits for the whole repository (enforced by the commit-msg hook and in CI).
 * Scopes are free-form but kebab-case, e.g. `feat(pricing): ...`, `fix(api-docs): ...`.
 */
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-case': [2, 'always', 'kebab-case'],
  },
  // Dependabot writes its own messages ("build(deps): Bump x from 1 to 2", long bodies); its
  // commits are identified by the sign-off it always adds.
  ignores: [(message) => /^Signed-off-by: dependabot\[bot\]/m.test(message)],
};
