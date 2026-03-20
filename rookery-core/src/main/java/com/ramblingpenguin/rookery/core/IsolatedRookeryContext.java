package com.ramblingpenguin.rookery.core;

import com.ramblingpenguin.rookery.api.RookeryContext;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class IsolatedRookeryContext implements RookeryContext {
    
    private ModuleLayer layer;
    private final List<Path> jarPaths;

    private IsolatedRookeryContext(List<Path> jarPaths) {
        this.jarPaths = List.copyOf(jarPaths);
        this.layer = buildLayer(this.jarPaths);
    }

    /**
     * Factory method invoked by the Rookery entrypoint after ArtifactLocators 
     * have finished fetching the physical JARs.
     */
    public static IsolatedRookeryContext create(List<Path> paths) {
        return new IsolatedRookeryContext(paths);
    }

    private static ModuleLayer buildLayer(List<Path> paths) {
        // 1. Point the ModuleFinder exactly at our downloaded JARs
        ModuleFinder finder = ModuleFinder.of(paths.toArray(new Path[0]));

        // 2. Dynamically extract the module names from the JAR descriptors
        // We need these roots to tell the Configuration what to resolve.
        Set<String> roots = finder.findAll().stream()
                .map(ModuleReference::descriptor)
                .map(java.lang.module.ModuleDescriptor::name)
                .collect(Collectors.toSet());

        if (roots.isEmpty()) {
            throw new IllegalArgumentException("No valid JPMS modules found in the resolved paths: " + paths);
        }

        // 3. Resolve the new configuration using the Boot Layer as the parent.
        // We pass an empty ModuleFinder for the second argument so it doesn't 
        // accidentally pull unintended modules from the system path.
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer.configuration().resolve(
                finder,
                ModuleFinder.of(), 
                roots
        );

        // 4. Spawn the layer. 
        // defineModulesWithOneLoader ensures all plugins in this context share a single 
        // classloader space, avoiding complex split-package issues between siblings.
        return bootLayer.defineModulesWithOneLoader(cf, ClassLoader.getSystemClassLoader());
    }

    @Override
    public ModuleLayer getLayer() {
        return layer;
    }



    @Override
    public Optional<Class<?>> loadClass(String className) {
        ensureOpen();
        
        // We use defineModulesWithOneLoader, so all plugins in this layer share the same classloader.
        // We can grab the classloader from any module in the layer.
        Optional<Module> anyModule = layer.modules().stream().findFirst();
        if (anyModule.isPresent()) {
            ClassLoader loader = anyModule.get().getClassLoader();
            if (loader != null) {
                try {
                    return Optional.of(loader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    // Swallow and return Optional.empty()
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        // Aggressively sever the strong reference to the ModuleLayer.
        // Without this reference, the GC will mark the layer, its classloader, 
        // and all loaded bytecode as unreachable and sweep it.
        this.layer = null;
    }
    
    private void ensureOpen() {
        if (layer == null) {
            throw new IllegalStateException("RookeryContext is closed. The ModuleLayer has been unlinked.");
        }
    }
}
