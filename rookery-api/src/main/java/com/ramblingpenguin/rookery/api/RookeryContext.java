package com.ramblingpenguin.rookery.api;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface RookeryContext extends AutoCloseable {

    // The raw isolated ModuleLayer containing the loaded plugins.
    // Callers MUST use this to invoke ServiceLoader directly, as JPMS requires the
    // calling
    // module to statically declare `uses <Interface>` in its module-info.java.
    ModuleLayer getLayer();

    /**
     * Extracts all implementations of a given service interface.
     * To satisfy JPMS encapsulation rules, the calling module must provide a lambda
     * that invokes {@code ServiceLoader.load()} so that the JVM can verify its
     * {@code uses} declaration.
     * 
     * Example:
     * {@code ctx.getImplementations(layer -> ServiceLoader.load(layer, MyService.class))}
     */
    default <T> List<T> getImplementations(Function<ModuleLayer, ServiceLoader<T>> loaderFunction) {
        return loaderFunction.apply(getLayer()).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    default <T> Optional<T> getImplementation(Function<ModuleLayer, ServiceLoader<T>> loaderFunction) {
        return loaderFunction.apply(getLayer()).stream()
                .map(ServiceLoader.Provider::get)
                .findFirst();
    }

    /**
     * Finds a specific implementation by its fully-qualified class name.
     * Only the matching provider gets instantiated.
     */
    default <T> Optional<T> getImplementation(Function<ModuleLayer, ServiceLoader<T>> loaderFunction,
            String className) {
        return loaderFunction.apply(getLayer()).stream()
                .filter(p -> p.type().getName().equals(className))
                .map(ServiceLoader.Provider::get)
                .findFirst();
    }

    // For advanced tasks, finds a specific class by name
    Optional<Class<?>> loadClass(String className);

    @Override
    void close(); // Triggers the GC release
}
