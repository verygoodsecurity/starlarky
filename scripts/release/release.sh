#!/bin/bash

# Copyright 2015 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -eu

# Generate the release branches and handle the release tags.

# Name of the default editor.
: ${EDITOR=vi}

# Repositories to push the release branch and the release tag.
RELEASE_REPOSITORY="git@github.com:bazelbuild/bazel"

# Repositories to push the master branch.
MASTER_REPOSITORY="https://bazel.googlesource.com/bazel"

# Author of the release commits.
RELEASE_AUTHOR="Bazel Release System <noreply@google.com>"

# Load relnotes.sh.
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source ${SCRIPT_DIR}/relnotes.sh

# Load common.sh.
source ${SCRIPT_DIR}/common.sh

# Editing release notes info for the user.
RELEASE_NOTE_MESSAGE='# Editing release notes
# Modify the release notes to make them suitable for the release.
# Every line starting with a # will be removed as well as every
# empty line at the start and at the end.
'

# Merge three release notes using branch $1 as a base.
# Args:
#   $1 the branch name to use to play on
#   $2 the new generated release notes
#   $3 the last generated release notes
#   $4 the last edited release notes
function __merge_release_notes() {
  local branch_name="$1"
  local relnotes="$2"
  local last_relnotes="$3"
  local last_savedrelnotes="$4"
  if [ "${last_relnotes}" == "${last_savedrelnotes}" ]; then
    echo "${relnotes}"
  else
    # Merge the three release notes using "git merge".
    git checkout -q -b "${branch_name}-merge-notes-1"
    echo "${last_relnotes}" >.relnotes
    git add .relnotes
    git commit -q -m "last_relnotes" --allow-empty

    echo "${last_savedrelnotes}" >.relnotes
    git add .relnotes
    git commit -q -m "last_savedrelnotes" --allow-empty
    git checkout -q -b "${branch_name}-merge-notes-2" HEAD~

    echo "${relnotes}" >.relnotes
    git add .relnotes
    git commit -q -m "relnotes" --allow-empty

    git merge -q --no-commit "${branch_name}-merge-notes-1" &>/dev/null || true
    cat .relnotes

    # Clean-up
    git merge --abort || true &>/dev/null
    git checkout -q "${branch_name}"
    git branch -D ${branch_name}-merge-notes-{1,2} >/dev/null
  fi
}

# Create the release commit by changing the CHANGELOG file
function __create_release_commit() {
  local infos=$(generate_release_message "${1}" HEAD '```')
  local changelog_path="$PWD/CHANGELOG.md"

  # Get the CHANGELOG.md from master to avoid missing release notes from release
  # that were in-between.
  git checkout master CHANGELOG.md

  # CHANGELOG.md
  local tmpfile="$(mktemp --tmpdir relnotes-XXXXXXXX)"
  {
    echo -n "## ${infos}"
    echo
    echo
    cat "${changelog_path}"
  } >> ${tmpfile}
  mv "${tmpfile}" "${changelog_path}"
  git add "${changelog_path}"

  # Commit
  infos="$(echo "${infos}" | grep -Ev '^```$')"
  git commit --no-verify -m "${infos}" --no-edit --author "${RELEASE_AUTHOR}"
}

function __apply_cherry_picks() {
  echo "Applying cherry-picks"
  # Apply cherry-picks
  for commit in "$@"; do
    local previous_head="$(git rev-parse HEAD)"
    echo "  Cherry-picking ${commit}"
    git cherry-pick ${commit} >/dev/null || {
      echo "Failed to cherry-pick ${commit}. please resolve the conflict and exit." >&2
      echo "  Use 'git cherry-pick --abort; exit' to abort the cherry-picks." >&2
      echo "  Use 'git cherry-pick --continue; exit' to resolve the conflict." >&2
      bash
      if [ "$(git rev-parse HEAD)" == "${previous_head}" ]; then
        echo "Cherry-pick aborted, aborting the whole command..." >&2
        return 1
      fi
    }
  done
  return 0
}

# Execute the create command:
#   Create a new release named "$1" with "$2" as the baseline commit.
function __create_release() {
  local force_rc=1
  if [[ "$1" =~ ^--force_rc=([0-9]*)$ ]]; then
    force_rc=${BASH_REMATCH[1]}
    shift 1
  fi
  local release_name="$1"
  local baseline="$2"
  shift 2
  local branch_name="release-${release_name}rc${force_rc}"

  # Fetch everything from remote repositories to avoid conflicts
  git fetch -f "${RELEASE_REPOSITORY}"

  echo "Creating new release branch ${branch_name} for release ${release_name}"
  git checkout -B ${branch_name} ${baseline}

  __apply_cherry_picks $@ || {
    git checkout master
    git branch -D ${branch_name}
    exit 1
  }

  echo "Created $(get_full_release_name) on branch ${branch_name}."
}

# Force push a ref $2 to repo $1 if exists
function __push_if_exists() {
  if git show-ref -q "${2}"; then
    git push -f "${1}" "+${2}"
  fi
}

# Push a given ref
function __push_ref() {
  local ref="$1"
  __push_if_exists "${RELEASE_REPOSITORY}" "${ref}"
}

# Push the release branch to the release repositories so a release
# candidate can be created.
function __push_release_candidate() {
  __push_ref "$(get_release_branch)"
}

# Deletes the release branch after a release or abandoning the release
function __cleanup_branches() {
  local tag_name=$1
  echo "Destroying the release branches for release ${tag_name}"
  for branch in $(git branch | grep -Po "release-${tag_name}rc([0-9])*")
  do
    echo "Deleting ${branch}"
    git branch -D "${branch}" &>/dev/null || true
    git push -f "${RELEASE_REPOSITORY}" ":${branch}" &>/dev/null || true
  done
}

# Releases the current release branch, creating the necessary tag,
# destroying the release branch, updating the master's CHANGELOG.md
# and pushing everything to GitHub.
function __do_release() {
  local branch=$(get_release_branch)
  local tag_name=$(get_release_name)
  local candidate=$(get_release_candidate)

  echo -n "You are about to release branch ${branch} in tag ${tag_name}, confirm? [y/N] "
  read answer
  if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
    echo "Creating the release commit"
    __create_release_commit "${tag_name}"

    echo "Creating the tag"
    git tag ${tag_name}

    echo "Cherry-picking CHANGELOG.md modification into master"
    git checkout master

    # Ensuring we are up to date for master
    git pull --rebase "$MASTER_REPOSITORY" master

    # We do not cherry-pick because we might have conflict if the baseline
    # does not contains the latest CHANGELOG.md file, so trick it.
    local changelog_path="$PWD/CHANGELOG.md"
    git show "${branch}:CHANGELOG.md" > "${changelog_path}"
    local tmpfile=$(mktemp --tmpdir relnotes-XXXXXXXX)
    trap 'rm -f ${tmpfile}' EXIT
    git_commit_msg "${branch}" > "${tmpfile}"
    git add "${changelog_path}"
    git commit --no-verify -F "${tmpfile}" --no-edit --author "${RELEASE_AUTHOR}"
    rm -f "${tmpfile}"
    trap - EXIT

    echo "Pushing the change to remote repositories"
    git push "${MASTER_REPOSITORY}" +master
    __push_ref "refs/tags/${tag_name}"
    __cleanup_branches "${tag_name}"
  fi
}

# Abandon the current release, deleting the branch on the local
# repository and on GitHub, discarding all changes
function __abandon_release() {
  local branch_info=$(get_release_branch)
  local tag_name=$(get_release_name)
  local candidate=$(get_release_candidate)
  echo -n "You are about to abandon release ${tag_name}, confirm? [y/N] "
  read answer
  if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
    git checkout -q master >/dev/null
    __cleanup_branches "${tag_name}"
  fi
}

function __usage() {
  cat >&2 <<EOF
Usage: $1 command [arguments]
Available commands are:
  - create [--force_rc=RC] RELEASE_NAME BASELINE [COMMIT1 ... COMMITN]:
      creates a new release branch for release named RELEASE_NAME,
      cutting it at the commit BASELINE and cherry-picking
      COMMIT1 ... COMMITN. The release candidate number will be
      computed from existing release branch unless --force_rc is
      specified.
  - push: push the current release branch to release repositories.
  - release: do the actual release of the current release branch.
  - abandon: abandon the current release branch.

The typical workflow for the release manager is:
1. Create the release branch using the decided name for the release
   (usually a version number). The BASELINE is generally a baseline
   that has been tested extensively including inside Google.
2. Push to the repository and wait for the continuous integration
   to rebuild and deploy the various artifacts and send the annoucement
   mails about a new release candidate being available.
3. If necessary, creates a new release branch with the same name to
   address return from the users and go back to 2.
4. Either abandon or release the branch depending on if too much
   problems are reported or if the release is considered viable. The
   CI system should then rebuild from the tag and deploy the artifact
   to GitHub and sends the announcement mails about the new release.
EOF
   exit 1
}

git diff-index --quiet HEAD -- || {
  echo "There are pending changes in this git repository." >&2
  echo "Please commit or stash them before using that script." >&2
  exit 1
}

[ "$(git rev-parse --show-toplevel)" == "$PWD" ] || {
  echo "You should run this script from the root of the git repository." >&2
  exit 1
}

progname=$0
cmd=${1-}
shift || __usage $progname

case $cmd in
  create)
    (( $# >= 2 )) || __usage $progname
    __create_release "$@"
    ;;
  push)
    __push_release_candidate
    ;;
  release)
    __do_release
    ;;
  abandon)
    __abandon_release
    ;;
  *)
    __usage $progname
    ;;
esac
