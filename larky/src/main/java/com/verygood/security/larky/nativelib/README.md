# Larky Native Lib

This is the Java native library that is publicly exposed for the `Larky` embedded language.

All libraries here are public and form the basis of the built-ins part of `Larky` that is specific to VGS.

## Python Compatibility

In order to ensure that Larky is compatible with Python (besides the obvious `load()` vs `import` differences), we try to emulate Python's stdlib interface as much as possible. 

As a result, globals should not be accessed directly. Instead, access Larky native functions and methods via the [`Larky` stdlib namespace](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/resources/stdlib/larky.star). Again, Do not access these libraries directly, but access them through Larky StdLib via the [`larky` namespace](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/resources/stdlib/larky.star). 

