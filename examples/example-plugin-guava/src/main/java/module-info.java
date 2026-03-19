module com.ramblingpenguin.rookery.examples.plugin {
    requires com.ramblingpenguin.rookery.examples.api;
    requires com.google.common;

    provides com.ramblingpenguin.rookery.examples.api.TextProcessor 
        with com.ramblingpenguin.rookery.examples.plugin.GuavaTextProcessor;
}
