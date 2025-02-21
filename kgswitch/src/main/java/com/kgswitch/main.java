package com.kgswitch;

import com.kgswitch.core.SchemaTransformationService;
import com.kgswitch.core.SchemaTransformationException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar kgswitch.jar <path-to-shacl-file>");
            System.exit(1);
        }

        try {
            // Initialize the transformation service
            SchemaTransformationService transformationService = new SchemaTransformationService();
            
            // Get input path from command line arguments
            Path inputPath = Paths.get(args[0]);
            
            // Transform the schema - this will generate both RDF and PG schema files
            transformationService.transformSchema(inputPath);
            
            // Output files will be created with the following naming convention:
            // - RDF output: biolink_model.shacl_transformed.ttl
            // - PG Schema output: biolink_model.shacl_pg_schema.json
            
            System.out.println("Transformation completed successfully!");
            System.out.println("Check the output files in the same directory as the input file.");
            
        } catch (SchemaTransformationException e) {
            System.err.println("Schema transformation failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}