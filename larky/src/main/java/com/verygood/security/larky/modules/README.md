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

Here's another way to automate this (mostly following instructions from [GraalVM: Working with Native Image Efficiently](https://medium.com/graalvm/working-with-native-image-efficiently-c512ccdcd61b))

```bash
# make sure your JAVA_HOME is set.
git clone https://github.com/graalvm/mx.git
ln -s $(pwd)/mx/mx /usr/local/bin/mx
git clone https://github.com/graalvm/labs-openjdk-11.git
pushd labs-openjdk-11 
python build_labsjdk.py --boot-jdk ${JAVA_HOME} --configure-option=--disable-warnings-as-errors
# copy the java_home symlink path - referencing this going forward as ${labs-openjdk-11/Home}
popd
git clone https://github.com/oracle/graal.git
pushd graal/compiler
mx --java-home=${labs-openjdk-11/Home} build
```

Once it's all built...that's when the fun starts:

then run the tests to instrument the dynamic calls:

```bash
java -agentpath:${GRAAL_VM_PATH}/libnative-image-agent.dylib=trace-output=${TRACE_PATH}/trace.json
```

Then from 

```bash
pushd graal/substratevm
env VERBOSE_GRAALVM_LAUNCHERS=true JAVA_HOME=${labs-openjdk-11/Home} \
  ./svmbuild/vm/bin/native-image-configure \
   --vm.-upgrade-module-path="$(pwd)/../compiler/mxbuild/dists/jdk11/graal.jar" \
   --vm.-add-modules="org.graalvm.truffle,org.graalvm.sdk,jdk.internal.vm.compiler" \
   --vm.-module-path="$(pwd)/../truffle/mxbuild/dists/jdk11/truffle-api.jar:$(pwd)/../sdk/mxbuild/dists/jdk11/graal-sdk.jar" \
   --vm.-add-exports=jdk.internal.vm.ci/jdk.vm.ci.code=jdk.internal.vm.compiler \
   --vm.-add-exports=jdk.internal.vm.compiler/org.graalvm.compiler.serviceprovider=ALL-UNNAMED \
   generate --trace-input=${TRACE_PATH}/trace.json --output-dir=runlarky/src/main/resources/META-INF/
```

and you'll have a nice json file with all the reflection you need.

### For all the larky modules:

```bash
rg --no-heading "package (com.*);"  --sort 'path' --replace '$1'   > out.csv
```

```python
import csv
import json
template = {
    "allDeclaredFields": True,
    "allDeclaredMethods": True,
    "allPublicMethods": True,
    "allDeclaredConstructors": True,
    "allPublicConstructors": True
  }
d = []
with open('./blah.csv') as f:
    for i in csv.DictReader(f, delimiter=':', fieldnames=['file', 'module']):
        t = template.copy()
        t["name"] = f"{i['module']}.{i['file'].rpartition('/')[-1].replace('.java', '')}"
        d.append(t)

with open('./starlarky/runlarky/src/main/resources/reflect-config.json', 'w+') as fd:
    json.dump(d, fd, indent=4)
```

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