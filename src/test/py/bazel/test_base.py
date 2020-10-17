# pylint: disable=g-bad-file-header
# pylint: disable=superfluous-parens
# Copyright 2017 The Bazel Authors. All rights reserved.
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

import locale
import os
import shutil
import socket
import stat
import subprocess
import sys
import tempfile
import unittest


class Error(Exception):
  """Base class for errors in this module."""
  pass


class ArgumentError(Error):
  """A function received a bad argument."""
  pass


class EnvVarUndefinedError(Error):
  """An expected environment variable is not defined."""

  def __init__(self, name):
    Error.__init__(self, 'Environment variable "%s" is not defined' % name)


class TestBase(unittest.TestCase):

  _runfiles = None
  _temp = None
  _tests_root = None
  _test_cwd = None
  _worker_stdout = None
  _worker_stderr = None
  _worker_proc = None
  _cas_path = None

  _SHARED_REPOS = (
      'rules_cc',
      'rules_java',
      'rules_proto',
      'remotejdk11_linux_for_testing',
      'remotejdk11_linux_aarch64_for_testing',
      'remotejdk11_linux_ppc64le_for_testing',
      'remotejdk11_linux_s390x_for_testing',
      'remotejdk11_macos_for_testing',
      'remotejdk11_win_for_testing',
      'remotejdk14_linux_for_testing',
      'remotejdk14_macos_for_testing',
      'remotejdk14_win_for_testing',
      'remotejdk15_linux_for_testing',
      'remotejdk15_macos_for_testing',
      'remotejdk15_win_for_testing',
      'remote_java_tools_darwin_for_testing',
      'remote_java_tools_linux_for_testing',
      'remote_java_tools_windows_for_testing',
      'remote_coverage_tools_for_testing',
  )

  def setUp(self):
    unittest.TestCase.setUp(self)
    if self._runfiles is None:
      self._runfiles = TestBase._LoadRunfiles()
    test_tmpdir = TestBase._CreateDirs(TestBase.GetEnv('TEST_TMPDIR'))
    self._tests_root = TestBase._CreateDirs(
        os.path.join(test_tmpdir, 'tests_root'))
    self._temp = TestBase._CreateDirs(os.path.join(test_tmpdir, 'tmp'))
    self._test_cwd = tempfile.mkdtemp(dir=self._tests_root)
    self._test_bazelrc = os.path.join(self._temp, 'test_bazelrc')
    with open(self._test_bazelrc, 'wt') as f:
      shared_repo_home = os.environ.get('TEST_REPOSITORY_HOME')
      if shared_repo_home and os.path.exists(shared_repo_home):
        for repo in self._SHARED_REPOS:
          f.write('common --override_repository={}={}\n'.format(
              repo.replace('_for_testing', ''),
              os.path.join(shared_repo_home, repo).replace('\\', '/')))
      shared_install_base = os.environ.get('TEST_INSTALL_BASE')
      if shared_install_base:
        f.write('startup --install_base={}\n'.format(shared_install_base))
      shared_repo_cache = os.environ.get('REPOSITORY_CACHE')
      if shared_repo_cache:
        f.write('common --repository_cache={}\n'.format(shared_repo_cache))
        f.write('common --experimental_repository_cache_hardlinks\n')
    os.chdir(self._test_cwd)

  def tearDown(self):
    self.RunBazel(['shutdown'])
    super(TestBase, self).tearDown()

  def _AssertExitCodeIs(self,
                        actual_exit_code,
                        exit_code_pred,
                        expectation_msg,
                        stderr_lines,
                        stdout_lines=None):
    """Assert that `actual_exit_code` == `expected_exit_code`."""
    if not exit_code_pred(actual_exit_code):
      # If stdout was provided, include it in the output. This is mostly useful
      # for tests.
      stdout = ''
      if stdout_lines:
        stdout = '\n'.join([
            '(start stdout)----------------------------------------',
        ] + stdout_lines + [
            '(end stdout)------------------------------------------',
        ])

      self.fail('\n'.join([
          'Bazel exited with %d %s, stderr:' %
          (actual_exit_code, expectation_msg),
          stdout,
          '(start stderr)----------------------------------------',
      ] + (stderr_lines or []) + [
          '(end stderr)------------------------------------------',
      ]))

  def AssertExitCode(self,
                     actual_exit_code,
                     expected_exit_code,
                     stderr_lines,
                     stdout_lines=None):
    """Assert that `actual_exit_code` == `expected_exit_code`."""
    self._AssertExitCodeIs(actual_exit_code, lambda x: x == expected_exit_code,
                           '(expected %d)' % expected_exit_code, stderr_lines,
                           stdout_lines)

  def AssertNotExitCode(self,
                        actual_exit_code,
                        not_expected_exit_code,
                        stderr_lines,
                        stdout_lines=None):
    """Assert that `actual_exit_code` != `not_expected_exit_code`."""
    self._AssertExitCodeIs(
        actual_exit_code, lambda x: x != not_expected_exit_code,
        '(against expectations)', stderr_lines, stdout_lines)

  def AssertFileContentContains(self, file_path, entry):
    with open(file_path, 'r') as f:
      if entry not in f.read():
        self.fail('File "%s" does not contain "%s"' % (file_path, entry))

  def AssertFileContentNotContains(self, file_path, entry):
    with open(file_path, 'r') as f:
      if entry in f.read():
        self.fail('File "%s" does contain "%s"' % (file_path, entry))

  def CreateWorkspaceWithDefaultRepos(self, path, lines=None):
    rule_definition = [
        'load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")'
    ]
    rule_definition.extend(self.GetDefaultRepoRules())
    self.ScratchFile(path, rule_definition + (lines if lines else []))

  def GetDefaultRepoRules(self):
    return self.GetCcRulesRepoRule()

  def GetCcRulesRepoRule(self):
    sha256 = '1d4dbbd1e1e9b57d40bb0ade51c9e882da7658d5bfbf22bbd15b68e7879d761f'
    strip_pfx = 'rules_cc-8bd6cd75d03c01bb82561a96d9c1f9f7157b13d0'
    url1 = ('https://mirror.bazel.build/github.com/bazelbuild/rules_cc/'
            'archive/8bd6cd75d03c01bb82561a96d9c1f9f7157b13d0.zip')
    url2 = ('https://github.com/bazelbuild/rules_cc/'
            'archive/8bd6cd75d03c01bb82561a96d9c1f9f7157b13d0.zip')
    return [
        'http_archive(',
        '    name = "rules_cc",',
        '    sha256 = "%s",' % sha256,
        '    strip_prefix = "%s",' % strip_pfx,
        '    urls = ["%s", "%s"],' % (url1, url2),
        ')',
    ]

  @staticmethod
  def GetEnv(name, default=None):
    """Returns environment variable `name`.

    Args:
      name: string; name of the environment variable
      default: anything; return this value if the envvar is not defined
    Returns:
      string, the envvar's value if defined, or `default` if the envvar is not
      defined but `default` is
    Raises:
      EnvVarUndefinedError: if `name` is not a defined envvar and `default` is
        None
    """
    value = os.getenv(name, '__undefined_envvar__')
    if value == '__undefined_envvar__':
      if default is not None:
        return default
      raise EnvVarUndefinedError(name)
    return value

  @staticmethod
  def IsWindows():
    """Returns true if the current platform is Windows."""
    return os.name == 'nt'

  @staticmethod
  def IsUnix():
    """Returns true if the current platform is Unix (Linux and Mac included)."""
    return os.name == 'posix'

  @staticmethod
  def IsLinux():
    """Returns true if the current platform is Linux."""
    return sys.platform.startswith('linux')

  def Path(self, path):
    """Returns the absolute path of `path` relative to self._test_cwd.

    Args:
      path: string; a path, relative to self._test_cwd,
        self._test_cwd is different for each test case.
        e.g. "foo/bar/BUILD"
    Returns:
      an absolute path
    Raises:
      ArgumentError: if `path` is absolute or contains uplevel references
    """
    if os.path.isabs(path) or '..' in path:
      raise ArgumentError(('path="%s" may not be absolute and may not contain '
                           'uplevel references') % path)
    return os.path.join(self._test_cwd, path)

  def Rlocation(self, runfile):
    """Returns the absolute path to a runfile."""
    if TestBase.IsWindows():
      return self._runfiles.get(runfile)
    else:
      return os.path.join(self._runfiles, runfile)

  def ScratchDir(self, path):
    """Creates directories under the test's scratch directory.

    Args:
      path: string; a path, relative to the test's scratch directory,
        e.g. "foo/bar"
    Raises:
      ArgumentError: if `path` is absolute or contains uplevel references
      IOError: if an I/O error occurs
    Returns:
      The absolute path of the directory created.
    """
    if not path:
      return None
    abspath = self.Path(path)
    if os.path.exists(abspath):
      if os.path.isdir(abspath):
        return abspath
      raise IOError('"%s" (%s) exists and is not a directory' % (path, abspath))
    os.makedirs(abspath)
    return abspath

  def ScratchFile(self, path, lines=None, executable=False):
    """Creates a file under the test's scratch directory.

    Args:
      path: string; a path, relative to the test's scratch directory,
        e.g. "foo/bar/BUILD"
      lines: [string]; the contents of the file (newlines are added
        automatically)
      executable: bool; whether to make the file executable
    Returns:
      The absolute path of the scratch file.
    Raises:
      ArgumentError: if `path` is absolute or contains uplevel references
      IOError: if an I/O error occurs
    """
    if not path:
      return
    abspath = self.Path(path)
    if os.path.exists(abspath) and not os.path.isfile(abspath):
      raise IOError('"%s" (%s) exists and is not a file' % (path, abspath))
    self.ScratchDir(os.path.dirname(path))
    with open(abspath, 'w') as f:
      if lines:
        for l in lines:
          f.write(l)
          f.write('\n')
    if executable:
      os.chmod(abspath, stat.S_IRWXU)
    return abspath

  def CopyFile(self, src_path, dst_path, executable=False):
    """Copy a file to a path under the test's scratch directory.

    Args:
      src_path: string; a path, the file to copy
      dst_path: string; a path, relative to the test's scratch directory, the
        destination to copy the file to, e.g. "foo/bar/BUILD"
      executable: bool; whether to make the destination file executable
    Returns:
      The absolute path of the destination file.
    Raises:
      ArgumentError: if `dst_path` is absolute or contains uplevel references
      IOError: if an I/O error occurs
    """
    if not src_path or not dst_path:
      return
    abspath = self.Path(dst_path)
    if os.path.exists(abspath) and not os.path.isfile(abspath):
      raise IOError('"%s" (%s) exists and is not a file' % (dst_path, abspath))
    self.ScratchDir(os.path.dirname(dst_path))
    with open(src_path, 'rb') as s:
      with open(abspath, 'wb') as d:
        d.write(s.read())
    if executable:
      os.chmod(abspath, stat.S_IRWXU)
    return abspath

  def RunBazel(self, args, env_remove=None, env_add=None, cwd=None):
    """Runs "bazel <args>", waits for it to exit.

    Args:
      args: [string]; flags to pass to bazel (e.g. ['--batch', 'build', '//x'])
      env_remove: iterable(string); optional; environment variables to NOT pass
        to Bazel
      env_add: {string: string}; optional; environment variables to pass to
        Bazel, won't be removed by env_remove.
      cwd: [string]; the working directory of Bazel, will be self._test_cwd if
        not specified.
    Returns:
      (int, [string], [string]) tuple: exit code, stdout lines, stderr lines
    """
    return self.RunProgram([
        self.Rlocation('io_bazel/src/bazel'),
        '--bazelrc=' + self._test_bazelrc,
        '--nomaster_bazelrc',
    ] + args, env_remove, env_add, False, cwd)

  def StartRemoteWorker(self):
    """Runs a "local remote worker" to run remote builds and tests on.

    Returns:
      int: port that the local remote worker runs on.
    """
    self._worker_stdout = tempfile.TemporaryFile(dir=self._test_cwd)
    self._worker_stderr = tempfile.TemporaryFile(dir=self._test_cwd)
    if TestBase.IsWindows():
      # Ideally we would use something under TEST_TMPDIR here, but the
      # worker path must be as short as possible so we don't exceed Windows
      # path length limits, so we run straight in TEMP. This should ideally
      # be set to something like C:\temp. On CI this is set to D:\temp.
      worker_path = TestBase.GetEnv('TEMP')
      worker_exe = self.Rlocation('io_bazel/src/tools/remote/worker.exe')
    else:
      worker_path = tempfile.mkdtemp(dir=self._tests_root)
      worker_exe = self.Rlocation('io_bazel/src/tools/remote/worker')
    self._cas_path = os.path.join(worker_path, 'cas')
    os.mkdir(self._cas_path)

    # Get an open port. Unfortunately this seems to be the best option in
    # Python.
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('', 0))
    port = s.getsockname()[1]
    s.close()

    env_add = {}
    try:
      env_add['RUNFILES_MANIFEST_FILE'] = TestBase.GetEnv(
          'RUNFILES_MANIFEST_FILE')
    except EnvVarUndefinedError:
      pass

    # Tip: To help debug remote build problems, add the --debug flag below.
    self._worker_proc = subprocess.Popen(
        [
            worker_exe,
            '--singlejar',
            '--listen_port=' + str(port),
            # This path has to be extremely short to avoid Windows path
            # length restrictions.
            '--work_path=' + worker_path,
            '--cas_path=' + self._cas_path,
        ],
        stdout=self._worker_stdout,
        stderr=self._worker_stderr,
        cwd=self._test_cwd,
        env=self._EnvMap(env_add=env_add))

    return port

  def StopRemoteWorker(self):
    """Stop the "local remote worker" started by StartRemoteWorker.

    Prints its stdout and stderr out for debug purposes.
    """
    self._worker_proc.terminate()
    self._worker_proc.wait()

    self._worker_stdout.seek(0)
    stdout_lines = [
        l.decode(locale.getpreferredencoding()).strip()
        for l in self._worker_stdout.readlines()
    ]
    if stdout_lines:
      print('Local remote worker stdout')
      print('--------------------------')
      print('\n'.join(stdout_lines))

    self._worker_stderr.seek(0)
    stderr_lines = [
        l.decode(locale.getpreferredencoding()).strip()
        for l in self._worker_stderr.readlines()
    ]
    if stderr_lines:
      print('Local remote worker stderr')
      print('--------------------------')
      print('\n'.join(stderr_lines))

    shutil.rmtree(self._cas_path)

  def RunProgram(self,
                 args,
                 env_remove=None,
                 env_add=None,
                 shell=False,
                 cwd=None):
    """Runs a program (args[0]), waits for it to exit.

    Args:
      args: [string]; the args to run; args[0] should be the program itself
      env_remove: iterable(string); optional; environment variables to NOT pass
        to the program
      env_add: {string: string}; optional; environment variables to pass to
        the program, won't be removed by env_remove.
      shell: {bool: bool}; optional; whether to use the shell as the program
        to execute
      cwd: [string]; the current working dirctory, will be self._test_cwd if not
        specified.
    Returns:
      (int, [string], [string]) tuple: exit code, stdout lines, stderr lines
    """
    with tempfile.TemporaryFile(dir=self._test_cwd) as stdout:
      with tempfile.TemporaryFile(dir=self._test_cwd) as stderr:
        proc = subprocess.Popen(
            args,
            stdout=stdout,
            stderr=stderr,
            cwd=(cwd if cwd else self._test_cwd),
            env=self._EnvMap(env_remove, env_add),
            shell=shell)
        exit_code = proc.wait()

        stdout.seek(0)
        stdout_lines = [
            l.decode(locale.getpreferredencoding()).strip()
            for l in stdout.readlines()
        ]

        stderr.seek(0)
        stderr_lines = [
            l.decode(locale.getpreferredencoding()).strip()
            for l in stderr.readlines()
        ]

        return exit_code, stdout_lines, stderr_lines

  def _EnvMap(self, env_remove=None, env_add=None):
    """Returns the environment variable map to run Bazel or other programs."""
    if TestBase.IsWindows():
      env = {
          'SYSTEMROOT':
              TestBase.GetEnv('SYSTEMROOT'),
          # TODO(laszlocsomor): Let Bazel pass BAZEL_SH to tests and use that
          # here instead of hardcoding paths.
          #
          # You can override this with
          # --action_env=BAZEL_SH=C:\path\to\my\bash.exe.
          'BAZEL_SH':
              TestBase.GetEnv('BAZEL_SH',
                              'c:\\tools\\msys64\\usr\\bin\\bash.exe'),
      }
      for k in [
          'JAVA_HOME', 'BAZEL_VC', 'BAZEL_VS', 'BAZEL_VC_FULL_VERSION',
          'BAZEL_WINSDK_FULL_VERSION'
      ]:
        v = TestBase.GetEnv(k, '')
        if v:
          env[k] = v
    else:
      env = {'HOME': os.path.join(self._temp, 'home')}

    env['PATH'] = TestBase.GetEnv('PATH')
    # The inner Bazel must know that it's running as part of a test (so that it
    # uses --max_idle_secs=15 by default instead of 3 hours, etc.), and it knows
    # that by checking for TEST_TMPDIR.
    env['TEST_TMPDIR'] = TestBase.GetEnv('TEST_TMPDIR')
    env['TMP'] = self._temp
    if env_remove:
      for e in env_remove:
        if e in env:
          del env[e]
    if env_add:
      for e in env_add:
        env[e] = env_add[e]
    return env

  @staticmethod
  def _LoadRunfiles():
    """Loads the runfiles manifest from ${TEST_SRCDIR}/MANIFEST.

    Only necessary to use on Windows, where runfiles are not symlinked in to the
    runfiles directory, but are written to a MANIFEST file instead.

    Returns:
      on Windows: {string: string} dictionary, keys are runfiles-relative paths,
        values are absolute paths that the runfiles entry is mapped to;
      on other platforms: string; value of $TEST_SRCDIR
    """
    test_srcdir = TestBase.GetEnv('TEST_SRCDIR')
    if not TestBase.IsWindows():
      return test_srcdir

    result = {}
    with open(os.path.join(test_srcdir, 'MANIFEST'), 'r') as f:
      for l in f:
        tokens = l.strip().split(' ')
        if len(tokens) == 2:
          result[tokens[0]] = tokens[1]
    return result

  @staticmethod
  def _CreateDirs(path):
    if not os.path.exists(path):
      os.makedirs(path)
    elif not os.path.isdir(path):
      os.remove(path)
      os.makedirs(path)
    return path
