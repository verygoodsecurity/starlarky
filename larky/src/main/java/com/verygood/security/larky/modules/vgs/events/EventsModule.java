package com.verygood.security.larky.modules.vgs.events;

import java.util.Map;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
    name = "native_events",
    category = "BUILTIN",
    doc = "Native events recording API for guarded routes")
public class EventsModule implements StarlarkValue {

  public static final EventsModule INSTANCE = new EventsModule();

  @StarlarkMethod(
      name = "record",
      documented = false,
      parameters = {
        @Param(name = "name"),
        @Param(name = "fields", defaultValue = "None"),
      },
      useStarlarkThread = true)
  public Object record(Object name, Object fields, StarlarkThread thread) throws EvalException {
    RecordedEvents recorded = thread.getThreadLocal(RecordedEvents.class);
    if (recorded == null) {
      return Starlark.NONE;
    }
    recorded.recordStarlark(name, fields);
    return Starlark.NONE;
  }
}
