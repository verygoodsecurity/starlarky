# Contributing to Starlarky
***

# Guidelines

Contribute to Starlarky by following the steps below.

### 1. Create an Issue
The first step to fixing a problem or adding a feature is defining it.
Start by creating an [issue](https://github.com/verygoodsecurity/starlarky/issues) 
to describe what you are trying to do and what value it will add.
Provide the test scenarios that need to be satisfied for this issue to be solved.

### 2. Open a Pull Request
Write some code and open a new [PR](https://github.com/verygoodsecurity/starlarky/pulls). 
In the description, 
link the PR to the issue you have created, 
and describe the changes introduced along with the documentation necessary.

Not sure where to start?
We recommend using [this guide](#extending-larky-guide) as a starting point.

### 3. Debug failing CI checks
Once the CI checks are complete, debug any issues that may cause these checks to fail.

### 4. Wait for a review
When your PR is ready, a member of the VGS Starlarky team will be there to help review and merge your changes!

***
# Extending Larky Guide
Before writing any code, please read the [Starlarky README](https://github.com/verygoodsecurity/starlarky/blob/master/README.md)
to understand the structure of this project.

This guide will only focus on extending _Larky_, VGS's Starlark additions. 

## Add a new Module

In this section, we want to add a new Larky (example) module `ex_module` to an existing Larky namespace `stdlib`.

#### 1. Adding a module using Larky (`.star`)
* Larky modules must belong to a namespace.

* The Larky module is defined and implemented in the `ex_module.star` file, 
  and stored in `larky/src/main/resources/stdlib/`.
  ```
  load("@stdlib/larky", "larky")
  
  def _method1(param1,param2):
    return param1 + " " + param2
  
  ex_module = larky.struct(
    method1=_method1,
  )
  ```

#### 2. Adding a module using Java (`.java`)

* More complex modules 
  (e.g., that require external dependencies and / or tooling not found in Starlarky)
  can use Java support.

* The Java module is implemented in the `ExampleModule.java` file, and stored in 
  `larky/src/main/java/com/verygood/security/larky/modules/`

* Java modules
  * (as of now) live in the `@stdlib` namespace by default.
  * are declared using the class annotation `@StarlarkBuiltin`.
    ```
    @StarlarkBuiltin(
        name = "ex_module",
        category = "BUILTIN",
        doc = "Module Description"
    )
    ```
      
  * implement `StarlarkValue`.
    ```
    public class ExampleModule implements StarlarkValue {
    ```

  * have an internal object `INSTANCE` 
    ```
        public static final ExampleModule INSTANCE = new ExampleModule();
    ```
    used by the [ModuleSupplier](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/java/com/verygood/security/larky/ModuleSupplier.java#L53) 
    to invoke module methods at runtime, and should be added to the `STD_MODULES` module map.
    ```
        public static final ImmutableSet<StarlarkValue> STD_MODULES = ImmutableSet.of(
            ExampleModule.INSTANCE,
            ...
        )
    ```
    
  * methods are declared using the method annotation `@StarlarkMethod`.
    ```
      @StarlarkMethod(
          name = "method1",
          doc = "Method Description",
          parameters = {
              @Param(
                  name = "param1",
                  doc = "param1 description",
                  allowedTypes = {
                      @ParamType(type = String.class)
                  }),
              @Param(
                  name = "param2",
                  doc = "param2 description",
                  named = true,
                  defaultValue = "None",
                  allowedTypes = {
                      @ParamType(type=String.class),
                  })
          }
      )
      public String method1(String param1, String param2) {
        return param1 + " " + param2;
      }
    ```

#### 3. Testing the module 
* The module should be tested using Larky script (.star) tests, 
  stored in `larky/src/test/resources/stdlib_tests/test_ex_module.star`.

* Larky tests in the `stdlib` namespace are run from a Java test file, 
  stored in `larky/src/test/java/com/verygood/security/larky/StdLibTests.java`.

* The example module can be imported and used in a Larky `.star` scripts as follows.
  ```
  load("@stdlib//ex_module", "ex_module")
       
  ex_module.method1("param1","param2");
  ```
* Larky supports unit tests and assertions. 
  ```
  load("@vendor//asserts", "asserts")
  load("@stdlib//unittest", "unittest")
  load("@stdlib//ex_module", "ex_module")

  def _test_method1():
    result = ex_module.method1("param1","param2")
    exp_result = "param1 param2"
  
    asserts.assert_that(result).is_equal_to(exp_result)
    
  def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_method1))
    return _suite
  
  _runner = unittest.TextTestRunner()
  _runner.run(_suite())
  ```

#### 4. Reflecting changes in Python `runlarky`
New modules should be reflected in `runlarky`. 
Follow [these instruction](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/java/com/verygood/security/larky/modules/README.md#graalvm-support)
for more information. 


## Add a new Namespace
In this section, we will add a new Larky namespace `ex_namespace`.

1. Create a directory in `larky/src/main/resources/ex_namespace` and place your `.star` module files in there.
2. Add the namespace name `ex_namespace//` to the [ResouceContentStarFile](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/java/com/verygood/security/larky/parser/ResourceContentStarFile.java#L79-L81) 
   `startsWithPrefix` method.
3. Create a directory in `larky/src/test/resources/ex_namespace_tests` and place your `test_*.star` test files in there.
4. Create a Java test file `ExNamespaceLibTests.java`, placed in `larky/src/test/java/com/verygood/security/larky/`, 
   that extracts and executes Larky tests from `larky/src/test/resources/ex_namespace_tests` 
   and runs them using a `LarkyScript` interpreter.
   
   Use the [stdlib test file](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/test/java/com/verygood/security/larky/StdLibTests.java)
   as a reference.
