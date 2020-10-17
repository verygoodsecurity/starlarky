#!/bin/bash
#
# Copyright 2018 The Bazel Authors. All rights reserved.
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

# Generate a versioned documentation tree.
#
# Run this script from a git checkout of a release tag. This script will infer
# the tag and create the documentation tree with the correct tag injected into
# the static pages, archive the tree, and copy it to Google Cloud Storage.
#
# This only needs to be done once per release. This script is non-destructive.
#
# TODO(jingwen): Automate this into the release pipeline.

set -eu

function log_info() {
  echo "[Info]" $@
}

readonly SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# Unless we're in a tag (e.g. 0.20.0), set DOC_VERSION to master.
readonly DOC_VERSION=$(git describe --tags --exact-match 2>/dev/null || echo "NOT_A_RELEASE")

if [[ $DOC_VERSION == "NOT_A_RELEASE" ]]
then
  log_info "You are currently in the branch: `git rev-parse --abbrev-ref HEAD`"
  log_info "Please run this script from a git checkout of a release tag, e.g. git checkout 0.20.0"
  log_info "See the valid list of tags with 'git tag'"
  exit 1
fi

function cleanup() {
  mv $SCRIPT_DIR/../../site/_config.yml{.bak,}
}

read -p "You're going to generate the docs for $DOC_VERSION. Continue? <y/n> " prompt
if [[ $prompt =~ [yY](es)* ]]
then
  # Modify the "version" Jekyll variable so all links in anchor tags are generated
  # with the injected version.
  sed -i.bak "s/master/$DOC_VERSION/" $SCRIPT_DIR/../../site/_config.yml
  trap cleanup EXIT

  bazel build //site:jekyll-tree --action_env=DOC_VERSION=$DOC_VERSION

  # -n: no-clobber; prevent overwriting existing archives to be non-destructive.
  # There should be no need to delete existing archives once it's uploaded. But
  # should there be such a need, please file an issue.
  #
  # -a public-read: set the default ACL for uploaded archives to public-read for Bazel to download it.
  gsutil cp -n $(cd $SCRIPT_DIR && bazel info bazel-genfiles)/site/jekyll-tree.tar gs://bazel-mirror/bazel_versioned_docs/jekyll-tree-$DOC_VERSION.tar

  log_info "Done."
  log_info "Now, please add \"$DOC_VERSION\" to the doc_versions list in <workspace>/site/_config.yml."
  log_info "Please also add \"$DOC_VERSION\" to <workspace>/scripts/docs/doc_versions.bzl."
fi
