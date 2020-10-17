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

package com.google.devtools.build.lib.exec;

import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.actions.AbstractAction;
import com.google.devtools.build.lib.actions.ActionExecutionContext;
import com.google.devtools.build.lib.actions.EnvironmentalExecException;
import com.google.devtools.build.lib.actions.RunningActionEvent;
import com.google.devtools.build.lib.actions.SpawnContinuation;
import com.google.devtools.build.lib.analysis.actions.DeterministicWriter;
import com.google.devtools.build.lib.analysis.actions.FileWriteActionContext;
import com.google.devtools.build.lib.profiler.AutoProfiler;
import com.google.devtools.build.lib.profiler.GoogleAutoProfilerUtils;
import com.google.devtools.build.lib.server.FailureDetails.Execution.Code;
import com.google.devtools.build.lib.vfs.Path;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;

/**
 * A strategy for executing an {@link
 * com.google.devtools.build.lib.analysis.actions.AbstractFileWriteAction}.
 */
public final class FileWriteStrategy implements FileWriteActionContext {
  public static final Class<FileWriteStrategy> TYPE = FileWriteStrategy.class;
  private static final Duration MIN_LOGGING = Duration.ofMillis(100);

  @Override
  public SpawnContinuation beginWriteOutputToFile(
      AbstractAction action,
      ActionExecutionContext actionExecutionContext,
      DeterministicWriter deterministicWriter,
      boolean makeExecutable,
      boolean isRemotable) {
    actionExecutionContext.getEventHandler().post(new RunningActionEvent(action, "local"));
    // TODO(ulfjack): Consider acquiring local resources here before trying to write the file.
    try (AutoProfiler p =
        GoogleAutoProfilerUtils.logged(
            "running write for action " + action.prettyPrint(), MIN_LOGGING)) {
      Path outputPath =
          actionExecutionContext.getInputPath(Iterables.getOnlyElement(action.getOutputs()));
      try {
        try (OutputStream out = new BufferedOutputStream(outputPath.getOutputStream())) {
          deterministicWriter.writeOutputFile(out);
        }
        if (makeExecutable) {
          outputPath.setExecutable(true);
        }
      } catch (IOException e) {
        return SpawnContinuation.failedWithExecException(
            new EnvironmentalExecException(e, Code.FILE_WRITE_IO_EXCEPTION));
      }
    }
    return SpawnContinuation.immediate();
  }
}
