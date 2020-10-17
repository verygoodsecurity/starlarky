// Copyright 2015 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.runtime;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.flogger.GoogleLogger;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.devtools.build.lib.actions.ActionCompletionEvent;
import com.google.devtools.build.lib.actions.ActionScanningCompletedEvent;
import com.google.devtools.build.lib.actions.ActionStartedEvent;
import com.google.devtools.build.lib.actions.RunningActionEvent;
import com.google.devtools.build.lib.actions.ScanningActionEvent;
import com.google.devtools.build.lib.actions.SchedulingActionEvent;
import com.google.devtools.build.lib.actions.StoppedScanningActionEvent;
import com.google.devtools.build.lib.analysis.AnalysisPhaseCompleteEvent;
import com.google.devtools.build.lib.analysis.NoBuildEvent;
import com.google.devtools.build.lib.analysis.NoBuildRequestFinishedEvent;
import com.google.devtools.build.lib.buildeventstream.AnnounceBuildEventTransportsEvent;
import com.google.devtools.build.lib.buildeventstream.BuildEventTransport;
import com.google.devtools.build.lib.buildeventstream.BuildEventTransportClosedEvent;
import com.google.devtools.build.lib.buildtool.buildevent.BuildCompleteEvent;
import com.google.devtools.build.lib.buildtool.buildevent.BuildStartingEvent;
import com.google.devtools.build.lib.buildtool.buildevent.ExecutionProgressReceiverAvailableEvent;
import com.google.devtools.build.lib.buildtool.buildevent.TestFilteringCompleteEvent;
import com.google.devtools.build.lib.clock.Clock;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.EventHandler;
import com.google.devtools.build.lib.events.EventKind;
import com.google.devtools.build.lib.events.ExtendedEventHandler.FetchProgress;
import com.google.devtools.build.lib.pkgcache.LoadingPhaseCompleteEvent;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.skyframe.ConfigurationPhaseStartedEvent;
import com.google.devtools.build.lib.skyframe.LoadingPhaseStartedEvent;
import com.google.devtools.build.lib.util.io.AnsiTerminal;
import com.google.devtools.build.lib.util.io.AnsiTerminal.Color;
import com.google.devtools.build.lib.util.io.AnsiTerminalWriter;
import com.google.devtools.build.lib.util.io.LineCountingAnsiTerminalWriter;
import com.google.devtools.build.lib.util.io.LineWrappingAnsiTerminalWriter;
import com.google.devtools.build.lib.util.io.LoggingTerminalWriter;
import com.google.devtools.build.lib.util.io.OutErr;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.view.test.TestStatus.BlazeTestStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.starlark.java.syntax.Location;

/** An experimental new output stream. */
public class UiEventHandler implements EventHandler {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  /** Latest refresh of the progress bar, if contents other than time changed */
  static final long MAXIMAL_UPDATE_DELAY_MILLIS = 200L;
  /** Minimal rate limiting (in ms), if the progress bar cannot be updated in place */
  static final long NO_CURSES_MINIMAL_PROGRESS_RATE_LIMIT = 1000L;
  /**
   * Minimal rate limiting, as fraction of the request time so far, if the progress bar cannot be
   * updated in place
   */
  static final double NO_CURSES_MINIMAL_RELATIVE_PROGRESS_RATE_LMIT = 0.15;
  /** Periodic update interval of a time-dependent progress bar if it can be updated in place */
  static final long SHORT_REFRESH_MILLIS = 1000L;
  /** Periodic update interval of a time-dependent progress bar if it cannot be updated in place */
  static final long LONG_REFRESH_MILLIS = 20000L;

  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("(HH:mm:ss) ");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final boolean cursorControl;
  private final Clock clock;
  private final long uiStartTimeMillis;
  private final AnsiTerminal terminal;
  private final boolean debugAllEvents;
  private final UiStateTracker stateTracker;
  private final LocationPrinter locationPrinter;
  private final boolean showProgress;
  private final boolean progressInTermTitle;
  private final boolean showTimestamp;
  private final OutErr outErr;
  private final ImmutableSet<EventKind> filteredEvents;
  private long minimalDelayMillis;
  private long minimalUpdateInterval;
  private long lastRefreshMillis;
  private long mustRefreshAfterMillis;
  private boolean dateShown;
  private int numLinesProgressBar;
  private boolean buildRunning;
  // Number of open build even protocol transports.
  private boolean progressBarNeedsRefresh;
  private volatile boolean shutdown;
  private final AtomicReference<Thread> updateThread;
  private final Lock updateLock;
  private ByteArrayOutputStream stdoutLineBuffer;
  private ByteArrayOutputStream stderrLineBuffer;

  private final int maxStdoutErrBytes;
  public final int terminalWidth;

  /**
   * An output stream that wraps another output stream and that fully buffers writes until flushed.
   */
  private static class FullyBufferedOutputStream extends ByteArrayOutputStream {
    /** The (possibly unbuffered) stream wrapped by this one. */
    private final OutputStream wrapped;

    /**
     * Constructs a new fully-buffered output stream that wraps an unbuffered one.
     *
     * @param wrapped the (possibly unbuffered) stream wrapped by this one
     */
    FullyBufferedOutputStream(OutputStream wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public void flush() throws IOException {
      super.flush();
      try {
        writeTo(wrapped);
        wrapped.flush();
      } finally {
        // If we failed to write our current buffered contents to the output, there is not much
        // we can do because reporting an error would require another write, and that write would
        // probably fail. So, instead, we silently discard whatever was previously buffered in the
        // hopes that the data itself was what caused the problem.
        reset();
      }
    }
  }

  public UiEventHandler(
      OutErr outErr, UiOptions options, Clock clock, @Nullable PathFragment workspacePathFragment) {
    this.terminalWidth = (options.terminalColumns > 0 ? options.terminalColumns : 80);
    this.maxStdoutErrBytes = options.maxStdoutErrBytes;
    this.outErr =
        OutErr.create(
            new FullyBufferedOutputStream(outErr.getOutputStream()),
            new FullyBufferedOutputStream(outErr.getErrorStream()));
    this.cursorControl = options.useCursorControl();
    this.terminal = new AnsiTerminal(this.outErr.getErrorStream());
    this.showProgress = options.showProgress;
    this.progressInTermTitle = options.progressInTermTitle && options.useCursorControl();
    this.showTimestamp = options.showTimestamp;
    this.clock = clock;
    this.uiStartTimeMillis = clock.currentTimeMillis();
    this.debugAllEvents = options.experimentalUiDebugAllEvents;
    this.locationPrinter =
        new LocationPrinter(options.attemptToPrintRelativePaths, workspacePathFragment);
    // If we have cursor control, we try to fit in the terminal width to avoid having
    // to wrap the progress bar. We will wrap the progress bar to terminalWidth - 1
    // characters to avoid depending on knowing whether the underlying terminal does the
    // line feed already when reaching the last character of the line, or only once an
    // additional character is written. Another column is lost for the continuation character
    // in the wrapping process.
    this.stateTracker =
        this.cursorControl
            ? new UiStateTracker(clock, this.terminalWidth - 2)
            : new UiStateTracker(clock);
    this.stateTracker.setProgressMode(options.uiProgressMode, options.uiSamplesShown);
    this.numLinesProgressBar = 0;
    if (this.cursorControl) {
      this.minimalDelayMillis = Math.round(options.showProgressRateLimit * 1000);
    } else {
      this.minimalDelayMillis =
          Math.max(
              Math.round(options.showProgressRateLimit * 1000),
              NO_CURSES_MINIMAL_PROGRESS_RATE_LIMIT);
    }
    this.minimalUpdateInterval = Math.max(this.minimalDelayMillis, MAXIMAL_UPDATE_DELAY_MILLIS);
    this.stdoutLineBuffer = new ByteArrayOutputStream();
    this.stderrLineBuffer = new ByteArrayOutputStream();
    this.dateShown = false;
    this.updateThread = new AtomicReference<>();
    this.updateLock = new ReentrantLock();
    this.filteredEvents = ImmutableSet.copyOf(options.eventFilters);
    // The progress bar has not been updated yet.
    ignoreRefreshLimitOnce();
  }

  /**
   * Flush buffers for stdout and stderr. Return if either of them flushed a non-zero number of
   * symbols.
   */
  private synchronized boolean flushStdOutStdErrBuffers() {
    boolean didFlush = false;
    try {
      if (stdoutLineBuffer.size() > 0) {
        stdoutLineBuffer.writeTo(outErr.getOutputStream());
        outErr.getOutputStream().flush();
        // Re-initialize the stream not to retain allocated memory.
        stdoutLineBuffer = new ByteArrayOutputStream();
        didFlush = true;
      }
      if (stderrLineBuffer.size() > 0) {
        stderrLineBuffer.writeTo(outErr.getErrorStream());
        outErr.getErrorStream().flush();
        // Re-initialize the stream not to retain allocated memory.
        stderrLineBuffer = new ByteArrayOutputStream();
        didFlush = true;
      }
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("IO Error writing to output stream");
    }
    return didFlush;
  }

  private synchronized void maybeAddDate() {
    if (!showTimestamp || dateShown || !buildRunning) {
      return;
    }
    dateShown = true;
    handle(
        Event.info(
            null,
            "Current date is "
                + DATE_FORMAT.format(
                    Instant.ofEpochMilli(clock.currentTimeMillis())
                        .atZone(ZoneId.systemDefault()))));
  }

  /**
   * Helper function for {@link #handleInternal} to process events in debug mode, which causes all
   * events to be dumped to the terminal.
   *
   * @param event the event to process
   * @param stdout the event's stdout, already read from disk to avoid blocking within the critical
   *     section. Null if there is no stdout for this event or if it is empty.
   * @param stderr the event's stderr, already read from disk to avoid blocking within the critical
   *     section. Null if there is no stderr for this event or if it is empty.
   */
  private void handleLockedDebug(Event event, @Nullable byte[] stdout, @Nullable byte[] stderr)
      throws IOException {
    synchronized (this) {
      // Debugging only: show all events visible to the new UI.
      clearProgressBar();
      terminal.flush();
      OutputStream stream = outErr.getOutputStream();
      stream.write((event + "\n").getBytes(StandardCharsets.ISO_8859_1));
      if (stdout != null) {
        stream.write("... with STDOUT: ".getBytes(StandardCharsets.ISO_8859_1));
        stream.write(stdout);
        stream.write("\n".getBytes(StandardCharsets.ISO_8859_1));
      }
      if (stderr != null) {
        stream.write("... with STDERR: ".getBytes(StandardCharsets.ISO_8859_1));
        stream.write(stderr);
        stream.write("\n".getBytes(StandardCharsets.ISO_8859_1));
      }
      stream.flush();
      addProgressBar();
      terminal.flush();
    }
  }

  /**
   * Helper function for {@link #handleInternal} to process events in non-debug mode, which filters
   * out and pretty-prints some events.
   *
   * @param event the event to process
   * @param stdout the event's stdout, already read from disk to avoid blocking within the critical
   *     section. Null if there is no stdout for this event or if it is empty.
   * @param stderr the event's stderr, already read from disk to avoid blocking within the critical
   *     section. Null if there is no stderr for this event or if it is empty.
   */
  private void handleLocked(Event event, @Nullable byte[] stdout, @Nullable byte[] stderr)
      throws IOException {
    synchronized (this) {
      maybeAddDate();
      switch (event.getKind()) {
        case STDOUT:
        case STDERR:
          OutputStream stream =
              event.getKind() == EventKind.STDOUT
                  ? outErr.getOutputStream()
                  : outErr.getErrorStream();
          if (!buildRunning) {
            stream.write(event.getMessageBytes());
            stream.flush();
          } else {
            writeToStream(
                stream,
                event.getKind(),
                event.getMessageBytes(),
                /* readdProgressBar= */ showProgress && cursorControl);
          }
          break;
        case ERROR:
        case FAIL:
        case WARNING:
        case CANCELLED:
        case INFO:
        case DEBUG:
        case SUBCOMMAND:
          boolean incompleteLine;
          if (showProgress && buildRunning) {
            clearProgressBar();
          }
          incompleteLine = flushStdOutStdErrBuffers();
          if (incompleteLine) {
            crlf();
          }
          if (showTimestamp) {
            terminal.writeString(
                TIMESTAMP_FORMAT.format(
                    Instant.ofEpochMilli(clock.currentTimeMillis())
                        .atZone(ZoneId.systemDefault())));
          }
          setEventKindColor(event.getKind());
          terminal.writeString(event.getKind() + ": ");
          terminal.resetTerminal();
          incompleteLine = true;
          Location location = event.getLocation();
          if (location != null) {
            terminal.writeString(locationPrinter.getLocationString(location) + ": ");
          }
          if (event.getMessage() != null) {
            terminal.writeString(event.getMessage());
            incompleteLine = !event.getMessage().endsWith("\n");
          }
          if (incompleteLine) {
            crlf();
          }
          if (showProgress && buildRunning && cursorControl) {
            addProgressBar();
          }
          terminal.flush();
          break;
        case PROGRESS:
          if (stateTracker.progressBarTimeDependent()) {
            refresh();
          }
          break;
        case START:
        case FINISH:
        case PASS:
        case TIMEOUT:
        case DEPCHECKER:
          break;
      }
      if (stdout != null || stderr != null) {
        clearProgressBar();
        terminal.flush();
        if (stderr != null) {
          writeToStream(
              outErr.getErrorStream(), EventKind.STDERR, stderr, /* readdProgressBar= */ false);
          outErr.getErrorStream().flush();
        }
        if (stdout != null) {
          writeToStream(
              outErr.getOutputStream(), EventKind.STDOUT, stdout, /* readdProgressBar= */ false);
          outErr.getOutputStream().flush();
        }
        if (showProgress && cursorControl) {
          addProgressBar();
        }
        terminal.flush();
      }
    }
  }

  @Nullable
  private byte[] getContentIfSmallEnough(
      String name, long size, Supplier<byte[]> getContent, Supplier<PathFragment> getPath) {
    if (size == 0) {
      // Avoid any possible I/O when we know it'll be empty anyway.
      return null;
    }

    if (size < maxStdoutErrBytes) {
      return getContent.get();
    } else {
      return String.format(
              "%s (%s) exceeds maximum size of --experimental_ui_max_stdouterr_bytes=%d bytes;"
                  + " skipping\n",
              name, getPath.get(), maxStdoutErrBytes)
          .getBytes(StandardCharsets.ISO_8859_1);
    }
  }

  private void handleInternal(Event event) {
    if (this.filteredEvents.contains(event.getKind())) {
      return;
    }
    try {
      // stdout and stderr may be files. Buffer them in memory to avoid doing I/O in the critical
      // sections of handleLocked*, at the expense of having to cap their size to avoid using too
      // much memory.
      byte[] stdout = null;
      byte[] stderr = null;
      if (event.hasStdoutStderr()) {
        stdout =
            getContentIfSmallEnough(
                "stdout", event.getStdOutSize(), event::getStdOut, event::getStdOutPathFragment);
        stderr =
            getContentIfSmallEnough(
                "stderr", event.getStdErrSize(), event::getStdErr, event::getStdErrPathFragment);
      }

      if (debugAllEvents) {
        handleLockedDebug(event, stdout, stderr);
      } else {
        handleLocked(event, stdout, stderr);
      }
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("IO Error writing to output stream");
    }
  }

  @Override
  public void handle(Event event) {
    if (!debugAllEvents
        && !showTimestamp
        && (event.getKind() == EventKind.START
            || event.getKind() == EventKind.FINISH
            || event.getKind() == EventKind.PASS
            || event.getKind() == EventKind.TIMEOUT
            || event.getKind() == EventKind.DEPCHECKER)) {
      // Keep this in sync with the list of no-op event kinds in actuallyHandle above.
      return;
    }
    handleInternal(event);
  }

  private void writeToStream(
      OutputStream stream, EventKind eventKind, byte[] message, boolean readdProgressBar)
      throws IOException {
    int eolIndex = Bytes.lastIndexOf(message, (byte) '\n');
    ByteArrayOutputStream outLineBuffer =
        eventKind == EventKind.STDOUT ? stdoutLineBuffer : stderrLineBuffer;
    if (eolIndex < 0) {
      outLineBuffer.write(message);
      return;
    }

    clearProgressBar();
    terminal.flush();

    // Write the buffer so far + the rest of the line (including newline).
    outLineBuffer.writeTo(stream);
    outLineBuffer.reset();

    stream.write(message, 0, eolIndex + 1);
    stream.flush();

    outLineBuffer.write(message, eolIndex + 1, message.length - eolIndex - 1);
    if (readdProgressBar) {
      addProgressBar();
      terminal.flush();
    }
  }

  private void setEventKindColor(EventKind kind) throws IOException {
    switch (kind) {
      case ERROR:
      case FAIL:
        terminal.setTextColor(Color.RED);
        terminal.textBold();
        break;
      case WARNING:
      case CANCELLED:
        terminal.setTextColor(Color.MAGENTA);
        break;
      case INFO:
        terminal.setTextColor(Color.GREEN);
        break;
      case DEBUG:
        terminal.setTextColor(Color.YELLOW);
        break;
      case SUBCOMMAND:
        terminal.setTextColor(Color.BLUE);
        break;
      default:
        terminal.resetTerminal();
    }
  }

  @Subscribe
  public void buildStarted(BuildStartingEvent event) {
    synchronized (this) {
      buildRunning = true;
    }
    maybeAddDate();
    stateTracker.buildStarted(event);
    // As a new phase started, inform immediately.
    ignoreRefreshLimitOnce();
    refresh();
  }

  @Subscribe
  public void loadingStarted(LoadingPhaseStartedEvent event) {
    maybeAddDate();
    stateTracker.loadingStarted(event);
    // As a new phase started, inform immediately.
    ignoreRefreshLimitOnce();
    refresh();
    startUpdateThread();
  }

  @Subscribe
  public void configurationStarted(ConfigurationPhaseStartedEvent event) {
    maybeAddDate();
    stateTracker.configurationStarted(event);
    // As a new phase started, inform immediately.
    ignoreRefreshLimitOnce();
    refresh();
    startUpdateThread();
  }

  @Subscribe
  public void loadingComplete(LoadingPhaseCompleteEvent event) {
    stateTracker.loadingComplete(event);
    refresh();
  }

  @Subscribe
  public synchronized void analysisComplete(AnalysisPhaseCompleteEvent event) {
    String analysisSummary = stateTracker.analysisComplete();
    handle(Event.info(null, analysisSummary));
  }

  @Subscribe
  public void progressReceiverAvailable(ExecutionProgressReceiverAvailableEvent event) {
    stateTracker.progressReceiverAvailable(event);
    // As this is the first time we have a progress message, update immediately.
    ignoreRefreshLimitOnce();
    startUpdateThread();
  }

  @Subscribe
  public void buildComplete(BuildCompleteEvent event) {
    // The final progress bar will flow into the scroll-back buffer, to if treat
    // it as an event and add a timestamp, if events are supposed to have a timestamp.
    boolean done = false;
    synchronized (this) {
      stateTracker.buildComplete(event);
      ignoreRefreshLimitOnce();
      refresh();

      // After a build has completed, only stop updating the UI if there is no more BEP
      // upload happening.
      if (stateTracker.pendingTransports() == 0) {
        buildRunning = false;
        done = true;
      }
    }
    if (done) {
      stopUpdateThread();
      flushStdOutStdErrBuffers();
    }
  }

  private void completeBuild() {
    synchronized (this) {
      if (!buildRunning) {
        return;
      }
      buildRunning = false;
    }
    stopUpdateThread();
    synchronized (this) {
      try {
        // If a progress bar is currently present, clean it and redraw it.
        boolean progressBarPresent = numLinesProgressBar > 0;
        if (progressBarPresent) {
          clearProgressBar();
        }
        terminal.flush();
        boolean incompleteLine = flushStdOutStdErrBuffers();
        if (incompleteLine) {
          crlf();
        }
        if (progressBarPresent) {
          addProgressBar();
        }
        terminal.flush();
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("IO Error writing to output stream");
      }
    }
  }

  @Subscribe
  public void packageLocatorCreated(PathPackageLocator packageLocator) {
    locationPrinter.packageLocatorCreated(packageLocator);
  }

  @Subscribe
  public void noBuild(NoBuildEvent event) {
    if (event.showProgress()) {
      synchronized (this) {
        buildRunning = true;
      }
      return;
    }
    completeBuild();
  }

  @Subscribe
  public void noBuildFinished(NoBuildRequestFinishedEvent event) {
    completeBuild();
  }

  @Subscribe
  public void afterCommand(AfterCommandEvent event) {
    synchronized (this) {
      buildRunning = true;
    }
    completeBuild();
    try {
      terminal.resetTerminal();
      terminal.flush();
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("IO Error writing to user terminal");
    }
  }

  @Subscribe
  public void downloadProgress(FetchProgress event) {
    maybeAddDate();
    stateTracker.downloadProgress(event);
    refresh();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void actionStarted(ActionStartedEvent event) {
    stateTracker.actionStarted(event);
    refresh();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void scanningAction(ScanningActionEvent event) {
    stateTracker.scanningAction(event);
    refresh();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void stopScanningAction(StoppedScanningActionEvent event) {
    stateTracker.stopScanningAction(event);
    refresh();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void schedulingAction(SchedulingActionEvent event) {
    stateTracker.schedulingAction(event);
    refresh();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void runningAction(RunningActionEvent event) {
    stateTracker.runningAction(event);
    refresh();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void actionCompletion(ActionScanningCompletedEvent event) {
    stateTracker.actionCompletion(event);
    refreshSoon();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void actionCompletion(ActionCompletionEvent event) {
    stateTracker.actionCompletion(event);
    refreshSoon();
  }

  @Subscribe
  public void testFilteringComplete(TestFilteringCompleteEvent event) {
    stateTracker.testFilteringComplete(event);
    refresh();
  }

  /**
   * Return true, if the test summary provides information that is both worth being shown in the
   * scroll-back buffer and new with respect to the alreay shown failure messages.
   */
  private boolean testSummaryProvidesNewInformation(TestSummary summary) {
    ImmutableSet<BlazeTestStatus> statusToIgnore =
        ImmutableSet.of(
            BlazeTestStatus.PASSED,
            BlazeTestStatus.FAILED_TO_BUILD,
            BlazeTestStatus.BLAZE_HALTED_BEFORE_TESTING,
            BlazeTestStatus.NO_STATUS);

    if (statusToIgnore.contains(summary.getStatus())) {
      return false;
    }
    if (summary.getStatus() == BlazeTestStatus.FAILED && summary.getFailedLogs().size() == 1) {
      return false;
    }
    return true;
  }

  @Subscribe
  public synchronized void testSummary(TestSummary summary) {
    stateTracker.testSummary(summary);
    if (testSummaryProvidesNewInformation(summary)) {
      // For failed test, write the failure to the scroll-back buffer immediately
      try {
        clearProgressBar();
        crlf();
        setEventKindColor(
            summary.getStatus() == BlazeTestStatus.FLAKY ? EventKind.WARNING : EventKind.ERROR);
        terminal.writeString("" + summary.getStatus() + ": ");
        terminal.resetTerminal();
        terminal.writeString(summary.getLabel().toString());
        terminal.writeString(" (Summary)");
        crlf();
        for (Path logPath : summary.getFailedLogs()) {
          terminal.writeString("      " + logPath.getPathString());
          crlf();
        }
        if (showProgress && cursorControl) {
          addProgressBar();
        }
        terminal.flush();
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("IO Error writing to output stream");
      }
    } else {
      refresh();
    }
  }

  @Subscribe
  public synchronized void buildEventTransportsAnnounced(AnnounceBuildEventTransportsEvent event) {
    stateTracker.buildEventTransportsAnnounced(event);
    if (debugAllEvents) {
      String message = "Transports announced:";
      for (BuildEventTransport transport : event.transports()) {
        message += " " + transport.name();
      }
      this.handle(Event.info(null, message));
    }
  }

  @Subscribe
  public void buildEventTransportClosed(BuildEventTransportClosedEvent event) {
    stateTracker.buildEventTransportClosed(event);
    if (debugAllEvents) {
      this.handle(Event.info(null, "Transport " + event.transport().name() + " closed"));
    }

    if (stateTracker.pendingTransports() == 0) {
      stopUpdateThread();
      flushStdOutStdErrBuffers();
      ignoreRefreshLimitOnce();
      refresh();
    } else {
      refresh();
    }
  }

  private void refresh() {
    if (showProgress) {
      progressBarNeedsRefresh = true;
      doRefresh();
    }
  }

  private void doRefresh(boolean fromUpdateThread) {
    if (!buildRunning) {
      return;
    }
    long nowMillis = clock.currentTimeMillis();
    if (lastRefreshMillis + minimalDelayMillis < nowMillis) {
      if (updateLock.tryLock()) {
        try {
          synchronized (this) {
            if (showProgress && (progressBarNeedsRefresh || timeBasedRefresh())) {
              progressBarNeedsRefresh = false;
              clearProgressBar();
              addProgressBar();
              terminal.flush();
              if (!cursorControl) {
                // If we can't update the progress bar in place, make sure we increase the update
                // interval as time progresses, to avoid too many progress messages in place.
                minimalDelayMillis =
                    Math.max(
                        minimalDelayMillis,
                        Math.round(
                            NO_CURSES_MINIMAL_RELATIVE_PROGRESS_RATE_LMIT
                                * (clock.currentTimeMillis() - uiStartTimeMillis)));
                minimalUpdateInterval = Math.max(minimalDelayMillis, MAXIMAL_UPDATE_DELAY_MILLIS);
              }
            }
          }
        } catch (IOException e) {
          logger.atWarning().withCause(e).log("IO Error writing to output stream");
        } finally {
          updateLock.unlock();
        }
      }
    } else {
      // We skipped an update due to rate limiting. If this however, turned
      // out to be the last update for a long while, we need to show it in a
      // timely manner, as it best describes the current state.
      if (!fromUpdateThread) {
        startUpdateThread();
      }
    }
  }

  private void doRefresh() {
    doRefresh(false);
  }

  private void refreshSoon() {
    // Schedule an update of the progress bar in the near future, unless there is already
    // a future update scheduled.
    long nowMillis = clock.currentTimeMillis();
    if (mustRefreshAfterMillis <= lastRefreshMillis) {
      mustRefreshAfterMillis = Math.max(nowMillis + minimalUpdateInterval, lastRefreshMillis + 1);
    }
    startUpdateThread();
  }

  /** Decide whether the progress bar should be redrawn only for the reason that time has passed. */
  private synchronized boolean timeBasedRefresh() {
    if (!stateTracker.progressBarTimeDependent()) {
      return false;
    }
    long nowMillis = clock.currentTimeMillis();
    long intervalMillis = cursorControl ? SHORT_REFRESH_MILLIS : LONG_REFRESH_MILLIS;
    if (lastRefreshMillis < mustRefreshAfterMillis
        && mustRefreshAfterMillis < nowMillis + minimalDelayMillis) {
      // Within the a smal interval from now, an update is scheduled anyway,
      // so don't do a time-based update of the progress bar now, to avoid
      // updates too close to each other.
      return false;
    }
    return lastRefreshMillis + intervalMillis < nowMillis;
  }

  private void ignoreRefreshLimitOnce() {
    // Set refresh time variables in a state such that the next progress bar
    // update will definitely be written out.
    lastRefreshMillis = clock.currentTimeMillis() - minimalDelayMillis - 1;
  }

  private void startUpdateThread() {
    // Refuse to start an update thread once the build is complete; such a situation might
    // arise if the completion of the build is reported (shortly) before the completion of
    // the last action is reported.
    if (buildRunning && updateThread.get() == null) {
      final UiEventHandler eventHandler = this;
      Thread threadToStart =
          new Thread(
              () -> {
                try {
                  while (!shutdown) {
                    Thread.sleep(minimalUpdateInterval);
                    if (lastRefreshMillis < mustRefreshAfterMillis
                        && mustRefreshAfterMillis < clock.currentTimeMillis()) {
                      progressBarNeedsRefresh = true;
                    }
                    eventHandler.doRefresh(/* fromUpdateThread= */ true);
                  }
                } catch (InterruptedException e) {
                  // Ignore
                }
              },
              "cli-update-thread");
      if (updateThread.compareAndSet(null, threadToStart)) {
        threadToStart.start();
      }
    }
  }

  /**
   * Stop the update thread and wait for it to terminate. As the update thread, which is a separate
   * thread, might have to call a synchronized method between being interrupted and terminating, DO
   * NOT CALL from a SYNCHRONIZED block, as this will give the opportunity for dead locks.
   */
  private void stopUpdateThread() {
    shutdown = true;
    Thread threadToWaitFor = updateThread.getAndSet(null);
    if (threadToWaitFor != null) {
      threadToWaitFor.interrupt();
      Uninterruptibles.joinUninterruptibly(threadToWaitFor);
    }
  }

  private void clearProgressBar() throws IOException {
    if (!cursorControl) {
      return;
    }
    for (int i = 0; i < numLinesProgressBar; i++) {
      terminal.cr();
      terminal.cursorUp(1);
      terminal.clearLine();
    }
    numLinesProgressBar = 0;
  }

  /** Terminate the line in the way appropriate for the operating system. */
  private void crlf() throws IOException {
    terminal.writeString(System.lineSeparator());
  }

  private synchronized void addProgressBar() throws IOException {
    LineCountingAnsiTerminalWriter countingTerminalWriter =
        new LineCountingAnsiTerminalWriter(terminal);
    AnsiTerminalWriter terminalWriter = countingTerminalWriter;
    lastRefreshMillis = clock.currentTimeMillis();
    if (cursorControl) {
      terminalWriter = new LineWrappingAnsiTerminalWriter(terminalWriter, terminalWidth - 1);
    }
    String timestamp = null;
    if (showTimestamp) {
      timestamp =
          TIMESTAMP_FORMAT.format(
              Instant.ofEpochMilli(clock.currentTimeMillis()).atZone(ZoneId.systemDefault()));
    }
    stateTracker.writeProgressBar(terminalWriter, /* shortVersion=*/ !cursorControl, timestamp);
    terminalWriter.newline();
    numLinesProgressBar = countingTerminalWriter.getWrittenLines();
    if (progressInTermTitle) {
      LoggingTerminalWriter stringWriter = new LoggingTerminalWriter(true);
      stateTracker.writeProgressBar(stringWriter, true);
      terminal.setTitle(stringWriter.getTranscript());
    }
  }
}
