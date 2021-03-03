# Larky Library of Native Module

This is the Java native library that is publicly exposed for the `Larky` embedded language.

All libraries here are public and form the basis of the built-ins part of `Larky` that is specific to VGS.

## GraalVM Support

NOTE: This library supports builds via GraalVM, in the [`runlarky`](https://github.com/verygoodsecurity/starlarky/blob/master/runlarky) folder. 
If you add a new module or change the layout of the module here, you must take care in also updating the [`reflect-config.json`](https://github.com/verygoodsecurity/starlarky/blob/master/runlarky/src/main/resources/reflect-config.json) file
for building a native image. 

We can probably find or build a maven plugin to automatically do this as per
[Simplifying native-image generation with Maven plugin and embeddable configuration](https://medium.com/graalvm/simplifying-native-image-generation-with-maven-plugin-and-embeddable-configuration-d5b283b92f57)
instead of having to do this manually. 

## Python Compatibility

In order to ensure that Larky is compatible with Python Simplifying native-image generation with Maven plugin and embeddable configuration(besides the obvious `load()` vs `import` differences), we try to emulate Python's stdlib interface as much as possible. 

As a result, globals should not be accessed directly. Instead, access Larky native functions and methods via the [`Larky` stdlib namespace](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/resources/stdlib/larky.star). Again, Do not access these libraries directly, but access them through Larky StdLib via the [`larky` namespace](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/resources/stdlib/larky.star). 

### How does one emulate a while loop?
```python
    while pos <= finish:
       # do stuff
```

emulate it by:

```python
    for _while_ in range(1000):  # "while pos <= finish" is the same as:
        if pos > finish:         # for _while_ in range(xxx):
            break                # if pos > finish: break
```

Obviously, range can take a larger number to emulate infinity.

### Native Module

Source files for standard library _extension_ modules.

These are *NOT* built-in modules, but are basically extension wrappers that help
implement the standard library. 