package com.verygood.security.larky;

import static com.verygood.security.larky.ModuleSupplier.CORE_MODULES;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.nativelib.PythonBuiltins;
import com.verygood.security.larky.nativelib.test.UnittestModule;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.PathBasedStarFile;
import com.verygood.security.larky.parser.StarFile;
import com.verygood.security.larky.py.LarkyType;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.ParserInput;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

public class LarkyTest {

  @Test
  public void testStarlarkExampleFile() {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_starlark_executes_example.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    System.out.println(absolutePath);

    try {
      Assert.assertEquals(
          "Did not successfully evaluate Starlark file",
          0,
          Larky.execute(ParserInput.readFile(absolutePath))
      );
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.err.println(e.getCause().toString());
      Throwable t = e.getCause();
      Assert.fail(t.toString());
    }
  }

  @Test
  public void testStdLib() throws IOException {
    Path stdlibTestDir = Paths.get(
            "src",
            "test",
            "resources",
            "stdlib_tests");
    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);

    ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier(ImmutableSet.of(
      new UnittestModule()
    )).create();

    TestingConsole console = new TestingConsole();
    try (Stream<Path> paths = Files.walk(stdlibTestDir)) {
      paths
        .filter(Files::isRegularFile)
        //.filter(f -> f.getFileName().startsWith("test_") && f.endsWith(".star"))
        .filter(f -> {
          String fileName = f.getFileName().toString();
          return fileName.startsWith("test_") && fileName.endsWith(".star");
        })
        .forEach(f -> {
          try {
            console.info("Running test: " + f);
            interpreter.evaluate(
              new PathBasedStarFile(f.toAbsolutePath(), null, null),
              moduleSet,
              console
            );
            console.info("Successfully executed: " + f);
          } catch (IOException e) {
            Assert.fail(e.getMessage());
          }
        });
    }
  }

  @Library
  public static class Foo {
    /**
     * Shadow the built-in type() for python compatibility
     */
    @StarlarkMethod(
          name = "type",
          doc =
              "type(object_or_name, bases, dict)"
            + "type(object) -> the object's type"
            + "type(name, bases, dict) -> a new type"
                  + "Returns the type name of its argument. This is useful for debugging and "
                  + "type-checking. Examples:"
                  + "<pre class=\"language-python\">"
                  + "type(2) == \"int\"\n"
                  + "type([1]) == \"list\"\n"
                  + "type(struct(a = 2)) == \"struct\""
                  + "</pre>"
                  + "This function might change in the future. To write Python-compatible code and "
                  + "be future-proof, use it only to compare return values: "
                  + "<pre class=\"language-python\">"
                  + "if type(x) == type([]):  # if x is a list"
                  + "</pre>",
        parameters = {
            @Param(name = "name", doc = "The object to check type of."),
            @Param(name = "bases", defaultValue = "None"),
            @Param(name = "dict", defaultValue = "None")
        },
        extraKeywords = @Param(name = "kwargs", defaultValue = "{}"),
        useStarlarkThread = true
    )
    //public Object type(Object object, Tuple bases, Dict<String, Object> dict, Dict<String, Object> kwargs, StarlarkThread thread) throws EvalException {
    public Object type(Object object, Object bases, Object dict, Dict<String, Object> kwargs, StarlarkThread thread) throws EvalException {
      //type(object_or_name, bases, dict)
      //type(object) -> the object's type
      //type(name, bases, dict) -> a new type

      if(Starlark.isNullOrNone(bases) && Starlark.isNullOrNone(dict) && kwargs.size() == 0) {
        // There is no 'type' type in Starlark, so we return a string with the type name.
        return Starlark.type(object);
      }
      else if (kwargs.size() != 0) {
        throw new EvalException("type() takes 1 or 3 arguments");
      }
      else {
        Tuple bazes = (Tuple) bases;
        // TODO(mahmoudimus): support bases
        if(bazes.size() != 0) {
          System.err.println(bases);
          throw new EvalException("Bases is not supported for now.");
        }

        return LarkyType.createExported(
            new LarkyType.Key("BUILTIN", String.valueOf(object)),
            thread.getCallerLocation(),
            bazes,
            Dict.cast(dict, String.class, Object.class, "dict"));
      }
    }
    @StarlarkMethod(
          name = "_type",
          doc =
              "Creates a declared provider 'constructor'. The return value of this "
                  + "function can be used to create \"struct-like\" values. Example:<br>"
                  + "<pre class=\"language-python\">data = provider()\n"
                  + "d = data(x = 2, y = 3)\n"
                  + "print(d.x + d.y) # prints 5</pre>"
                  + "<p>See <a href='../rules.$DOC_EXT#providers'>Rules (Providers)</a> for a "
                  + "comprehensive guide on how to use providers.",
          parameters = {
            @Param(
                name = "name",
                named = true,
                defaultValue = "''",
                doc =
                    "A description of the provider that can be extracted by documentation generating"
                        + " tools."),
            @Param(
                name = "fields",
                doc =
                    "If specified, restricts the set of allowed fields. <br>Possible values are:<ul> "
                        + " <li> list of fields:<br>       <pre"
                        + " class=\"language-python\">provider(fields = ['a', 'b'])</pre><p>  <li>"
                        + " dictionary field name -> documentation:<br>       <pre"
                        + " class=\"language-python\">provider(\n"
                        + "       fields = { 'a' : 'Documentation for a', 'b' : 'Documentation for b'"
                        + " })</pre></ul>All fields are optional.",
                allowedTypes = {
                  @ParamType(type = Sequence.class, generic1 = String.class),
                  @ParamType(type = Dict.class),
                  @ParamType(type = NoneType.class),
                },
                named = true,
                positional = false,
                defaultValue = "None")
          },
          useStarlarkThread = true)
    public LarkyType _type(String name, Object fields, StarlarkThread thread) throws EvalException {
      System.out.println(">>>> ------ " + String.valueOf(fields));
      return LarkyType.createExported(
          new LarkyType.Key("BUILTIN", name),
          thread.getCallerLocation(),
          null,
          null);
    }

    public LarkyType __new__(String name, Object fields, StarlarkThread thread) throws EvalException {
      Collection<String> fieldNames =
          fields instanceof Sequence
              ? Sequence.cast(fields, String.class, "fields")
              : fields instanceof Dict
              ? Dict.cast(fields, String.class, StarlarkValue.class, "fields").keySet()
              : null;

      if(!Strings.isNullOrEmpty(name)) {
         return LarkyType.createExported(
             new LarkyType.Key("BUILTIN", name),
             thread.getCallerLocation(),
             null,
             null
         );
      }
      return LarkyType.createUnexported(
          thread.getCallerLocation(),
          null,
          null);
    }
  }

  @Test
  public void testTypes() throws IOException {
    Path resourceDirectory = Paths.get(
           "src",
           "test",
           "resources",
           "test_types.star");
       String absolutePath = resourceDirectory.toFile().getAbsolutePath();
       System.out.println(absolutePath);

       LarkyScript interpreter = new LarkyScript(
           ImmutableSet.<Class<?>>builder()
               .addAll(CORE_MODULES)
               .add(Foo.class)
               .build(),
           LarkyScript.StarlarkMode.STRICT);
       StarFile starFile = new PathBasedStarFile(
           Paths.get(absolutePath),
           null,
           null);
       ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier(ImmutableSet.of(
           new UnittestModule()
           )).create();
       interpreter.evaluate(starFile, moduleSet, new TestingConsole());
  }

  @Test
  public void testStructBuiltin() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_struct.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier().create();
    config = interpreter.evaluate(starFile, moduleSet, new TestingConsole());
  }

  @Test
  public void testLoadingModules() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_loading_module.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @Test
  public void testUnitTestModule() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_unittest.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyScript interpreter = new LarkyScript(
        ImmutableSet.of(
            PythonBuiltins.class,
            LarkyGlobals.class,
            UnittestModule.class
        ),
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @Test
  public void testFCOAlternative() throws IOException, URISyntaxException {

    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_fco_operation.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);

    StarFile starFile = new PathBasedStarFile(
            Paths.get(absolutePath),
            null,
            null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier(ImmutableSet.of(
        new UnittestModule()
    )).create(), new TestingConsole());
  }
}
