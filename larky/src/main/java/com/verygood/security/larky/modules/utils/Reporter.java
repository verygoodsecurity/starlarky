package com.verygood.security.larky.modules.utils;

import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.LogConsole;

import net.starlark.java.eval.StarlarkThread;

import java.util.List;


public class Reporter {

  private final Console console;

  public Reporter(Console console) {
    this.console = console;
  }

  public void report(StarlarkThread thread, String msg) {
    report(thread, msg, console);
  }

  public void reportError(StarlarkThread thread, String message) {
    reportError(thread, message, console);
  }

  static public void report(StarlarkThread thread, String msg, Console console) {
    // limit the size of a string to 1K (1024 characters)
    msg = msg.length() <= 1024 ? msg : msg.substring(0, 1024);
    if (console == null) {
      console = LogConsole.writeOnlyConsole(System.err, true);
    }
    if (console.isVerbose()) {
      console.verbose(thread.getCallerLocation() + ": " + msg);
    } else {
      console.info(msg);
    }
  }

  /**
   * Should be called by an assertion method when the test encounters an unexpected evaluation. It
   * does not stop the program; multiple failures may be reported in a single run.
   */
  static public void reportError(StarlarkThread thread, String message, Console console) {
    if (console == null) {
      console = LogConsole.writeOnlyConsole(System.err, true);
    }
    console.error("Traceback (most recent call last):\n");
    List<StarlarkThread.CallStackEntry> stack = thread.getCallStack();
    stack = stack.subList(0, stack.size() - 1); // pop the built-in function
    for (StarlarkThread.CallStackEntry fr : stack) {
      console.error(String.format("%s: called from %s\n", fr.location, fr.name));
    }
    console.error(String.format("Error: %s%s", message, System.lineSeparator()));
  }

  public void error(String s) {
    console.error(s);
  }
}
