// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.verygood.security.larky.debug;

import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.Reporter;
import com.google.devtools.build.lib.starlarkdebug.server.StarlarkDebugServer;
import java.io.IOException;
import net.starlark.java.eval.Debug;

public final class StarlarkDebuggerModule {

  public static void initializeDebugging(Reporter reporter, String path, int debugPort, boolean verboseLogs) {
    try {
      StarlarkDebugServer server =
          StarlarkDebugServer.createAndWaitForConnection(reporter, debugPort, verboseLogs);
      server.setFirstBreakpoint(path);
      Debug.setDebugger(server);
    } catch (IOException e) {
      reporter.handle(Event.error("Error while setting up the debug server: " + e.getMessage()));
    }
  }

  public static void disableDebugging() {
    Debug.setDebugger(null);
  }
}
