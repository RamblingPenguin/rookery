package com.ramblingpenguin.rookery.core;

import com.ramblingpenguin.rookery.api.RookeryContext;
import com.ramblingpenguin.rookery.api.RookeryEngine;

import java.nio.file.Path;
import java.util.List;

public class CoreRookeryEngine implements RookeryEngine {
    @Override
    public RookeryContext create(List<Path> jarPaths) {
        return IsolatedRookeryContext.create(jarPaths);
    }
}
