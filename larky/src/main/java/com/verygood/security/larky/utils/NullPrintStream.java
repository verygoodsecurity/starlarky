package com.verygood.security.larky.utils;

import com.google.common.io.ByteStreams;

import java.io.PrintStream;

public final class NullPrintStream extends PrintStream {
  @SuppressWarnings("UnstableApiUsage")
  public NullPrintStream() {
    super(ByteStreams.nullOutputStream());
  }
}
