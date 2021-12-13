package com.verygood.security.larky.modules.vgs.http;

import net.starlark.java.eval.StarlarkCallable;


public interface Router {
    void inbound(Upstream upstream);
    void outbound(Upstream upstream);
    void onRequest(Upstream upstream, StarlarkCallable handler, String path, String method);
    void onResponse(Upstream upstream, StarlarkCallable handler, String path, String method);
}
