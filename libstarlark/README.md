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

#### Surprising Differences

- No `**` operator. Instead, use `math.pow()`

## Extending Starlark

The easiest way to learn about extending Starlark with custom built-ins is to look into the tests directory. 
Start by examining [`Examples`](https://github.com/verygoodsecurity/starlarky/tree/master/libstarlark/src/test/java/net/starlark/java/eval/Examples.java):

```java
/** This function shows how to construct a callable Starlark value from a Java method. */
  ImmutableMap<String, Object> makeEnvironment() {
    ImmutableMap.Builder<String, Object> env = ImmutableMap.builder();
    env.put("zero", 0);
    Starlark.addMethods(env, new MyFunctions(), StarlarkSemantics.DEFAULT); // adds 'square'
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

1. Go to Project Structure
1. Under `Project Settings`, go to `Modules`
1. Find `libstarlark` and click on the `Paths` tab
1. Under `Compiler Output`, select `Use module compile output path` 
1. Replace the `target/classes` in output path to `src/main/java` 
1. Replace the `target/test-classes` in test output path  to `src/test/java`

You can now run the tests from IntelliJ. 
