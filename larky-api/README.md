# Larky Versioned Engine API

## How to use the API

### 1) Include it as a dependency in your project.

#### Maven:
```
<dependency>
  <groupId>com.verygood.security</groupId>
  <artifactId>larky-api</artifactId>
  <version>x.x.x</version>
  <type>pom</type>
</dependency>
```
Note that if `com.verygood.security:larky` is already in your classpath, 
this may impact the usability of this API. 

### 2) Write some code.

Try this Hello World example.

```
String script = 
            "def hello_world():\n" +
            "    return \"Hello From Larky!\"\n" +
            "output = hello_world()";
    
LarkyAPIEngine engine = new LarkyAPIEngine();
String result = engine.executeScript(script, "output").toString();

// result == "Hello From Larky!"
```