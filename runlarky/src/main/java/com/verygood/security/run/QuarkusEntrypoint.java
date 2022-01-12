package com.verygood.security.run;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class QuarkusEntrypoint implements QuarkusApplication {

  @Override
  public int run(String... args) throws Exception {
    return LarkyEntrypoint.run(args);
  }
}
