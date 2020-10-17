#!/usr/bin/env python
"""
git remote add bazel https://github.com/bazelbuild/bazel
git fetch bazel --depth 1
git subtree add --prefix .tmp/bazel bazel master --squash
git fetch bazel master && git subtree pull --prefix .tmp/bazel bazel master --squash
"""
import os
import subprocess


class cd:
    """Context manager for changing the current working directory"""
    def __init__(self, new_path):
        self.new_path = os.path.expanduser(new_path)

    def __enter__(self):
        self.saved_path = os.getcwd()
        os.chdir(self.new_path)

    def __exit__(self, etype, value, traceback):
        os.chdir(self.saved_path)


os.makedirs('.tmp', exist_ok=True)


with cd('.tmp'):
    bazel_repo = "https://github.com/bazelbuild/bazel"
    if os.path.exists('bazel') and os.path.exists('bazel/.git'):
        cmds = (
            ["git", "fetch", "bazel", "master"],
            ["git", "subtree", "pull", "--prefix", ".tmp/bazel", "bazel", "master", "--squash"],
        )
    else:
        cmds = (
                ["git", "remote", "add", "bazel", bazel_repo],
                ["git", "fetch", "bazel", "--depth", "1"],
                ["git", "subtree", "add", "--prefix", ".tmp/bazel", "bazel", "master", "--squash"],
        )

    for cmd in cmds:
        subprocess.call(cmd)


subprocess.call([
    'rsync',
     '-avz',
     '--progress',
     '--exclude=BUILD',
    '.tmp/bazel/src/main/java/net/',
    'libstarlark/src/main/java/net/',
], cwd=os.getcwd())