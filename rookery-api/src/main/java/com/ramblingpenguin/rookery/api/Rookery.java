package com.ramblingpenguin.rookery.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class Rookery {

    private Rookery() {}

    public static RookeryContext fromEnv() {
        return fromConfig(System.getenv());
    }

    public static RookeryContext fromConfig(Map<String, String> config) {
        String pluginsStr = config.get("rookery.plugins");
        if (pluginsStr == null || pluginsStr.isBlank()) {
            pluginsStr = config.get("ROOKERY_PLUGINS");
        }
        
        if (pluginsStr == null || pluginsStr.isBlank()) {
            return load(new String[0]); // Load empty context if no plugins specified
        }
        
        String[] pluginUris = pluginsStr.split(",");
        for (int i = 0; i < pluginUris.length; i++) {
            pluginUris[i] = pluginUris[i].trim();
        }
        
        return load(pluginUris);
    }

    public static RookeryContext load(String... pluginUris) {
        List<ArtifactLocator.ArtifactInfo> infos = new ArrayList<>();
        
        @SuppressWarnings("rawtypes")
        ServiceLoader<ArtifactLocator> locators = ServiceLoader.load(ArtifactLocator.class);
        
        for (String uri : pluginUris) {
            boolean parsed = false;
            for (ArtifactLocator<?> locator : locators) {
                if (locator.canParseURI(uri)) {
                    try {
                        infos.add(locator.parseURI(uri));
                        parsed = true;
                        break;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse URI: " + uri, e);
                    }
                }
            }
            if (!parsed) {
                throw new IllegalArgumentException("No ArtifactLocator found for URI scheme: " + uri);
            }
        }
        
        return load(infos.toArray(new ArtifactLocator.ArtifactInfo[0]));
    }

    public static RookeryContext load(ArtifactLocator.ArtifactInfo... plugins) {
        List<Path> allPaths = new ArrayList<>();
        
        @SuppressWarnings("rawtypes")
        ServiceLoader<ArtifactLocator> locators = ServiceLoader.load(ArtifactLocator.class);
        
        for (ArtifactLocator.ArtifactInfo plugin : plugins) {
            boolean fetched = false;
            for (ArtifactLocator<?> locator : locators) {
                if (locator.supportedType().isInstance(plugin)) {
                    try {
                        allPaths.addAll(fetchWithLocator(locator, plugin));
                        fetched = true;
                        break;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to fetch plugin: " + plugin, e);
                    }
                }
            }
            if (!fetched) {
                throw new IllegalArgumentException("No ArtifactLocator found for plugin type: " + plugin.getClass().getName());
            }
        }
        
        RookeryEngine engine = ServiceLoader.load(RookeryEngine.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No RookeryEngine provider found. Make sure rookery-core is on the module path."));
            
        return engine.create(allPaths);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ArtifactLocator.ArtifactInfo> List<Path> fetchWithLocator(ArtifactLocator<T> locator, ArtifactLocator.ArtifactInfo plugin) throws Exception {
        return locator.fetch((T) plugin);
    }
}
