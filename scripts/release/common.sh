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

# Some common method for release scripts

# A release candidate is created from a branch named "release-%name%"
# where %name% is the name of the release. Once promoted to a release,
# A tag %name% will be created from this branch and the corresponding
# branch removed.
# The last commit of the release branch is always a commit containing
# the release notes in the commit message and updating the CHANGELOG.md.
# This last commit will be cherry-picked back in the master branch
# when the release candidate is promoted to a release.
# To follow tracks and to support how CI systems fetch the refs, we
# store two commit notes: the release name and the candidate number.

# Get the short hash of a commit
function __git_commit_hash() {
  git rev-parse "${1}"
}

# Get the subject (first line of the commit message) of a commit
function __git_commit_subject() {
  git show -s --pretty=format:%s "$@"
}

# Returns the branch name of the current git repository
function git_get_branch() {
  git symbolic-ref --short HEAD
}

# Returns the tag name of the current git repository
function git_get_tag() {
  git describe --tag
}

# Show the commit message of the ref specified in argument
function git_commit_msg() {
  git show -s --pretty=format:%B "$@"
}

# Extract the release candidate number from the git branch name
function get_release_candidate() {
  # Match rcX and return X
  git_get_branch 2>/dev/null | grep -Po "(?<=rc)([0-9]|\.)*$" || true
}

# Extract the release name from the git branch name
function get_release_name() {
  # Match branch name release-X.X.X-rcY and return X.X.X
  # or match tag name X.X.X and return X.X.X
  git_get_branch 2>/dev/null | grep -Po "(?<=release-)([0-9]|\.)*(?=rc)" || git_get_tag | grep -Po "^([0-9]|\.)*$" || true
}

# Get the list of commit hashes between two revisions
function git_log_hash() {
  local baseline="$1"
  local head="$2"
  shift 2
  git log --pretty=format:%H "${baseline}".."${head}" "$@"
}

# Extract the full release name from the branch name or tag name
function get_full_release_name() {
  local name="$(get_release_name "$@")"
  local rc="$(get_release_candidate "$@")"
  if [ -n "${rc}" ]; then
    echo "${name}rc${rc}"
  else
    echo "${name}"
  fi
}

# Returns the info from the branch of the release. It is the current branch
# but it errors out if the current branch is not a release branch. This
# method returns the tag of the release and the number of the current
# candidate in this release.
function get_release_branch() {
  local branch_name=$(git_get_branch)
  if [ -z "$(get_release_name)" ] || [ -z "$(get_release_candidate)" ]; then
    echo "Not a release branch: ${branch_name}." >&2
    exit 1
  fi
  echo "${branch_name}"
}

# fmt behaves differently on *BSD and on GNU/Linux, use fold.
function wrap_text() {
  fold -s -w $1 | sed 's/ *$//'
}

