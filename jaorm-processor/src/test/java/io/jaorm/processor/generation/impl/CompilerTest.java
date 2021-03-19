package io.jaorm.processor.generation.impl;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Assertions;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.stream.Collectors;

public abstract class CompilerTest {

    protected JavaFileObject getFile(String folder, String name) {
        return JavaFileObjects.forResource(CompilerTest.class.getResource("/" + folder + "/" + name));
    }

    protected static JavaFileObject getFile(String fullName) {
        return JavaFileObjects.forResource(CompilerTest.class.getResource(fullName));
    }

    protected void checkCompilation(Compilation compilation) {
        Assertions.assertEquals(Compilation.Status.SUCCESS, compilation.status(), generateError(compilation.errors()));
    }

    protected void checkCompilation(Compilation compilation, String message) {
        Assertions.assertEquals(Compilation.Status.SUCCESS, compilation.status(), message + " " + generateError(compilation.errors()));
    }

    protected String generateError(ImmutableList<Diagnostic<? extends JavaFileObject>> errors) {
        return errors.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }
}
