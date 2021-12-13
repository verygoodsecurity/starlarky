package com.verygood.security.larky.modules.vgs.http;


import net.starlark.java.eval.StarlarkCallable;

public class NoopRouter implements Router {

    @Override
    public void inbound(Upstream upstream) {}

    @Override
    public void outbound(Upstream upstream) {}

    @Override
    public void onRequest(Upstream upstream, StarlarkCallable handler, String path, String method) {}

    @Override
    public void onResponse(Upstream upstream, StarlarkCallable handler, String path, String method) {}

}
