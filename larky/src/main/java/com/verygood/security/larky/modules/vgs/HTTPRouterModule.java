package com.verygood.security.larky.modules.vgs;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.http.NoopRouter;
import com.verygood.security.larky.modules.vgs.http.Router;
import com.verygood.security.larky.modules.vgs.http.Upstream;
import java.util.List;
import java.util.ServiceLoader;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "http",
    category = "BUILTIN",
    doc = "Provides configuration for HTTP proxy")
public class HTTPRouterModule implements StarlarkValue {

  public static final HTTPRouterModule INSTANCE = new HTTPRouterModule();

  private final Router router;

  public HTTPRouterModule() {
    ServiceLoader<Router> loader = ServiceLoader.load(Router.class);
    List<Router> providers = ImmutableList.copyOf(loader.iterator());

    if (providers.size() > 1) {
      throw new IllegalArgumentException(String.format(
          "HTTPRouterModule expecting only 1 http provider of type Router, found %d",
          providers.size()
      ));
    }

    router = providers.stream()
        .findFirst()
        .orElse(new NoopRouter());
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
    Upstream upstream = new Upstream(router, host, port);
    router.inbound(upstream);

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
    Upstream upstream = new Upstream(router, host, port);
    router.outbound(upstream);

    return upstream;
  }

}
