package com.ramblingpenguin.rookery.api;

import java.nio.file.Path;
import java.util.List;

public interface ArtifactLocator<T extends ArtifactLocator.ArtifactInfo> {

    // Marker interface for strongly-typed configuration records
    interface ArtifactInfo {}

    // 1. "What type of configuration parameter do you support?"
    Class<T> supportedType();

    // 2. Dynamic Route (For Env vars, Config files, URIs)
    boolean canParseURI(String uri);
    T parseURI(String uri) throws Exception;

    // 3. "Fetch using the configuration"
    List<Path> fetch(T info) throws Exception;
}
