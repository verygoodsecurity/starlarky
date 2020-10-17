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
package com.google.devtools.build.lib.util;

import com.google.common.flogger.GoogleLogger;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility methods relating to threads and stack traces.
 */
public class ThreadUtils {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private ThreadUtils() {
  }

  /** Write a thread dump to the blaze.INFO log if interrupt took too long. */
  public static synchronized void warnAboutSlowInterrupt() {
    logger.atWarning().log("Interrupt took too long. Dumping thread state.");
    for (Map.Entry <Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
      Thread t = e.getKey();
      logger.atWarning().log("\"%s\"  Thread id=%d %s", t.getName(), t.getId(), t.getState());
      for (StackTraceElement line : e.getValue()) {
        logger.atWarning().log("\t%s", line);
      }
      logger.atWarning().log("");
    }
    LoggingUtil.logToRemote(Level.WARNING, "Slow interrupt", new SlowInterruptException());
  }

  private static final class SlowInterruptException extends RuntimeException {
    public SlowInterruptException() {
      super("Slow interruption...");
    }
  }
}
