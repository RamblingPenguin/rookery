package com.ramblingpenguin.rookery.examples.app;

import com.ramblingpenguin.rookery.api.Rookery;
import com.ramblingpenguin.rookery.api.RookeryContext;
import com.ramblingpenguin.rookery.examples.api.TextProcessor;

import java.util.List;

public class ExampleApp {
    public static void main(String[] args) {
        System.out.println("Starting Rookery Example App...");
        
        // Instruct Rookery to load our isolated plugin via Maven
        // (Make sure you've ran `mvn install` on the project first so it's in ~/.m2!)
        String pluginCoordinates = "maven://com.ramblingpenguin.rookery:example-plugin-guava:1.0.0-SNAPSHOT";
        
        System.out.println("Fetching plugin: " + pluginCoordinates);
        
        try (RookeryContext context = Rookery.load(pluginCoordinates)) {
            
            // Fetch implementations of our contract from the isolated plugin layer!
            List<TextProcessor> processors = context.getImplementationsOf(TextProcessor.class);
            
            if (processors.isEmpty()) {
                System.out.println("No TextProcessor implementations found!");
                return;
            }
            
            TextProcessor processor = processors.get(0);
            System.out.println("Loaded TextProcessor: " + processor.getClass().getName());
            
            String input = "Hello 123 World 456!";
            System.out.println("Original Text: '" + input + "'");
            
            // This will execute Guava's CharMatcher completely isolated!
            String processed = processor.process(input);
            System.out.println("Processed Text: '" + processed + "'");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Context closed and isolated classloaders have been released for GC.");
    }
}
