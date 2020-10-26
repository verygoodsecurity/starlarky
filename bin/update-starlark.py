#!/usr/bin/env python
"""
git remote add bazel https://github.com/bazelbuild/bazel
git fetch bazel --depth 1
git subtree add --prefix .tmp/bazel bazel master --squash
git fetch bazel master && git subtree pull --prefix .tmp/bazel bazel master --squash
"""
import os
import subprocess
import sys


class cd(object):
    """Context manager for changing the current working directory"""
    def __init__(self, new_path):
        self.new_path = os.path.expanduser(new_path)

    def __enter__(self):
        self.saved_path = os.getcwd()
        os.chdir(self.new_path)
        return self.new_path

    def __exit__(self, etype, value, traceback):
        os.chdir(self.saved_path)


os.makedirs('.tmp', exist_ok=True)


def get_submodules(root):
    """return submodules relative to root"""
    return [
        os.path.join(root, '.tmp', 'bazel'),
    ]


def is_repo(d):
    """is d a git repo?"""
    if not os.path.exists(os.path.join(d, '.git')):
        return False
    proc = subprocess.Popen('git status',
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            shell=True,
                            cwd=d,
    )
    status, _ = proc.communicate()
    return status == 0


def check_submodule_status(root=None):
    """check submodule status
    Has three return values:
    'missing' - submodules are absent
    'unclean' - submodules have unstaged changes
    'clean' - all submodules are up to date
    """

    if hasattr(sys, "frozen"):
        # frozen via py2exe or similar, don't bother
        return 'clean'

    if not root:
        root = os.getcwd()

    if not is_repo(root):
        # not in git, assume clean
        return 'clean'

    submodules = get_submodules(root)

    for submodule in submodules:
        if not os.path.exists(submodule):
            return 'missing'

    # Popen can't handle unicode cwd on Windows Python 2
    if sys.platform == 'win32' and sys.version_info[0] < 3 \
        and not isinstance(root, bytes):
        root = root.encode(sys.getfilesystemencoding() or 'ascii')
    # check with git submodule status
    proc = subprocess.Popen('git submodule status',
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            shell=True,
                            cwd=root,
    )
    status, _ = proc.communicate()
    status = status.decode("ascii", "replace")

    for line in status.splitlines():
        if line.startswith('-'):
            return 'missing'
        elif line.startswith('+'):
            return 'unclean'

    return 'clean'


def update_submodules(repo_dir):
    """update submodules in a repo"""
    subprocess.check_call("git submodule init", cwd=repo_dir, shell=True)
    subprocess.check_call("git submodule update --depth 1 --recursive", cwd=repo_dir, shell=True)


def add_submodule(url, path):
  subprocess.check_call(['git', 'submodule', 'add', '--depth', '1', '-f', '--', url, path])


def remove_submodule(path):
  subprocess.check_call(['git', 'submodule', 'deinit', path])
  subprocess.check_call(['git', 'rm', path])


bazel_repo = "https://github.com/bazelbuild/bazel"


for submodule_dir in get_submodules(os.getcwd()):
    add_submodule(bazel_repo, submodule_dir)
    update_submodules(submodule_dir)


# if os.path.exists('bazel') and os.path.exists('.tmp/bazel/.git'):
#     cmds = (
#         ["git", "fetch", "bazel", "master"],
#         ["git", "subtree", "pull", "--prefix", ".tmp/bazel", "bazel", "master", "--squash"],
#     )
# else:
#     cmds = (
#             ["git", "remote", "add", "bazel", bazel_repo],
#             ["git", "fetch", "bazel", "--depth", "1"],
#             ["git", "subtree", "add", "--prefix", ".tmp/bazel", "bazel", "master", "--squash"],
#     )
#
# for cmd in cmds:
#     subprocess.call(cmd)


for target_dir in ('src/main', 'src/test',):
    subprocess.call([
        'rsync',
        '-avz',
        '--progress',
        '--exclude=BUILD',
        f'.tmp/bazel/{target_dir}/java/net/',
        f'libstarlark/{target_dir}/java/net/',
        '--delete',
    ], cwd=os.getcwd())
