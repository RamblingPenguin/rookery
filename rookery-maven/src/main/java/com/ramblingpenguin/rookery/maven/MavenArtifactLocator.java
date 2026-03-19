package com.ramblingpenguin.rookery.maven;

import com.ramblingpenguin.rookery.api.ArtifactLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MavenArtifactLocator implements ArtifactLocator<MavenArtifactLocator.MavenArtifactInfo> {

    public record MavenArtifactInfo(String groupId, String artifactId, String version, String updatePolicy) implements ArtifactLocator.ArtifactInfo {
        public MavenArtifactInfo(String groupId, String artifactId, String version) {
            this(groupId, artifactId, version, org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_DAILY);
        }
    }

    @Override
    public Class<MavenArtifactInfo> supportedType() {
        return MavenArtifactInfo.class;
    }

    @Override
    public boolean canParseURI(String uri) {
        return uri != null && uri.startsWith("maven://");
    }

    @Override
    public MavenArtifactInfo parseURI(String uri) throws Exception {
        if (!canParseURI(uri)) {
            throw new IllegalArgumentException("Unsupported URI scheme: " + uri);
        }
        String ssp = uri.substring("maven://".length());
        
        String updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY;
        if (ssp.contains("?")) {
            String[] split = ssp.split("\\?");
            ssp = split[0];
            String query = split[1];
            if (query.startsWith("updatePolicy=")) {
                updatePolicy = query.substring("updatePolicy=".length());
            }
        }
        
        String[] parts = ssp.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid Maven URI. Expected maven://groupId:artifactId:version[?updatePolicy=interval:X]");
        }
        return new MavenArtifactInfo(parts[0], parts[1], parts[2], updatePolicy);
    }

    @Override
    public List<Path> fetch(MavenArtifactInfo info) throws Exception {
        RepositorySystem system = newRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(system);

        RepositoryPolicy policy = new RepositoryPolicy(true, info.updatePolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);
        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
            .setSnapshotPolicy(policy)
            .setReleasePolicy(policy)
            .build();

        Artifact artifact = new DefaultArtifact(info.groupId(), info.artifactId(), "jar", info.version());
        
        if ("LATEST".equalsIgnoreCase(info.version()) || "RELEASE".equalsIgnoreCase(info.version()) 
            || info.version().startsWith("[") || info.version().startsWith("(")) {
            
            String rangeVersion = "LATEST".equalsIgnoreCase(info.version()) || "RELEASE".equalsIgnoreCase(info.version()) ? "[0,)" : info.version();
            Artifact rangeArtifact = new DefaultArtifact(info.groupId(), info.artifactId(), "jar", rangeVersion);
            
            VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(rangeArtifact);
            rangeRequest.setRepositories(Collections.singletonList(central));
            
            VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);
            Version highestVersion = rangeResult.getHighestVersion();
            
            if (highestVersion == null) {
                throw new IllegalArgumentException("Could not resolve dynamic version " + info.version() + " for " + info.groupId() + ":" + info.artifactId());
            }
            artifact = artifact.setVersion(highestVersion.toString());
        }

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "compile"));
        collectRequest.setRepositories(Collections.singletonList(central));

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);

        return dependencyResult.getArtifactResults().stream()
                .map(ArtifactResult::getArtifact)
                .map(Artifact::getFile)
                .map(java.io.File::toPath)
                .collect(Collectors.toList());
    }

    private static RepositorySystem newRepositorySystem() {
        @SuppressWarnings("deprecation")
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                System.err.println("Service creation failed for " + type + " with impl " + impl);
                exception.printStackTrace();
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        @SuppressWarnings("deprecation")
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        
        String userHome = System.getProperty("user.home");
        LocalRepository localRepo = new LocalRepository(userHome + "/.m2/repository");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }
}
