package com.ramblingpenguin.rookery.api;

import java.nio.file.Path;
import java.util.List;

public interface RookeryEngine {
    RookeryContext create(List<Path> jarPaths);
}
