package com.ramblingpenguin.rookery.api;

import java.util.List;
import java.util.Optional;

public interface RookeryContext extends AutoCloseable {
    // Magically extracts all implementations of a given interface from the isolated layer
    <T> List<T> getImplementationsOf(Class<T> serviceInterface);

    // For advanced tasks, finds a specific class by name
    Optional<Class<?>> loadClass(String className);

    @Override
    void close(); // Triggers the GC release
}
