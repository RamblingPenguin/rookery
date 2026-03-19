package com.ramblingpenguin.rookery.maven;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MavenArtifactLocatorTest {

    @Test
    void testResolveSimpleArtifact() throws Exception {
        MavenArtifactLocator locator = new MavenArtifactLocator();
        
        assertEquals(MavenArtifactLocator.MavenArtifactInfo.class, locator.supportedType());

        // Resolve slf4j-api which is small and fast
        MavenArtifactLocator.MavenArtifactInfo info = new MavenArtifactLocator.MavenArtifactInfo("org.slf4j", "slf4j-api", "2.0.9");
        List<Path> paths = locator.fetch(info);
        
        assertFalse(paths.isEmpty(), "Should have resolved at least the main artifact");
        
        boolean foundSlf4j = paths.stream()
                .anyMatch(p -> p.getFileName().toString().contains("slf4j-api-2.0.9.jar"));
        
        assertTrue(foundSlf4j, "Should contain the resolved jar file. Found: " + paths);
    }
}
