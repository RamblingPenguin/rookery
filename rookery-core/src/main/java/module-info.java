module com.ramblingpenguin.rookery.core {
    requires transitive com.ramblingpenguin.rookery.api;
    
    exports com.ramblingpenguin.rookery.core;
    
    provides com.ramblingpenguin.rookery.api.RookeryEngine 
        with com.ramblingpenguin.rookery.core.CoreRookeryEngine;
}
