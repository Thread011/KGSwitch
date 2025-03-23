package com.kgswitch;

import com.kgswitch.core.SchemaTransformationService;
import com.kgswitch.core.SchemaTransformationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        try {
            // Parse command line arguments
            Map<String, String> options = parseCommandLineArgs(args);
            
            // Get input path from command line arguments
            Path inputPath = Paths.get(options.get("input"));
            
            // Initialize the transformation service
            SchemaTransformationService transformationService = new SchemaTransformationService();
            
            // Check if image visualization is enabled
            boolean generateImage = options.containsKey("image");
            String outputImage = options.get("output");
            
            if (generateImage && outputImage != null) {
                // Generate image visualization
                String imageFile = transformationService.transformSchemaWithImage(
                    inputPath,
                    outputImage
                );
                System.out.println("Image visualization generated: " + imageFile);
            } else {
                // Check if Neo4j visualization is enabled
                boolean visualizeInNeo4j = options.containsKey("neo4j");
                String neo4jUri = options.get("uri");
                String neo4jUser = options.get("user");
                String neo4jPassword = options.get("password");
                
                // Transform the schema - this will generate both RDF and PG schema files
                // and optionally visualize in Neo4j
                transformationService.transformSchema(
                    inputPath, 
                    visualizeInNeo4j, 
                    neo4jUri, 
                    neo4jUser, 
                    neo4jPassword
                );
                
                System.out.println("Transformation completed successfully!");
            }
        } catch (SchemaTransformationException e) {
            System.err.println("Schema transformation failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse command line arguments into a map of options
     * 
     * @param args Command line arguments
     * @return Map of option name to value
     */
    private static Map<String, String> parseCommandLineArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        
        // First argument is always the input file
        options.put("input", args[0]);
        
        // Process additional options
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.equals("--neo4j")) {
                // Enable Neo4j visualization
                options.put("neo4j", "true");
            } else if (arg.equals("--image")) {
                // Enable image visualization
                options.put("image", "true");
            } else if (arg.equals("--output") && i + 1 < args.length) {
                // Output image file path
                options.put("output", args[++i]);
            } else if (arg.equals("--uri") && i + 1 < args.length) {
                // Neo4j URI
                options.put("uri", args[++i]);
            } else if (arg.equals("--user") && i + 1 < args.length) {
                // Neo4j username
                options.put("user", args[++i]);
            } else if (arg.equals("--password") && i + 1 < args.length) {
                // Neo4j password
                options.put("password", args[++i]);
            } else if (arg.equals("--help")) {
                // Print usage and exit
                printUsage();
                System.exit(0);
            }
        }
        
        return options;
    }
    
    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar kgswitch.jar <path-to-shacl-file> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --neo4j               Enable Neo4j visualization");
        System.out.println("  --uri <uri>           Neo4j connection URI (default: bolt://localhost:7687)");
        System.out.println("  --user <username>     Neo4j username (default: neo4j)");
        System.out.println("  --password <password> Neo4j password (default: password)");
        System.out.println("  --image               Generate image visualization");
        System.out.println("  --output <path>       Output image file path (.dot format recommended)");
        System.out.println("  --help                Display this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Visualize schema in Neo4j");
        System.out.println("  java -jar kgswitch.jar schema.ttl --neo4j --uri bolt://localhost:7687 --user neo4j --password mypassword");
        System.out.println();
        System.out.println("  # Generate image visualization");
        System.out.println("  java -jar kgswitch.jar schema.ttl --image --output schema.dot");
        System.out.println();
        System.out.println("  # View the DOT file or convert it to an image using Graphviz:");
        System.out.println("  dot -Tpng schema.dot -o schema.png");
    }
}