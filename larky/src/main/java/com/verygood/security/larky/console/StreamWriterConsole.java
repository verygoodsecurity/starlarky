package com.verygood.security.larky.console;

import java.io.IOException;
import java.io.Writer;

public class StreamWriterConsole implements Console {

  private final Writer output;
  private final boolean verbose;
  private final boolean autoFlush;

  public StreamWriterConsole(final Writer writer) {
    this(writer, false);
  }

  public StreamWriterConsole(final Writer writer, boolean verbose) {
    this(writer, verbose, false);
  }

  public StreamWriterConsole(final Writer writer, boolean verbose, boolean autoFlush) {
    this.output = writer;
    this.verbose = verbose;
    this.autoFlush = autoFlush;
  }

  @Override
  public void startupMessage(String version) {
    try {
      output.write(String.format("Starlarky (Version: %1$s)%n", version));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isVerbose() {
    return verbose;
  }

  @Override
  public void error(String message) {
    printMessage(message);
  }

  @Override
  public void warn(String message) {
    printMessage(message);
  }

  @Override
  public void info(String message) {
    printMessage(message);
  }

  @Override
  public void progress(final String task) {
    printMessage(task);
  }

  /**
   * Returns true if this Console's input registers Y/y after showing the prompt message.
   */
  @Override
  public boolean promptConfirmation(String message) {
    return false;
  }

  /**
   * Given a message and a console that support colors, return a string that prints the message in
   * the {@code ansiColor}.
   *
   * <p>Note that not all consoles support colors. so messages should be readable without colors.
   */
  @Override
  public String colorize(AnsiColor ansiColor, String message) {
    return message;
  }

  private void printMessage(String message) {
    try {
      output.write(String.format("%s%n", message));
      if(autoFlush) {
        output.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
