package com.verygood.security.larky.modules.vgs.config.http;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.ServiceLoader;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;


public class HTTPConfigModule implements StarlarkValue {

  public static final HTTPConfigModule INSTANCE = new HTTPConfigModule();

  private final HTTPConfig config;

  public HTTPConfigModule() {
    ServiceLoader<HTTPConfig> loader = ServiceLoader.load(HTTPConfig.class);
    List<HTTPConfig> providers = ImmutableList.copyOf(loader.iterator());

    if (providers.size() > 1) {
      throw new IllegalArgumentException(String.format(
          "HTTPConfigModule expecting only 1 http provider of type HTTPConfig, found %d",
          providers.size()
      ));
    }

    config = providers.stream()
        .findFirst()
        .orElse(new NoopHTTPConfig());
  }

  @StarlarkMethod(
      name = "inbound",
      doc = "defines the inbound route",
      parameters = {
          @Param(
              name = "host",
              doc = "upstream host",
              allowedTypes = {
                  @ParamType(type = String.class)
              }),
          @Param(
              name = "port",
              doc = "upstream port",
              defaultValue = "80",
              allowedTypes = {
                  @ParamType(type = StarlarkInt.class)
              })
      })
  public Object inbound(String host, StarlarkInt port) {
    Upstream upstream = new Upstream(config, host, port);
    config.inbound(upstream);

    return upstream;
  }

  @StarlarkMethod(
      name = "outbound",
      doc = "defines an outbound route",
      parameters = {
          @Param(
              name = "host",
              doc = "upstream host",
              allowedTypes = {
                  @ParamType(type = String.class)
              }),
          @Param(
              name = "port",
              doc = "upstream port",
              defaultValue = "80",
              allowedTypes = {
                  @ParamType(type = StarlarkInt.class)
              })
      })
  public Object outbound(String host, StarlarkInt port) {
    Upstream upstream = new Upstream(config, host, port);
    config.outbound(upstream);

    return upstream;
  }

}
