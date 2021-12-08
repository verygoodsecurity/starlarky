# Starlark

Starlark is a scripting language from Google that is mostly a subset of python. This package implements starlark as a 
library to allow embedded usage of Starlark in Very Good Security's Secure Compute environment.

Starlark is used as a _transformation syntax_ and are about as close as one can get to the full power of a programming language for computing on data safely and securely. 
It allows Very Good Security to execute untrusted code, similar to how Serverless-like applications work, and controls language side effects.

### Key Differences from Python

You can read more about the [key differences here](https://docs.bazel.build/versions/master/skylark/language.html#differences-with-python), but in short, the following is a general overview of differences from Python 

- No While Loops
- No Recursion
- Variables are frozen after mutation
- Strings are not iterable

## Differences between `libstarlark` and upstream `bazelbuild` 

For all intents and purposes, any changes to `libstarlark` are kept to an absolute minimum.

This version contains a copy of a `net.starlark.java` from bazel's repository (revision [09c621e4cf5b968f4c6cdf905ab142d5961f9ddc](https://github.com/bazelbuild/bazel/tree/09c621e4cf5b968f4c6cdf905ab142d5961f9ddc)).

The following is a **complete** list of changes applied to upstream.  The list exists here to simplify sync with upstream:

- [5f8103c](https://github.com/verygoodsecurity/starlarky/commit/5f8103c22e40ec33c92d5846ec2849eb481a0e2b): starlark: add 'bytes' data type, for binary strings
- [396e243](https://github.com/verygoodsecurity/starlarky/commit/5f8103c22e40ec33c92d5846ec2849eb481a0e2b): Rename StarlarkByte => StarlarkBytes
- [34abe6b](https://github.com/verygoodsecurity/starlarky/commit/34abe6bd9c00101b690feba7da0a20b0bb80644f): Introduced ByteStringModuleApi
- [d796c6f](https://github.com/verygoodsecurity/starlarky/commit/d796c6f6779b6541fc859ae699f91afa783b355a): Use StarlarkBytes and StarlarkByteArray instead of LarkyByte and LarkyByteArray
- [1716e47](https://github.com/verygoodsecurity/starlarky/pull/149/commits/1716e47a1f756516834da971d31e849982ca9fe0): StarlarkBytes - General cleanup  

## Extending Starlark

The easiest way to learn about extending Starlark with custom built-ins is to look into the tests directory. 
Start by examining [`Examples`](https://github.com/verygoodsecurity/starlarky/tree/master/libstarlark/src/test/java/net/starlark/java/eval/Examples.java):

### `py2star`

We have [developed a translating compiler](https://github.com/mahmoudimus/py2star) (i.e. "transpiler") that can be used to migrate python libraries automatically to compensate  for the differences between Python and Starlark. This should help migrate many external third-party python packages without too much effort.

py2star [has a roadmap](https://github.com/mahmoudimus/py2star/blob/main/ROADMAP.md) which includes a list of either completed or to-be-done tranforms.

More on py2star here: https://github.com/mahmoudimus/py2star
More on migrating python to starlark/larky FAQs here: https://github.com/mahmoudimus/py2star/blob/main/MIGRATING.md

### Extending Starlark from Java

```java
/** This function shows how to construct a callable Starlark value from a Java method. */
import net.starlark.java.eval.Starlark;

ImmutableMap<String, Object> makeEnvironment(){
  ImmutableMap.Builder<String, Object> env=ImmutableMap.builder();
  env.put("zero",0);
  Starlark.addMethods(env,new MyFunctions(),StarlarkSemantics.DEFAULT); // adds 'square'
  return env.build();
  }

/**
 * The annotated methods of this class are added to the environment by {@link
 * Starlark#addMethods}.
 */
static final class MyFunctions {
  @StarlarkMethod(
    name = "square",
    parameters = {@Param(name = "x", type = int.class)},
    doc = "Returns the square of its integer argument.")
  public int square(int x) {
    return x * x;
  }
}
```

Then, you can:

```python
if square(2) == 4:
    print("square function works!") 
```

### Language Lawyers

If you want to read more about the specification, please visit the [official Starlark specification](https://github.com/bazelbuild/starlark/blob/master/spec.md)

### Contributing

#### IntelliJ Configuration

1. Go to Project Structure.
1. Under `Project Settings`, go to `Modules`.
1. Find `libstarlark` and click on the `Paths` tab.
1. Under `Compiler Output`, select `Use module compile output path`.
1. ~~Replace the `target/classes` in output path to `src/main/java`.~~
1. ~~Replace the `target/test-classes` in test output path  to `src/test/java`.~~

You can now run the tests from IntelliJ.