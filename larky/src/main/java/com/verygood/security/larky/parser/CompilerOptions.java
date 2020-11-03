package com.verygood.security.larky.parser;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class CompilerOptions extends OptionsBase {

  @Option(
      name = "script",
      abbrev = 's',
      help = "Starlark script",
      category = "startup",
      defaultValue = ""
  )
  public String script;

  @Option(
      name = "input",
      abbrev = 'i',
      help = "Input parameters",
      category = "startup",
      defaultValue = ""
  )
  public String input;

  @Option(
      name = "output",
      abbrev = 'o',
      help = "Output parameters",
      category = "startup",
      defaultValue = ""
  )
  public String output;

  @Option(
      name = "output",
      abbrev = 'l',
      help = "Log output",
      category = "startup",
      defaultValue = ""
  )
  public String log;
}
