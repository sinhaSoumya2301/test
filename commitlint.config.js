// Enforces Conventional Commits (SPEC.md "Developer Workflow" > Automated Changelog & Releases) —
// required input for release-please to generate CHANGELOG.md and pick the next version.
export default {
  extends: ["@commitlint/config-conventional"],
};
