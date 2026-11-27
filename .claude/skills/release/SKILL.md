---
name: release
description: Release a new version of neon-util - version bump, local verification, PR, CI on all platforms, merge, GitHub release, and the JitPack check. Use when the user asks to release, publish or ship a new version.
argument-hint: "[version, e.g. 0.1.2]"
disable-model-invocation: true
---

# Release neon-util

Release version `$ARGUMENTS` of neon-util. Background and reasoning for every step are in `AGENTS.md` ("Versioning",
"Release process", "Pitfalls from past releases"). Read it first.

Run the steps in order. Stop and report to the user whenever a step fails. Never merge or release on a failed check.

## 1. Determine the version

- If `$ARGUMENTS` is empty, look at the changes since the last release (`gh release list --limit 1`,
  `git log --oneline <last-tag>..main`). Propose a version following the rules in `AGENTS.md` (breaking API change → minor
  while below 1.0, otherwise patch) and ask the user to confirm it.
- Call the version `X.Y.Z` and the tag `vX.Y.Z`. Check that the tag does not exist yet (`git ls-remote --tags origin vX.Y.Z`).

## 2. Prepare the release branch

```shell
git switch main && git pull --ff-only
git status --short                  # must be clean. Otherwise ask the user what to do with local changes.
git log --oneline origin/main..HEAD # unpushed commits on main? Ask whether they belong into this release.
git switch -c release/vX.Y.Z
```

If unpushed commits are on local `main` and should go into the release, create the branch from there. Afterwards, reset
local `main` to `origin/main` once the branch exists, so `main` is never pushed directly.

Bump the version everywhere (see "Versioning" in `AGENTS.md`):
- `build.gradle`: `version = 'X.Y.Z'`
- `README.md`: the example tag ("e.g. `vX.Y.Z`") and the JitPack command examples
- `README.md` "Upgrading" section: add a "Since X.Y.Z, …" note if this release changes behavior or API

Check for leftovers: `grep -rn "<old version>" --exclude-dir=build --exclude-dir=.git --exclude-dir=.gradle --exclude-dir=.idea .`
Old versions that appear only as history (e.g. "Version 0.1.0 renamed …") stay.

## 3. Verify locally

All of these must pass:

```shell
./gradlew clean build --warning-mode all        # no failures, no deprecation warnings
(cd build/libs && java -jar neon-util-X.Y.Z.jar | grep -c "information:")   # jar runs standalone
./gradlew clean build --rerun-tasks --configuration-cache
./gradlew clean -Pgroup=com.github.NeonMika -Pversion=vX.Y.Z -xtest assemble publishToMavenLocal \
  -Dmaven.repo.local=<scratch dir>/m2   # exactly what JitPack runs. Keep ~/.m2 clean.
```

Commit the version bump (plus any other release changes) with a descriptive message and the agent's `Co-Authored-By` line.

## 4. Push and open the PR

```shell
git push -u origin release/vX.Y.Z
gh pr create --base main --head release/vX.Y.Z --title "vX.Y.Z: <short summary>" --body "<hand-written body>"
```

PR body: what changes for users, a list of changes, tests, docs. Put a `## ⚠️ Breaking change` section first if there is one.
End with the PR attribution line from the system instructions.

## 5. Wait for CI on the exact head commit

Auto-merge is disabled and `main` has no required checks, so wait yourself:

```shell
sha=$(git rev-parse HEAD)
id=$(gh run list --branch release/vX.Y.Z --workflow test.yml --commit $sha --limit 1 --json databaseId --jq '.[0].databaseId')
# retry the lookup for a few seconds if the run has not been created yet
gh run watch $id --exit-status --interval 15
gh run view $id --json conclusion,headSha,jobs --jq '.conclusion, .headSha, (.jobs[] | "\(.name): \(.conclusion)")'
```

All three jobs (ubuntu, windows, macos) must be `success`, and `headSha` must equal `$sha`. On failure, read the log with
`gh run view $id --log-failed`, fix the problem on the branch, push, and wait again. Typical Windows pitfall: `\r\n` line
endings in tests.

## 6. Merge

```shell
gh pr merge <N> --merge --delete-branch --subject "Merge pull request #<N>: vX.Y.Z"
gh pr view <N> --json state,mergeCommit --jq '.state, .mergeCommit.oid'   # MERGED, note the merge SHA
git switch main && git pull --ff-only && git rev-parse HEAD               # must equal the merge SHA
```

## 7. Create the GitHub release

```shell
gh release create vX.Y.Z --target <merge-sha> --title "Version X.Y.Z" --notes "<notes>"
```

Release notes: a short hand-written bullet list of user-visible changes. Put breaking changes first in a
`> [!IMPORTANT]` block, and end with `Full changes: #<N>`. Look at earlier releases (`gh release view <tag>`) for the style.
Do **not** request the version from JitPack yourself (see the pitfall in `AGENTS.md`).

## 8. Watch the release workflows

Both `Test` and `Test JitPack` are triggered by the release:

```shell
gh run list --event release --commit <merge-sha> --json databaseId,workflowName
gh run watch <id> --exit-status --interval 20   # for each of the two runs
gh run view <jitpack-run-id> --log | grep -E "Testing neon-util version|Attempt [0-9]+ of|\"status\"|is available on JitPack"
```

`Test JitPack` can take up to about 10 minutes while JitPack builds the version.

If `Test JitPack` fails, read `https://jitpack.io/com/github/NeonMika/neon-util/vX.Y.Z/build.log`, then:
1. fix the problem in a new patch release (never re-tag a published version),
2. add a `> [!WARNING]` note at the top of the broken release's notes, pointing to the fix.

## 9. Report

Tell the user:
- the PR, its CI result and the merge commit
- the release URL
- the results of both release workflows (and how many JitPack attempts were needed)
- that local `main` matches `origin/main` and the release branch is deleted
- anything that went wrong along the way
