module com.ramblingpenguin.rookery.maven {
    requires transitive com.ramblingpenguin.rookery.api;
    
    requires org.apache.maven.resolver;
    requires org.apache.maven.resolver.spi;
    requires org.apache.maven.resolver.util;
    requires org.apache.maven.resolver.impl;
    requires org.apache.maven.resolver.connector.basic;
    requires org.apache.maven.resolver.transport.http;
    requires org.apache.maven.resolver.transport.file;
    requires maven.resolver.provider;
    requires org.slf4j;
    
    exports com.ramblingpenguin.rookery.maven;
    
    provides com.ramblingpenguin.rookery.api.ArtifactLocator 
        with com.ramblingpenguin.rookery.maven.MavenArtifactLocator;
}
