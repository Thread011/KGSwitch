package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

import org.apache.jena.rdf.model.*;
import com.kgswitch.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

class SchemaTransformationServiceTest {
    private static final String TEST_RESOURCES = "src/test/resources";
    private Path watchDir;
    private SchemaTransformationService transformationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        // Create test directory
        watchDir = Paths.get(TEST_RESOURCES + "/test_dataset").toAbsolutePath();
        Files.createDirectories(watchDir);
        
        // Initialize object mapper
        objectMapper = new ObjectMapper();
        
        // Create flight schema
        String flightSchemaContent = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix schema: <http://schema.org/> .
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

            schema:FlightReservationShape
                a sh:NodeShape ;
                sh:targetClass schema:FlightReservation ;
                sh:property [
                    sh:path schema:reservationId ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:reservationStatus ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:underName ;
                    sh:class schema:Person ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                    sh:property [
                        sh:path schema:bookingTime ;
                        sh:datatype xsd:dateTime ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:bookingAgent ;
                        sh:datatype xsd:string ;
                    ] ;
                ] ;
                sh:property [
                    sh:path schema:reservationFor ;
                    sh:class schema:Flight ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                    sh:property [
                        sh:path schema:seatNumber ;
                        sh:datatype xsd:string ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:ticketNumber ;
                        sh:datatype xsd:string ;
                        sh:minCount 1 ;
                    ] ;
                ] .

            schema:PersonShape
                a sh:NodeShape ;
                sh:targetClass schema:Person ;
                sh:property [
                    sh:path schema:givenName ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:familyName ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:email ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:memberOf ;
                    sh:class schema:Organization ;
                    sh:minCount 0 ;
                    sh:property [
                        sh:path schema:startDate ;
                        sh:datatype xsd:date ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:role ;
                        sh:datatype xsd:string ;
                        sh:minCount 1 ;
                    ] ;
                ] .

            schema:FlightShape
                a sh:NodeShape ;
                sh:targetClass schema:Flight ;
                sh:property [
                    sh:path schema:flightNumber ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:departureAirport ;
                    sh:class schema:Airport ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                    sh:property [
                        sh:path schema:scheduledTime ;
                        sh:datatype xsd:dateTime ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:terminal ;
                        sh:datatype xsd:string ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:gate ;
                        sh:datatype xsd:string ;
                    ] ;
                ] ;
                sh:property [
                    sh:path schema:arrivalAirport ;
                    sh:class schema:Airport ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                    sh:property [
                        sh:path schema:scheduledTime ;
                        sh:datatype xsd:dateTime ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:terminal ;
                        sh:datatype xsd:string ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:gate ;
                        sh:datatype xsd:string ;
                    ] ;
                ] .

            schema:AirportShape
                a sh:NodeShape ;
                sh:targetClass schema:Airport ;
                sh:property [
                    sh:path schema:iataCode ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                    sh:maxCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:name ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:address ;
                    sh:class schema:PostalAddress ;
                    sh:minCount 1 ;
                    sh:property [
                        sh:path schema:verified ;
                        sh:datatype xsd:boolean ;
                        sh:minCount 1 ;
                    ] ;
                    sh:property [
                        sh:path schema:lastUpdated ;
                        sh:datatype xsd:dateTime ;
                        sh:minCount 1 ;
                    ] ;
                ] .

            schema:OrganizationShape
                a sh:NodeShape ;
                sh:targetClass schema:Organization ;
                sh:property [
                    sh:path schema:name ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                ] ;
                sh:property [
                    sh:path schema:legalName ;
                    sh:datatype xsd:string ;
                    sh:minCount 1 ;
                ] .
            """;
        
        Path schemaPath = watchDir.resolve("flight-schema.ttl");
        Files.writeString(schemaPath, flightSchemaContent);
        
        transformationService = new SchemaTransformationService();
        System.out.println("Test setup complete. Schema file created at: " + schemaPath);
    }

    @Test
    void testFlightSchemaTransformation() throws Exception {
        // Define input and output file paths
        Path schemaPath = watchDir.resolve("flight-schema.ttl");
        Path jsonSchemaPath = watchDir.resolve("flight-schema_pg_schema.json");
        
        // Ensure the input schema file exists
        assertTrue(Files.exists(schemaPath), 
            "Input schema file does not exist: " + schemaPath);
    
        // Perform transformation
        transformationService.transformSchema(schemaPath);
    
        // Verify JSON schema file was created
        assertTrue(Files.exists(jsonSchemaPath), 
            "JSON schema file should be created at: " + jsonSchemaPath);
    
        // Read and parse the JSON
        String jsonContent = Files.readString(jsonSchemaPath);
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        
        // Debug: Print the entire JSON structure
        System.out.println("\n=== Full JSON Schema ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
        
        // Verify relationships with detailed debugging
        JsonNode relationships = jsonNode.get("relationships");
        assertNotNull(relationships, "Relationships array should exist");
        assertTrue(relationships.isArray(), "Relationships should be an array");
        
        System.out.println("\n=== Relationships Debug ===");
        System.out.println("Total relationships found: " + relationships.size());
        
        for (JsonNode rel : relationships) {
            System.out.println("\nRelationship Details:");
            System.out.println("Type: " + rel.get("type").asText());
            System.out.println("Source: " + rel.get("source").asText());
            System.out.println("Target: " + rel.get("target").asText());
            
            JsonNode props = rel.get("properties");
            System.out.println("Properties:");
            if (props != null && props.size() > 0) {
                props.fields().forEachRemaining(entry -> {
                    System.out.println("  - " + entry.getKey() + ":");
                    JsonNode propDetails = entry.getValue();
                    System.out.println("    Type: " + propDetails.get("type"));
                    if (propDetails.has("minCount")) {
                        System.out.println("    MinCount: " + propDetails.get("minCount"));
                    }
                    if (propDetails.has("maxCount")) {
                        System.out.println("    MaxCount: " + propDetails.get("maxCount"));
                    }
                });
            } else {
                System.out.println("  No properties found!");
            }
            
            // Check cardinality of the relationship itself
            if (rel.has("minCount")) {
                System.out.println("Relationship MinCount: " + rel.get("minCount"));
            }
            if (rel.has("maxCount")) {
                System.out.println("Relationship MaxCount: " + rel.get("maxCount"));
            }
        }
        
        // Verify specific relationships with properties
        System.out.println("\n=== Verifying Specific Relationships ===");
        
        // Check underName relationship
        boolean foundUnderName = false;
        for (JsonNode rel : relationships) {
            if (rel.get("type").asText().equalsIgnoreCase("undername")) {
                foundUnderName = true;
                System.out.println("\nFound underName relationship:");
                
                // Verify source and target
                assertTrue(rel.get("source").asText().contains("FlightReservation"),
                    "Source should be FlightReservation");
                assertTrue(rel.get("target").asText().contains("Person"),
                    "Target should be Person");
                
                // Verify properties
                JsonNode props = rel.get("properties");
                assertNotNull(props, "Properties should exist for underName relationship");
                
                System.out.println("Checking underName properties:");
                assertTrue(props.has("bookingTime"), 
                    "Should have bookingTime property");
                assertTrue(props.has("bookingAgent"), 
                    "Should have bookingAgent property");
                
                // Verify property details
                if (props.has("bookingTime")) {
                    JsonNode bookingTime = props.get("bookingTime");
                    System.out.println("bookingTime details: " + bookingTime);
                    assertEquals("DateTime", bookingTime.get("type").asText(),
                        "bookingTime should be DateTime type");
                    assertEquals(1, bookingTime.get("minCount").asInt(),
                        "bookingTime should have minCount 1");
                }
                
                if (props.has("bookingAgent")) {
                    JsonNode bookingAgent = props.get("bookingAgent");
                    System.out.println("bookingAgent details: " + bookingAgent);
                    assertEquals("String", bookingAgent.get("type").asText(),
                        "bookingAgent should be String type");
                }
            }
        }
        
        assertTrue(foundUnderName, "Should contain UNDERNAME relationship");
    }

    @AfterEach
    void cleanup() throws IOException {
        // Clean up test files
        if (Files.exists(watchDir)) {
            Files.walk(watchDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Error deleting: " + path);
                    }
                });
        }
    }
}