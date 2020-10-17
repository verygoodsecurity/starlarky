// Copyright 2019 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// daemonize [-a] -l log_path -p pid_path -- binary_path binary_name [args]
//
// daemonize spawns a program as a daemon, redirecting all of its output to the
// given log_path and writing the daemon's PID to pid_path.  binary_path
// specifies the full location of the program to execute and binary_name
// indicates its display name (aka argv[0], so the optional args do not have to
// specify it again).  log_path is created/truncated unless the -a (append) flag
// is specified.  Also note that pid_path is guaranteed to exists when this
// program terminates successfully.
//
// Some important details about the implementation of this program:
//
// * No threads to ensure the use of fork below does not cause trouble.
//
// * Pure C, no C++. This is intentional to keep the program low overhead
//   and to avoid the accidental introduction of heavy dependencies that
//   could spawn threads.
//
// * Error handling is extensive but there is no error propagation.  Given
//   that the goal of this program is just to spawn another one as a daemon,
//   we take the freedom to immediatey exit from anywhere as soon as we
//   hit an error.

#include <sys/types.h>

#include <assert.h>
#include <err.h>
#include <fcntl.h>
#include <getopt.h>
#include <inttypes.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// Configures std{in,out,err} of the current process to serve as a daemon.
//
// stdin is configured to read from /dev/null.
//
// stdout and stderr are configured to write to log_path, which is created and
// truncated unless log_append is set to true, in which case it is open for
// append if it exists.
static void SetupStdio(const char* log_path, bool log_append) {
  close(STDIN_FILENO);
  int fd = open("/dev/null", O_RDONLY);
  if (fd == -1) {
    err(EXIT_FAILURE, "Failed to open /dev/null");
  }
  assert(fd == STDIN_FILENO);

  close(STDOUT_FILENO);
  int flags = O_WRONLY | O_CREAT | (log_append ? O_APPEND : O_TRUNC);
  fd = open(log_path, flags, 0666);
  if (fd == -1) {
    err(EXIT_FAILURE, "Failed to create log file %s", log_path);
  }
  assert(fd == STDOUT_FILENO);

  close(STDERR_FILENO);
  fd = dup(STDOUT_FILENO);
  if (fd == -1) {
    err(EXIT_FAILURE, "dup failed");
  }
  assert(fd == STDERR_FILENO);
}

// Writes the given pid to a new file at pid_path.
//
// Once the pid file has been created, this notifies pid_done_fd by writing a
// dummy character to it and closing it.
static void WritePidFile(pid_t pid, const char* pid_path, int pid_done_fd) {
  FILE* pid_file = fopen(pid_path, "w");
  if (pid_file == NULL) {
    err(EXIT_FAILURE, "Failed to create %s", pid_path);
  }
  fprintf(pid_file, "%" PRIdMAX, (intmax_t) pid);
  fclose(pid_file);

  char dummy = '\0';
  write(pid_done_fd, &dummy, sizeof(dummy));
  close(pid_done_fd);
}

static void ExecAsDaemon(const char* log_path, bool log_append, int pid_done_fd,
                         const char* exe, char** argv)
    __attribute__((noreturn));

// Executes the requested binary configuring it to behave as a daemon.
//
// The stdout and stderr of the current process are redirected to the given
// log_path.  See SetupStdio for details on how this is handled.
//
// This blocks execution until pid_done_fd receives a write.  We do this
// because the Bazel server process (which is what we start with this helper
// binary) requires the PID file to be present at startup time so we must
// wait until the parent process has created it.
//
// This function never returns.
static void ExecAsDaemon(const char* log_path, bool log_append, int pid_done_fd,
                         const char* exe, char** argv) {
  char dummy;
  if (read(pid_done_fd, &dummy, sizeof(dummy)) == -1) {
    err(EXIT_FAILURE, "Failed to wait for pid file creation");
  }
  close(pid_done_fd);

  if (signal(SIGHUP, SIG_IGN) == SIG_ERR) {
    err(EXIT_FAILURE, "Failed to install SIGHUP handler");
  }

  if (setsid() == -1) {
    err(EXIT_FAILURE, "setsid failed");
  }

  SetupStdio(log_path, log_append);

  execv(exe, argv);
  err(EXIT_FAILURE, "Failed to execute %s", exe);
}

// Starts the given process as a daemon.
//
// This spawns a subprocess that will be configured to run the desired program
// as a daemon.  The program to run is supplied in exe and the arguments to it
// are given in the NULL-terminated argv.  argv[0] must be present and
// contain the program name (which may or may not match the basename of exe).
static void Daemonize(const char* log_path, bool log_append,
                      const char* pid_path, const char* exe, char** argv) {
  assert(argv[0] != NULL);

  int pid_done_fds[2];
  if (pipe(pid_done_fds) == -1) {
    err(EXIT_FAILURE, "pipe failed");
  }

  pid_t pid = fork();
  if (pid == -1) {
    err(EXIT_FAILURE, "fork failed");
  } else if (pid == 0) {
    close(pid_done_fds[1]);
    ExecAsDaemon(log_path, log_append, pid_done_fds[0], exe, argv);
    abort();  // NOLINT Unreachable.
  }
  close(pid_done_fds[0]);

  WritePidFile(pid, pid_path, pid_done_fds[1]);
}

// Program entry point.
//
// The primary responsibility of this function is to parse program options.
// Once that is done, delegates all work to Daemonize.
int main(int argc, char** argv) {
  bool log_append = false;
  const char* log_path = NULL;
  const char* pid_path = NULL;
  int opt;
  while ((opt = getopt(argc, argv, ":al:p:")) != -1) {
    switch (opt) {
      case 'a':
        log_append = true;
        break;

      case 'l':
        log_path = optarg;
        break;

      case 'p':
        pid_path = optarg;
        break;

      case ':':
        errx(EXIT_FAILURE, "Option -%c requires an argument", optopt);

      case '?':
      default:
        errx(EXIT_FAILURE, "Unknown option -%c", optopt);
    }
  }
  argc -= optind;
  argv += optind;

  if (log_path == NULL) {
    errx(EXIT_FAILURE, "Must specify a log file with -l");
  }
  if (pid_path == NULL) {
    errx(EXIT_FAILURE, "Must specify a pid file with -p");
  }

  if (argc < 2) {
    errx(EXIT_FAILURE, "Must provide at least an executable name and arg0");
  }
  Daemonize(log_path, log_append, pid_path, argv[0], argv + 1);
  return EXIT_SUCCESS;
}
