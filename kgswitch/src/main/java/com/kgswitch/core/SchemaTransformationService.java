package com.kgswitch.core;

import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.transforms.rdf.RDFSchemaTransformer;
import com.kgswitch.transforms.pg.PGSchemaToStatementTransformer;
import com.kgswitch.transforms.pg.PGStatementToSchemaTransformer;
import com.kgswitch.transforms.rdf.StatementToRDFTransformer;
import com.kgswitch.util.JsonSchemaGenerator;
import com.kgswitch.util.CypherQueryGenerator;
import com.kgswitch.util.Neo4jConnector;
import org.apache.jena.rdf.model.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.kgswitch.util.GraphVisualizer;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Collection;
import java.io.IOException;
import java.util.Map;

public class SchemaTransformationService {
    private final RDFSchemaTransformer rdfTransformer;
    private final ObjectMapper objectMapper;
    
    public SchemaTransformationService() {
        this.rdfTransformer = new RDFSchemaTransformer();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Transform an RDF/SHACL schema to a Property Graph schema
     * 
     * @param schemaFile Path to the schema file
     * @throws SchemaTransformationException If transformation fails
     */
    public void transformSchema(Path schemaFile) throws SchemaTransformationException {
        transformSchema(schemaFile, false, null, null, null);
    }
    
    /**
     * Transform an RDF/SHACL schema to a Property Graph schema with option to visualize in Neo4j
     * 
     * @param schemaFile Path to the schema file
     * @param visualizeInNeo4j Whether to visualize the schema in Neo4j
     * @param neo4jUri Neo4j connection URI (null for default)
     * @param neo4jUser Neo4j username (null for default)
     * @param neo4jPassword Neo4j password (null for default)
     * @throws SchemaTransformationException If transformation fails
     */
    public void transformSchema(Path schemaFile, boolean visualizeInNeo4j, 
                                String neo4jUri, String neo4jUser, String neo4jPassword) 
                                throws SchemaTransformationException {
        try {
            System.out.println("Starting transformation for schema: " + schemaFile);
            
            // Step 1: RDF to RDF Statement Graph
            SchemaGraph rdfStatementGraph = rdfTransformer.transformToStatementGraph(
                schemaFile.toAbsolutePath().normalize().toString());
            validateGraph(rdfStatementGraph, "RDF Statement Graph");
            
            // Step 2: RDF Statement Graph to PG Statement Graph
            PGSchemaToStatementTransformer pgTransformer = 
                new PGSchemaToStatementTransformer(rdfStatementGraph);
            SchemaGraph pgStatementGraph = pgTransformer.transformToStatementGraph();
            validateGraph(pgStatementGraph, "PG Statement Graph");
            
            // Step 3: PG Statement Graph to PG Schema
            PGStatementToSchemaTransformer schemaTransformer = 
                new PGStatementToSchemaTransformer(pgStatementGraph);
            SchemaGraph pgSchema = schemaTransformer.transformToPGSchema();
            validateGraph(pgSchema, "PG Schema");
            
            // Convert back to RDF and save
            StatementToRDFTransformer rdfTransformer = 
                new StatementToRDFTransformer(pgStatementGraph);
            Model transformedRDF = rdfTransformer.transformToRDF();
            saveRDFModel(schemaFile, transformedRDF);
            
            // Save PG Schema statements
            String jsonSchemaFile = savePGSchema(schemaFile, pgSchema);
            
            // Generate Cypher for Neo4j
            String cypherFile = generateCypherQueries(schemaFile, jsonSchemaFile);
            
            // Visualize in Neo4j if requested
            if (visualizeInNeo4j) {
                visualizeSchemaInNeo4j(jsonSchemaFile, neo4jUri, neo4jUser, neo4jPassword);
            }
            
            System.out.println("Transformation completed successfully. Files created:");
            System.out.println("- RDF: " + schemaFile.toString().replace(".ttl", "_transformed.ttl"));
            System.out.println("- PG Schema: " + jsonSchemaFile);
            System.out.println("- Cypher: " + cypherFile);
            
        } catch (Exception e) {
            System.err.println("Transformation failed: " + e.getMessage());
            throw new SchemaTransformationException(
                "Failed to transform schema: " + schemaFile, schemaFile, e);
        }
    }
    
    /**
     * Generate Cypher queries for Neo4j visualization
     * 
     * @param schemaFile Original schema file path
     * @param jsonSchemaFile JSON schema file path
     * @return Path to the generated Cypher file
     * @throws Exception If generation fails
     */
    private String generateCypherQueries(Path schemaFile, String jsonSchemaFile) throws Exception {
        // Generate Cypher queries for Neo4j
        CypherQueryGenerator cypherGenerator = new CypherQueryGenerator();
        String cypherQueries = cypherGenerator.generateCypherFromFile(jsonSchemaFile);
        
        // Save Cypher to file
        String cypherFile = schemaFile.toString().replace(".ttl", "_neo4j.cypher");
        cypherGenerator.writeCypherToFile(cypherQueries, cypherFile);
        
        return cypherFile;
    }
    
    /**
     * Visualize the schema in Neo4j
     * 
     * @param jsonSchemaFile JSON schema file path
     * @param neo4jUri Neo4j URI (null for default)
     * @param neo4jUser Neo4j username (null for default)
     * @param neo4jPassword Neo4j password (null for default)
     * @throws Exception If visualization fails
     */
    private void visualizeSchemaInNeo4j(String jsonSchemaFile, 
                                    String neo4jUri, String neo4jUser, String neo4jPassword) 
                                    throws Exception {
        // Connect to Neo4j and visualize the schema
        try (Neo4jConnector connector = (neo4jUri != null && neo4jUser != null && neo4jPassword != null) ? 
                new Neo4jConnector(neo4jUri, neo4jUser, neo4jPassword) : 
                new Neo4jConnector()) {
            
            String result = connector.visualizeJsonSchema(jsonSchemaFile);
            System.out.println("Neo4j Visualization Result:");
            System.out.println(result);
        }
    }
    
    private void validateGraph(SchemaGraph graph, String phase) {
        if (graph == null) {
            throw new IllegalStateException(phase + " produced null graph");
        }
        System.out.println(phase + " contains " + graph.getNodes().size() + 
                          " nodes and " + graph.getEdges().size() + " edges");
    }
    
    private void saveRDFModel(Path schemaFile, Model model) throws Exception {
        String outputFile = schemaFile.toString().replace(".ttl", "_transformed.ttl");
        try (FileWriter writer = new FileWriter(outputFile)) {
            model.write(writer, "TURTLE");
        }
    }
    
    private String savePGSchema(Path schemaFile, SchemaGraph pgSchema) throws Exception {
        String outputFile = schemaFile.toString().replace(".ttl", "_pg_schema.json");
        JsonSchemaGenerator jsonGenerator = new JsonSchemaGenerator();
        String jsonSchema = jsonGenerator.generateJson(pgSchema);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(jsonSchema);
        }
        
        return outputFile;
    }

    private JsonNode createSchemaJson(SchemaGraph graph) {
        ObjectNode schemaJson = objectMapper.createObjectNode();
        schemaJson.set("nodes", createNodesJson(graph.getNodes()));
        schemaJson.set("relationships", createRelationshipsJson(graph.getEdges()));
        return schemaJson;
    }

    private JsonNode createNodesJson(Collection<SchemaNode> nodes) {
        ArrayNode nodesArray = objectMapper.createArrayNode();
        
        for (SchemaNode node : nodes) {
            ObjectNode nodeJson = objectMapper.createObjectNode();
            nodeJson.put("id", node.getId());
            
            // Add labels
            ArrayNode labels = objectMapper.createArrayNode();
            for (String label : node.getLabels()) {
                labels.add(label);
            }
            nodeJson.set("labels", labels);
            
            // Add properties
            ObjectNode properties = objectMapper.createObjectNode();
            Map<String, Object> nodeProps = node.getProperties();
            
            // First pass: collect base properties and their types
            for (Map.Entry<String, Object> entry : nodeProps.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (!key.contains("_minCount") && !key.contains("_maxCount") && 
                    !key.equals("minCount") && !key.equals("maxCount")) {
                    ObjectNode propertyDetails = objectMapper.createObjectNode();
                    propertyDetails.put("type", String.valueOf(value));
                    properties.set(key, propertyDetails);
                }
            }
            
            // Second pass: add cardinality to properties
            for (String propName : nodeProps.keySet()) {
                if (properties.has(propName)) {
                    ObjectNode propDetails = (ObjectNode) properties.get(propName);
                    
                    // Check for minCount
                    Object minCount = nodeProps.get(propName + "_minCount");
                    if (minCount != null) {
                        try {
                            propDetails.put("minCount", Integer.parseInt(String.valueOf(minCount)));
                        } catch (NumberFormatException e) {
                            // Skip invalid numbers
                        }
                    }
                    
                    // Check for maxCount
                    Object maxCount = nodeProps.get(propName + "_maxCount");
                    if (maxCount != null) {
                        try {
                            propDetails.put("maxCount", Integer.parseInt(String.valueOf(maxCount)));
                        } catch (NumberFormatException e) {
                            // Skip invalid numbers
                        }
                    }
                }
            }
            
            nodeJson.set("properties", properties);
            nodesArray.add(nodeJson);
        }
        
        return nodesArray;
    }

    private JsonNode createRelationshipsJson(Collection<SchemaEdge> edges) {
        ArrayNode relationships = objectMapper.createArrayNode();
        
        System.out.println("\nDEBUG: SchemaTransformationService - Creating relationships JSON");
        System.out.println("Total edges to process: " + edges.size());
        
        for (SchemaEdge edge : edges) {
            System.out.println("\nProcessing edge: " + edge.getLabel());
            System.out.println("Property constraints: " + edge.getPropertyConstraints().size());
            
            ObjectNode relationship = objectMapper.createObjectNode();
            relationship.put("type", edge.getType().toLowerCase());
            relationship.put("source", edge.getSource().getLabels().iterator().next());
            relationship.put("target", edge.getTarget().getLabels().iterator().next());
            
            // Add relationship properties
            ObjectNode properties = objectMapper.createObjectNode();
            
            // Add property constraints as properties
            edge.getPropertyConstraints().forEach((key, constraint) -> {
                System.out.println("Adding property: " + key);
                ObjectNode propertyDetails = objectMapper.createObjectNode();
                propertyDetails.put("type", constraint.getDataType());
                
                if (constraint.getMinCardinality() > 0) {
                    propertyDetails.put("minCount", constraint.getMinCardinality());
                }
                if (constraint.getMaxCardinality() != -1) {
                    propertyDetails.put("maxCount", constraint.getMaxCardinality());
                }
                
                properties.set(key, propertyDetails);
            });
            
            // Add relationship cardinality
            if (edge.hasProperty("minCount")) {
                relationship.put("minCount", edge.getProperty("minCount").toString());
            }
            if (edge.hasProperty("maxCount")) {
                relationship.put("maxCount", edge.getProperty("maxCount").toString());
            }
            
            relationship.set("properties", properties);
            relationships.add(relationship);
        }
        
        return relationships;
    }

    private void writeJsonToFile(JsonNode json, Path outputPath) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), json);
    }

    /**
     * Transform the schema and generate a visualization image
     * 
     * @param schemaFile Path to the schema file
     * @param outputImageFile Path to the output image file (png and svg formats are fully supported)
     * @return Path to the generated image file
     * @throws SchemaTransformationException If transformation or visualization fails
     */
    public String transformSchemaWithImage(Path schemaFile, String outputImageFile) throws SchemaTransformationException {
        try {
            System.out.println("Starting transformation for schema with image output: " + schemaFile);
            
            // Step 1: RDF to RDF Statement Graph
            SchemaGraph rdfStatementGraph = rdfTransformer.transformToStatementGraph(
                schemaFile.toAbsolutePath().normalize().toString());
            validateGraph(rdfStatementGraph, "RDF Statement Graph");
            
            // Step 2: RDF Statement Graph to PG Statement Graph
            PGSchemaToStatementTransformer pgTransformer = 
                new PGSchemaToStatementTransformer(rdfStatementGraph);
            SchemaGraph pgStatementGraph = pgTransformer.transformToStatementGraph();
            validateGraph(pgStatementGraph, "PG Statement Graph");
            
            // Step 3: PG Statement Graph to PG Schema
            PGStatementToSchemaTransformer schemaTransformer = 
                new PGStatementToSchemaTransformer(pgStatementGraph);
            SchemaGraph pgSchema = schemaTransformer.transformToPGSchema();
            validateGraph(pgSchema, "PG Schema");
            
            // Generate visualization image
            return visualizeSchemaAsImage(schemaFile, pgSchema, outputImageFile);
            
        } catch (Exception e) {
            throw new SchemaTransformationException(
                "Failed to transform schema and generate image: " + schemaFile, schemaFile, e);
        }
    }

    /**
     * Visualize the schema as an image file
     * 
     * @param schemaFile Original schema file path
     * @param pgSchema The property graph schema
     * @param outputImageFile Path to output image file (png and svg formats are fully supported)
     * @return Path to the generated image file
     * @throws Exception If visualization fails
     */
    public String visualizeSchemaAsImage(Path schemaFile, SchemaGraph pgSchema, String outputImageFile) throws Exception {
        try {
            System.out.println("Generating visualization image for schema: " + schemaFile);
            
            // Create the visualizer
            GraphVisualizer visualizer = new GraphVisualizer();
            String imageFile = visualizer.generateImageFromSchemaGraph(pgSchema, outputImageFile);
            
            System.out.println("Schema visualization image created: " + imageFile);
            return imageFile;
        } catch (Exception e) {
            throw new SchemaTransformationException(
                "Failed to visualize schema as image: " + e.getMessage(), 
                schemaFile, e);
        }
    }
}