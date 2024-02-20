package com.verygood.security.larky.modules.vgs.metrics.defaults;

import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import lombok.extern.slf4j.Slf4j;
import net.starlark.java.eval.Dict;

@Slf4j
public class DefaultMetrics implements LarkyMetrics {

  /**
   * Not used in production
   */
  @Override
  public void track(Dict<String, String> dict) {
    System.out.println(dict);
  }
}
