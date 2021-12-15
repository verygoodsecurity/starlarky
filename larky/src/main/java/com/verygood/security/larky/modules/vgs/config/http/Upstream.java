package com.verygood.security.larky.modules.vgs.config.http;

import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import java.util.HashMap;
import lombok.Getter;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;


@StarlarkBuiltin(
    name = "Upstream",
    category = "BUILTIN",
    doc = "The type represents HTTP Upstream")
public class Upstream extends SimpleStruct {

  private final HTTPConfig config;

  @Getter
  private final String host;
  @Getter
  private final Integer port;

  public Upstream(HTTPConfig config, String host, StarlarkInt port) {
    super(new HashMap<String, Object>() {{
      put("host", host);
      put("port", port);
    }}, null);

    this.config = config;
    this.host = host;
    this.port = port.toIntUnchecked();
  }

  @StarlarkMethod(
      name = "on_request",
      parameters = {
          @Param(
              name = "handler",
              allowedTypes = {
                  @ParamType(type = StarlarkCallable.class)
              }),
          @Param(
              name = "path",
              defaultValue = "None",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class)
              }),
          @Param(
              name = "method",
              defaultValue = "None",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class)
              })
      })
  public void onRequest(StarlarkCallable handler, Object path, Object method) {
    config.onRequest(this, handler, getValue(path), getValue(method));
  }

  @StarlarkMethod(
      name = "on_response",
      parameters = {
          @Param(
              name = "handler",
              allowedTypes = {
                  @ParamType(type = StarlarkCallable.class)
              }),
          @Param(
              name = "path",
              defaultValue = "None",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class)
              }),
          @Param(
              name = "method",
              defaultValue = "None",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class)
              })
      })
  public void onResponse(StarlarkCallable handler, Object path, Object method) {
    config.onResponse(this, handler, getValue(path), getValue(method));
  }

  private String getValue(Object value) {
    if (value instanceof String) {
      return (String) value;
    }

    return null;
  }
}
