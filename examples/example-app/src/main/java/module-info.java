module com.ramblingpenguin.rookery.examples.app {
    requires com.ramblingpenguin.rookery.api;
    requires com.ramblingpenguin.rookery.examples.api;
    
    // We need to 'uses' the SPI that the plugin will provide
    uses com.ramblingpenguin.rookery.examples.api.TextProcessor;
}
