package com.ramblingpenguin.rookery.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

import static org.junit.jupiter.api.Assertions.*;

class IsolatedRookeryContextTest {

    private WeakReference<ClassLoader> invokePluginAndGetLoader(Path jarPath) throws Exception {
        try (IsolatedRookeryContext context = IsolatedRookeryContext.create(List.of(jarPath))) {
            Optional<Class<?>> pluginClassOpt = context.loadClass("com.example.dummy.DummyPlugin");
            assertTrue(pluginClassOpt.isPresent(), "Loaded class should be present in new layer");
            
            Class<?> pluginClass = pluginClassOpt.get();
            Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
            assertEquals("world", pluginClass.getMethod("hello").invoke(pluginInstance));
            
            return new WeakReference<>(pluginClass.getClassLoader());
        }
    }

    @Test
    void testLayerIsolationAndGarbageCollection(@TempDir Path tempDir) throws Exception {
        // ... compilation ...
        Path dummySrcPath = tempDir.resolve("src").resolve("com.example.dummy");
        Files.createDirectories(dummySrcPath);
        
        Path moduleInfoPath = dummySrcPath.resolve("module-info.java");
        Files.writeString(moduleInfoPath, "module com.example.dummy { exports com.example.dummy; }");
        
        Path pluginClassPath = dummySrcPath.resolve("DummyPlugin.java");
        Files.writeString(pluginClassPath, 
            "package com.example.dummy;\n" +
            "public class DummyPlugin { public String hello() { return \"world\"; } }\n");

        Path outDir = tempDir.resolve("out");
        Files.createDirectories(outDir);
        
        ToolProvider javac = ToolProvider.findFirst("javac")
                .orElseThrow(() -> new IllegalStateException("javac tool not found"));
        int compileResult = javac.run(System.out, System.err, "-d", outDir.toString(), moduleInfoPath.toString(), pluginClassPath.toString());
        assertEquals(0, compileResult, "Compilation failed");

        Path jarPath = tempDir.resolve("dummy-plugin.jar");
        ToolProvider jarTool = ToolProvider.findFirst("jar")
                .orElseThrow(() -> new IllegalStateException("jar tool not found"));
        int jarResult = jarTool.run(System.out, System.err, "--create", "--file", jarPath.toString(), "-C", outDir.toString(), ".");
        assertEquals(0, jarResult, "Jar creation failed");

        // 4. Test Isolation & 5. Test GC Sweep
        WeakReference<ClassLoader> classLoaderRef = invokePluginAndGetLoader(jarPath);

        for (int i = 0; i < 15; i++) {
            System.gc();
            Thread.sleep(100);
            if (classLoaderRef.get() == null) {
                break;
            }
        }
        
        assertNull(classLoaderRef.get(), "ClassLoader should be garbage collected after context closed");
    }
}
