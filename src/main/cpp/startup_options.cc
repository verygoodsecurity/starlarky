// Copyright 2014 The Bazel Authors. All rights reserved.
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
#include "src/main/cpp/startup_options.h"

#include <assert.h>

#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "src/main/cpp/blaze_util.h"
#include "src/main/cpp/blaze_util_platform.h"
#include "src/main/cpp/util/errors.h"
#include "src/main/cpp/util/exit_code.h"
#include "src/main/cpp/util/file.h"
#include "src/main/cpp/util/logging.h"
#include "src/main/cpp/util/numbers.h"
#include "src/main/cpp/util/path.h"
#include "src/main/cpp/util/path_platform.h"
#include "src/main/cpp/util/strings.h"
#include "src/main/cpp/workspace_layout.h"

namespace blaze {

using std::string;
using std::vector;

void StartupOptions::RegisterNullaryStartupFlag(const std::string &flag_name,
                                                bool *flag_value) {
  all_nullary_startup_flags_[std::string("--") + flag_name] = flag_value;
  all_nullary_startup_flags_[std::string("--no") + flag_name] = flag_value;
}

void StartupOptions::RegisterNullaryStartupFlagNoRc(
    const std::string &flag_name, bool *flag_value) {
  RegisterNullaryStartupFlag(flag_name, flag_value);
  no_rc_nullary_startup_flags_.insert(std::string("--") + flag_name);
  no_rc_nullary_startup_flags_.insert(std::string("--no") + flag_name);
}

void StartupOptions::RegisterSpecialNullaryStartupFlag(
    const std::string &flag_name, SpecialNullaryFlagHandler handler) {
  RegisterNullaryStartupFlag(flag_name, nullptr);
  special_nullary_startup_flags_[std::string("--") + flag_name] = handler;
  special_nullary_startup_flags_[std::string("--no") + flag_name] = handler;
}

void StartupOptions::RegisterUnaryStartupFlag(const std::string &flag_name) {
  valid_unary_startup_flags_.insert(std::string("--") + flag_name);
}

void StartupOptions::OverrideOptionSourcesKey(const std::string &flag_name,
                                              const std::string &new_name) {
  option_sources_key_override_[flag_name] = new_name;
}

StartupOptions::StartupOptions(const string &product_name,
                               const WorkspaceLayout *workspace_layout)
    : product_name(product_name),
      ignore_all_rc_files(false),
      deep_execroot(true),
      block_for_lock(true),
      host_jvm_debug(false),
      batch(false),
      batch_cpu_scheduling(false),
      io_nice_level(-1),
      shutdown_on_low_sys_mem(false),
      oom_more_eagerly(false),
      oom_more_eagerly_threshold(100),
      write_command_log(true),
      watchfs(false),
      fatal_event_bus_exceptions(false),
      command_port(0),
      connect_timeout_secs(30),
      have_invocation_policy_(false),
      client_debug(false),
      java_logging_formatter(
          "com.google.devtools.build.lib.util.SingleLineFormatter"),
      expand_configs_in_place(true),
      digest_function(),
      idle_server_tasks(true),
      original_startup_options_(std::vector<RcStartupFlag>()),
#if defined(__APPLE__)
      macos_qos_class(QOS_CLASS_DEFAULT),
#endif
      unlimit_coredumps(false),
      incompatible_enable_execution_transition(false),
      windows_enable_symlinks(false) {
  if (blaze::IsRunningWithinTest()) {
    output_root = blaze_util::MakeAbsolute(blaze::GetPathEnv("TEST_TMPDIR"));
    max_idle_secs = 15;
    BAZEL_LOG(USER) << "$TEST_TMPDIR defined: output root default is '"
                    << output_root << "' and max_idle_secs default is '"
                    << max_idle_secs << "'.";
  } else {
    output_root = workspace_layout->GetOutputRoot();
    max_idle_secs = 3 * 3600;
    BAZEL_LOG(INFO) << "output root is '" << output_root
                    << "' and max_idle_secs default is '" << max_idle_secs
                    << "'.";
  }

#if defined(_WIN32) || defined(__CYGWIN__)
  string windows_unix_root = DetectBashAndExportBazelSh();
  if (!windows_unix_root.empty()) {
    host_jvm_args.push_back(string("-Dbazel.windows_unix_root=") +
                            windows_unix_root);
  }
#endif  // defined(_WIN32) || defined(__CYGWIN__)

  const string product_name_lower = GetLowercaseProductName();
  output_user_root = blaze_util::JoinPath(
      output_root, "_" + product_name_lower + "_" + GetUserName());

  // IMPORTANT: Before modifying the statements below please contact a Bazel
  // core team member that knows the internal procedure for adding/deprecating
  // startup flags.
  RegisterNullaryStartupFlag("batch", &batch);
  RegisterNullaryStartupFlag("batch_cpu_scheduling", &batch_cpu_scheduling);
  RegisterNullaryStartupFlag("block_for_lock", &block_for_lock);
  RegisterNullaryStartupFlag("client_debug", &client_debug);
  RegisterNullaryStartupFlag("deep_execroot", &deep_execroot);
  RegisterNullaryStartupFlag("expand_configs_in_place",
                             &expand_configs_in_place);
  RegisterNullaryStartupFlag("fatal_event_bus_exceptions",
                             &fatal_event_bus_exceptions);
  RegisterNullaryStartupFlag("host_jvm_debug", &host_jvm_debug);
  RegisterNullaryStartupFlag("idle_server_tasks", &idle_server_tasks);
  RegisterNullaryStartupFlag("incompatible_enable_execution_transition",
                             &incompatible_enable_execution_transition);
  RegisterNullaryStartupFlag("shutdown_on_low_sys_mem",
                             &shutdown_on_low_sys_mem);
  RegisterNullaryStartupFlagNoRc("ignore_all_rc_files", &ignore_all_rc_files);
  RegisterNullaryStartupFlag("unlimit_coredumps", &unlimit_coredumps);
  RegisterNullaryStartupFlag("watchfs", &watchfs);
  RegisterNullaryStartupFlag("write_command_log", &write_command_log);
  RegisterNullaryStartupFlag("windows_enable_symlinks",
                             &windows_enable_symlinks);
  RegisterUnaryStartupFlag("command_port");
  RegisterUnaryStartupFlag("connect_timeout_secs");
  RegisterUnaryStartupFlag("digest_function");
  RegisterUnaryStartupFlag("unix_digest_hash_attribute_name");
  RegisterUnaryStartupFlag("server_javabase");
  RegisterUnaryStartupFlag("host_jvm_args");
  RegisterUnaryStartupFlag("host_jvm_profile");
  RegisterUnaryStartupFlag("invocation_policy");
  RegisterUnaryStartupFlag("io_nice_level");
  RegisterUnaryStartupFlag("install_base");
  RegisterUnaryStartupFlag("macos_qos_class");
  RegisterUnaryStartupFlag("max_idle_secs");
  RegisterUnaryStartupFlag("output_base");
  RegisterUnaryStartupFlag("output_user_root");
  RegisterUnaryStartupFlag("server_jvm_out");
  RegisterUnaryStartupFlag("failure_detail_out");
}

StartupOptions::~StartupOptions() {}

string StartupOptions::GetLowercaseProductName() const {
  string lowercase_product_name = product_name;
  blaze_util::ToLower(&lowercase_product_name);
  return lowercase_product_name;
}

bool StartupOptions::IsUnary(const string &arg) const {
  std::string::size_type i = arg.find_first_of('=');
  if (i == std::string::npos) {
    return valid_unary_startup_flags_.find(arg) !=
           valid_unary_startup_flags_.end();
  } else {
    return valid_unary_startup_flags_.find(arg.substr(0, i)) !=
           valid_unary_startup_flags_.end();
  }
}

bool StartupOptions::MaybeCheckValidNullary(const string &arg, bool *result,
                                            std::string *error) const {
  std::string::size_type i = arg.find_first_of('=');
  if (i == std::string::npos) {
    *result = all_nullary_startup_flags_.find(arg) !=
              all_nullary_startup_flags_.end();
    return true;
  }
  std::string f = arg.substr(0, i);
  if (all_nullary_startup_flags_.find(f) == all_nullary_startup_flags_.end()) {
    *result = false;
    return true;
  }

  blaze_util::StringPrintf(
      error, "In argument '%s': option '%s' does not take a value.",
      arg.c_str(), f.c_str());
  return false;
}

void StartupOptions::AddExtraOptions(vector<string> *result) const {
  if (incompatible_enable_execution_transition) {
    result->push_back("--incompatible_enable_execution_transition");
  } else {
    result->push_back("--noincompatible_enable_execution_transition");
  }
}

blaze_exit_code::ExitCode StartupOptions::ProcessArg(
      const string &argstr, const string &next_argstr, const string &rcfile,
      bool *is_space_separated, string *error) {
  // We have to parse a specific option syntax, so GNU getopts won't do.  All
  // options begin with "--" or "-". Values are given together with the option
  // delimited by '=' or in the next option.
  const char* arg = argstr.c_str();
  const char* next_arg = next_argstr.empty() ? NULL : next_argstr.c_str();
  const char* value = NULL;

  bool is_nullary;
  if (!MaybeCheckValidNullary(argstr, &is_nullary, error)) {
    *is_space_separated = false;
    return blaze_exit_code::BAD_ARGV;
  }

  if (is_nullary) {
    // 'enabled' is true if 'argstr' is "--foo", and false if it's "--nofoo".
    bool enabled = (argstr.compare(0, 4, "--no") != 0);
    if (no_rc_nullary_startup_flags_.find(argstr) !=
        no_rc_nullary_startup_flags_.end()) {
      // no_rc_nullary_startup_flags_ are forbidden in .bazelrc files.
      if (!rcfile.empty()) {
        *error = std::string("Can't specify ") + argstr + " in the " +
                 GetRcFileBaseName() + " file.";
        return blaze_exit_code::BAD_ARGV;
      }
    }
    if (special_nullary_startup_flags_.find(argstr) !=
        special_nullary_startup_flags_.end()) {
      // 'argstr' is either "--foo" or "--nofoo", and the map entry is the
      // lambda that handles setting the flag's value.
      special_nullary_startup_flags_[argstr](enabled);
    } else {
      // 'argstr' is either "--foo" or "--nofoo", and the map entry is the
      // pointer to the bool storing the flag's value.
      *all_nullary_startup_flags_[argstr] = enabled;
    }
    // Use the key "foo" for 'argstr' of "--foo" / "--nofoo", unless there's an
    // overridden name we must use.
    std::string key = argstr.substr(enabled ? 2 : 4);
    if (option_sources_key_override_.find(key) !=
        option_sources_key_override_.end()) {
      key = option_sources_key_override_[key];
    }
    option_sources[key] = rcfile;
    *is_space_separated = false;
    return blaze_exit_code::SUCCESS;
  }

  if ((value = GetUnaryOption(arg, next_arg, "--output_base")) != NULL) {
    output_base = blaze_util::Path(blaze::AbsolutePathFromFlag(value));
    option_sources["output_base"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg,
                                     "--install_base")) != NULL) {
    install_base = blaze::AbsolutePathFromFlag(value);
    option_sources["install_base"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg,
                                     "--output_user_root")) != NULL) {
    output_user_root = blaze::AbsolutePathFromFlag(value);
    option_sources["output_user_root"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg,
                                     "--server_jvm_out")) != NULL) {
    server_jvm_out = blaze_util::Path(blaze::AbsolutePathFromFlag(value));
    option_sources["server_jvm_out"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--failure_detail_out")) !=
             NULL) {
    failure_detail_out = blaze_util::Path(blaze::AbsolutePathFromFlag(value));
    option_sources["failure_detail_out"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--host_jvm_profile")) !=
             NULL) {
    host_jvm_profile = value;
    option_sources["host_jvm_profile"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--server_javabase")) !=
             NULL) {
    // TODO(bazel-team): Consider examining the javabase and re-execing in case
    // of architecture mismatch.
    explicit_server_javabase_ =
        blaze_util::Path(blaze::AbsolutePathFromFlag(value));
    option_sources["server_javabase"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--host_jvm_args")) !=
             NULL) {
    host_jvm_args.push_back(value);
    option_sources["host_jvm_args"] = rcfile;  // NB: This is incorrect
  } else if ((value = GetUnaryOption(arg, next_arg, "--io_nice_level")) !=
             NULL) {
    if (!blaze_util::safe_strto32(value, &io_nice_level) ||
        io_nice_level > 7) {
      blaze_util::StringPrintf(error,
          "Invalid argument to --io_nice_level: '%s'. Must not exceed 7.",
          value);
      return blaze_exit_code::BAD_ARGV;
    }
    option_sources["io_nice_level"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--max_idle_secs")) !=
             NULL) {
    if (!blaze_util::safe_strto32(value, &max_idle_secs) ||
        max_idle_secs < 0) {
      blaze_util::StringPrintf(error,
          "Invalid argument to --max_idle_secs: '%s'.", value);
      return blaze_exit_code::BAD_ARGV;
    }
    option_sources["max_idle_secs"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--macos_qos_class")) !=
             NULL) {
    // We parse the value of this flag on all platforms even if it is
    // macOS-specific to ensure that rc files mentioning it are valid.
    if (strcmp(value, "user-interactive") == 0) {
#if defined(__APPLE__)
      macos_qos_class = QOS_CLASS_USER_INTERACTIVE;
#endif
    } else if (strcmp(value, "user-initiated") == 0) {
#if defined(__APPLE__)
      macos_qos_class = QOS_CLASS_USER_INITIATED;
#endif
    } else if (strcmp(value, "default") == 0) {
#if defined(__APPLE__)
      macos_qos_class = QOS_CLASS_DEFAULT;
#endif
    } else if (strcmp(value, "utility") == 0) {
#if defined(__APPLE__)
      macos_qos_class = QOS_CLASS_UTILITY;
#endif
    } else if (strcmp(value, "background") == 0) {
#if defined(__APPLE__)
      macos_qos_class = QOS_CLASS_BACKGROUND;
#endif
    } else {
      blaze_util::StringPrintf(
          error, "Invalid argument to --macos_qos_class: '%s'.", value);
      return blaze_exit_code::BAD_ARGV;
    }
    option_sources["macos_qos_class"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg,
                                     "--connect_timeout_secs")) != NULL) {
    if (!blaze_util::safe_strto32(value, &connect_timeout_secs) ||
        connect_timeout_secs < 1 || connect_timeout_secs > 120) {
      blaze_util::StringPrintf(error,
          "Invalid argument to --connect_timeout_secs: '%s'.\n"
          "Must be an integer between 1 and 120.\n",
          value);
      return blaze_exit_code::BAD_ARGV;
    }
    option_sources["connect_timeout_secs"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--digest_function")) !=
             NULL) {
    digest_function = value;
    option_sources["digest_function"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg,
                                     "--unix_digest_hash_attribute_name")) !=
             NULL) {
    unix_digest_hash_attribute_name = value;
    option_sources["unix_digest_hash_attribute_name"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--command_port")) !=
             NULL) {
    if (!blaze_util::safe_strto32(value, &command_port) ||
        command_port < 0 || command_port > 65535) {
      blaze_util::StringPrintf(error,
          "Invalid argument to --command_port: '%s'.\n"
          "Must be a valid port number or 0.\n",
          value);
      return blaze_exit_code::BAD_ARGV;
    }
    option_sources["command_port"] = rcfile;
  } else if ((value = GetUnaryOption(arg, next_arg, "--invocation_policy")) !=
             NULL) {
    if (!have_invocation_policy_) {
      have_invocation_policy_ = true;
      invocation_policy = value;
      option_sources["invocation_policy"] = rcfile;
    } else {
      *error = "The startup flag --invocation_policy cannot be specified "
          "multiple times.";
      return blaze_exit_code::BAD_ARGV;
    }
  } else {
    bool extra_argument_processed;
    blaze_exit_code::ExitCode process_extra_arg_exit_code = ProcessArgExtra(
        arg, next_arg, rcfile, &value, &extra_argument_processed, error);
    if (process_extra_arg_exit_code != blaze_exit_code::SUCCESS) {
      return process_extra_arg_exit_code;
    }
    if (!extra_argument_processed) {
      blaze_util::StringPrintf(
          error,
          "Unknown startup option: '%s'.\n"
          "  For more info, run '%s help startup_options'.",
          arg, GetLowercaseProductName().c_str());
      return blaze_exit_code::BAD_ARGV;
    }
  }

  *is_space_separated = ((value == next_arg) && (value != NULL));
  return blaze_exit_code::SUCCESS;
}

blaze_exit_code::ExitCode StartupOptions::ProcessArgs(
    const std::vector<RcStartupFlag>& rcstartup_flags,
    std::string *error) {
  std::vector<RcStartupFlag>::size_type i = 0;
  while (i < rcstartup_flags.size()) {
    bool is_space_separated = false;
    const std::string next_value =
        (i == rcstartup_flags.size() - 1) ? "" : rcstartup_flags[i + 1].value;
    const blaze_exit_code::ExitCode process_arg_exit_code =
        ProcessArg(rcstartup_flags[i].value, next_value,
                   rcstartup_flags[i].source, &is_space_separated, error);
    // Store the provided option in --flag(=value)? form. Store these before
    // propagating any error code, since we want to have the correct
    // information for the output. The fact that the options aren't parseable
    // doesn't matter for this step.
    if (is_space_separated) {
      const std::string combined_value =
          rcstartup_flags[i].value + "=" + next_value;
      original_startup_options_.push_back(
          RcStartupFlag(rcstartup_flags[i].source, combined_value));
      i += 2;
    } else {
      original_startup_options_.push_back(
          RcStartupFlag(rcstartup_flags[i].source, rcstartup_flags[i].value));
      i++;
    }

    if (process_arg_exit_code != blaze_exit_code::SUCCESS) {
      return process_arg_exit_code;
    }
  }
  return blaze_exit_code::SUCCESS;
}

blaze_util::Path StartupOptions::GetSystemJavabase() const {
  return blaze_util::Path(blaze::GetSystemJavabase());
}

blaze_util::Path StartupOptions::GetEmbeddedJavabase() const {
  blaze_util::Path bundled_jre_path = blaze_util::Path(
      blaze_util::JoinPath(install_base, "embedded_tools/jdk"));
  if (blaze_util::CanExecuteFile(
          bundled_jre_path.GetRelative(GetJavaBinaryUnderJavabase()))) {
    return bundled_jre_path;
  }
  return blaze_util::Path();
}

std::pair<blaze_util::Path, StartupOptions::JavabaseType>
StartupOptions::GetServerJavabaseAndType() const {
  // 1) Allow overriding the server_javabase via --server_javabase.
  if (!explicit_server_javabase_.IsEmpty()) {
    return std::pair<blaze_util::Path, JavabaseType>(explicit_server_javabase_,
                                                     JavabaseType::EXPLICIT);
  }
  if (default_server_javabase_.first.IsEmpty()) {
    blaze_util::Path bundled_jre_path = GetEmbeddedJavabase();
    if (!bundled_jre_path.IsEmpty()) {
      // 2) Use a bundled JVM if we have one.
      default_server_javabase_ = std::pair<blaze_util::Path, JavabaseType>(
          bundled_jre_path, JavabaseType::EMBEDDED);
    } else {
      // 3) Otherwise fall back to using the default system JVM.
      blaze_util::Path system_javabase = GetSystemJavabase();
      if (system_javabase.IsEmpty()) {
        BAZEL_DIE(blaze_exit_code::LOCAL_ENVIRONMENTAL_ERROR)
            << "Could not find system javabase. Ensure JAVA_HOME is set, or "
               "javac is on your PATH.";
      }
      default_server_javabase_ = std::pair<blaze_util::Path, JavabaseType>(
          system_javabase, JavabaseType::SYSTEM);
    }
  }
  return default_server_javabase_;
}

blaze_util::Path StartupOptions::GetServerJavabase() const {
  return GetServerJavabaseAndType().first;
}

blaze_util::Path StartupOptions::GetExplicitServerJavabase() const {
  return explicit_server_javabase_;
}

blaze_util::Path StartupOptions::GetJvm() const {
  auto javabase_and_type = GetServerJavabaseAndType();
  blaze_exit_code::ExitCode sanity_check_code =
      SanityCheckJavabase(javabase_and_type.first, javabase_and_type.second);
  if (sanity_check_code != blaze_exit_code::SUCCESS) {
    exit(sanity_check_code);
  }
  return javabase_and_type.first.GetRelative(GetJavaBinaryUnderJavabase());
}

// Prints an appropriate error message and returns an appropriate error exit
// code for a server javabase which failed sanity checks.
static blaze_exit_code::ExitCode BadServerJavabaseError(
    StartupOptions::JavabaseType javabase_type,
    const std::map<string, string> &option_sources) {
  switch (javabase_type) {
    case StartupOptions::JavabaseType::EXPLICIT: {
      auto source = option_sources.find("server_javabase");
      string rc_file;
      if (source != option_sources.end() && !source->second.empty()) {
        rc_file = source->second;
      }
      BAZEL_LOG(ERROR)
          << "  The java path was specified by a '--server_javabase' option " +
                 (rc_file.empty() ? "on the command line" : "in " + rc_file);
      return blaze_exit_code::BAD_ARGV;
    }
    case StartupOptions::JavabaseType::EMBEDDED:
      BAZEL_LOG(ERROR) << "  Internal error: embedded JDK fails sanity check.";
      return blaze_exit_code::INTERNAL_ERROR;
    case StartupOptions::JavabaseType::SYSTEM:
      return blaze_exit_code::LOCAL_ENVIRONMENTAL_ERROR;
    default:
      BAZEL_LOG(ERROR)
          << "  Internal error: server javabase type was not initialized.";
      // Fall through.
  }
  return blaze_exit_code::INTERNAL_ERROR;
}

blaze_exit_code::ExitCode StartupOptions::SanityCheckJavabase(
    const blaze_util::Path &javabase,
    StartupOptions::JavabaseType javabase_type) const {
  blaze_util::Path java_program =
      javabase.GetRelative(GetJavaBinaryUnderJavabase());
  if (!blaze_util::CanExecuteFile(java_program)) {
    if (!blaze_util::PathExists(java_program)) {
      BAZEL_LOG(ERROR) << "Couldn't find java at '"
                       << java_program.AsPrintablePath() << "'.";
    } else {
      string err = blaze_util::GetLastErrorString();
      BAZEL_LOG(ERROR) << "Java at '" << java_program.AsPrintablePath()
                       << "' exists but is not executable: " << err;
    }
    return BadServerJavabaseError(javabase_type, option_sources);
  }
  if (  // If the full JDK is installed
      blaze_util::CanReadFile(javabase.GetRelative("jre/lib/rt.jar")) ||
      // If just the JRE is installed
      blaze_util::CanReadFile(javabase.GetRelative("lib/rt.jar")) ||
      // rt.jar does not exist in java 9+ so check for java instead
      blaze_util::CanReadFile(javabase.GetRelative("bin/java")) ||
      blaze_util::CanReadFile(javabase.GetRelative("bin/java.exe"))) {
    return blaze_exit_code::SUCCESS;
  }
  BAZEL_LOG(ERROR) << "Problem with java installation: couldn't find/access "
                      "rt.jar or java in "
                   << javabase.AsPrintablePath();
  return BadServerJavabaseError(javabase_type, option_sources);
}

blaze_util::Path StartupOptions::GetExe(const blaze_util::Path &jvm,
                                        const string &jar_path) const {
  return jvm;
}

void StartupOptions::AddJVMArgumentPrefix(const blaze_util::Path &javabase,
                                          std::vector<string> *result) const {}

void StartupOptions::AddJVMArgumentSuffix(
    const blaze_util::Path &real_install_dir, const string &jar_path,
    std::vector<string> *result) const {
  result->push_back("-jar");
  result->push_back(real_install_dir.GetRelative(jar_path).AsJvmArgument());
}

blaze_exit_code::ExitCode StartupOptions::AddJVMArguments(
    const blaze_util::Path &server_javabase, std::vector<string> *result,
    const vector<string> &user_options, string *error) const {
  AddJVMLoggingArguments(result);

  // Disable the JVM's own unlimiting of file descriptors.  We do this
  // ourselves in blaze.cc so we want our setting to propagate to the JVM.
  //
  // The reason to do this is that the JVM's unlimiting is suboptimal on
  // macOS.  Under that platform, the JVM limits the open file descriptors
  // to the OPEN_MAX constant... which is much lower than the per-process
  // kernel allowed limit of kern.maxfilesperproc (which is what we set
  // ourselves to).
  result->push_back("-XX:-MaxFDLimit");

  return AddJVMMemoryArguments(server_javabase, result, user_options, error);
}

static std::string GetSimpleLogHandlerProps(
    const blaze_util::Path &java_log,
    const std::string &java_logging_formatter) {
  return "handlers=com.google.devtools.build.lib.util.SimpleLogHandler\n"
         ".level=INFO\n"
         "com.google.devtools.build.lib.util.SimpleLogHandler.level=INFO\n"
         "com.google.devtools.build.lib.util.SimpleLogHandler.prefix=" +
         java_log.AsJvmArgument() +
         "\n"
         "com.google.devtools.build.lib.util.SimpleLogHandler.limit=1024000\n"
         "com.google.devtools.build.lib.util.SimpleLogHandler.total_limit="
         "20971520\n"  // 20 MB.
         "com.google.devtools.build.lib.util.SimpleLogHandler.formatter=" +
         java_logging_formatter + "\n";
}

void StartupOptions::AddJVMLoggingArguments(std::vector<string> *result) const {
  // Configure logging
  const blaze_util::Path propFile =
      output_base.GetRelative("javalog.properties");
  const blaze_util::Path java_log = output_base.GetRelative("java.log");
  const std::string loggingProps =
      GetSimpleLogHandlerProps(java_log, java_logging_formatter);

  if (!blaze_util::WriteFile(loggingProps, propFile)) {
    perror(
        ("Couldn't write logging file " + propFile.AsPrintablePath()).c_str());
  } else {
    result->push_back("-Djava.util.logging.config.file=" +
                      propFile.AsJvmArgument());
    result->push_back(
        "-Dcom.google.devtools.build.lib.util.LogHandlerQuerier.class="
        "com.google.devtools.build.lib.util.SimpleLogHandler$HandlerQuerier");
  }
}

blaze_exit_code::ExitCode StartupOptions::AddJVMMemoryArguments(
    const blaze_util::Path &, std::vector<string> *, const vector<string> &,
    string *) const {
  return blaze_exit_code::SUCCESS;
}

}  // namespace blaze
