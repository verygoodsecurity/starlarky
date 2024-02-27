package com.verygood.security.larky.modules.vgs.metrics;

import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import lombok.extern.slf4j.Slf4j;
import net.starlark.java.eval.Dict;

@Slf4j
public class DefaultMetrics implements LarkyMetrics {

  /**
   * Not used in production
   */
  @Override
  public void track(Dict<String, Object> dict) {
    System.out.println(dict);
  }
}
