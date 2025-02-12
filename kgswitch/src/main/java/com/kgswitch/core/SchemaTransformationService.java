package com.kgswitch.core;

import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.transforms.rdf.RDFSchemaTransformer;
import com.kgswitch.transforms.pg.PGSchemaToStatementTransformer;
import com.kgswitch.transforms.pg.PGStatementToSchemaTransformer;
import com.kgswitch.transforms.rdf.StatementToRDFTransformer;
import com.kgswitch.util.CypherQueryGenerator;
import com.kgswitch.util.JsonSchemaGenerator;
import org.apache.jena.rdf.model.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

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
    
    public void transformSchema(Path schemaFile) throws SchemaTransformationException {
        try {
            System.out.println("Starting transformation for schema: " + schemaFile);
            
            // Step 1: RDF to RDF Statement Graph
            SchemaGraph rdfStatementGraph = rdfTransformer.transformToStatementGraph(
                schemaFile.toString());
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
            savePGSchema(schemaFile, pgSchema);
            
            System.out.println("Transformation completed successfully. Files should be created.");
            
        } catch (Exception e) {
            System.err.println("Transformation failed: " + e.getMessage());
            throw new SchemaTransformationException(
                "Failed to transform schema: " + schemaFile, schemaFile, e);
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
    
    private void savePGSchema(Path schemaFile, SchemaGraph pgSchema) throws Exception {
        String outputFile = schemaFile.toString().replace(".ttl", "_pg_schema.json");
        JsonSchemaGenerator jsonGenerator = new JsonSchemaGenerator();
        String jsonSchema = jsonGenerator.generateJson(pgSchema);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(jsonSchema);
        }
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
}