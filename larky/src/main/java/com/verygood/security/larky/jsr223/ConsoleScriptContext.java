package com.verygood.security.larky.jsr223;

import com.verygood.security.larky.console.CapturingConsole;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.Message;
import com.verygood.security.larky.console.StreamWriterConsole;
import lombok.Getter;

import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

@Getter
public class ConsoleScriptContext extends SimpleScriptContext {

    private Console console;

    public ConsoleScriptContext() {
        this(new StringReader(""),
                new StringWriter(),
                new StringWriter());
        engineScope = new SimpleBindings();
        globalScope = null;
        console = CapturingConsole.captureAllConsole(
                new StreamWriterConsole(this.writer, true));
    }

    ConsoleScriptContext(Reader reader, Writer writer, Writer errorWriter) {
        this.reader = reader;
        this.writer = writer;
        this.errorWriter = errorWriter;
    }

}
