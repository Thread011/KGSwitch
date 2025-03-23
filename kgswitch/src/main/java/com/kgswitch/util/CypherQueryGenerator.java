package com.kgswitch.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to generate Neo4j Cypher queries from JSON schema files.
 * This enables visualization of property graph schemas in Neo4j.
 */
public class CypherQueryGenerator {
    
    private final ObjectMapper objectMapper;
    
    public CypherQueryGenerator() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate Cypher queries from a JSON schema file
     * 
     * @param jsonSchemaFile Path to the JSON schema file
     * @return String containing the Cypher queries
     * @throws IOException If file cannot be read or parsed
     */
    public String generateCypherFromFile(String jsonSchemaFile) throws IOException {
        String jsonContent = Files.readString(Paths.get(jsonSchemaFile));
        return generateCypherFromJson(jsonContent);
    }
    
    /**
     * Generate Cypher queries from a JSON string
     * 
     * @param jsonSchema JSON schema as a string
     * @return String containing the Cypher queries
     * @throws IOException If JSON cannot be parsed
     */
    public String generateCypherFromJson(String jsonSchema) throws IOException {
        JsonNode root = objectMapper.readTree(jsonSchema);
        
        StringBuilder cypher = new StringBuilder();
        
        cypher.append("// Uncomment to clear the database before import\n");
        cypher.append("MATCH (n) DETACH DELETE n;\n\n");
        
        // Generate node creation queries
        cypher.append("// Create nodes\n");
        Map<String, List<String>> nodeIdsByLabel = generateNodeQueries(root.get("nodes"), cypher);
        cypher.append("\n");
        
        // Generate relationship creation queries
        cypher.append("// Create relationships\n");
        generateRelationshipQueries(root.get("relationships"), nodeIdsByLabel, cypher);
        
        // Add Neo4j Browser styling commands at the end
        cypher.append("\n// Set Neo4j Browser styling to use node colors\n");
        cypher.append("// Note: these commands require APOC to be installed in Neo4j\n");
        cypher.append("// If you don't have APOC, you can still see the colors by manually setting\n");
        cypher.append("// the browser style in Neo4j Browser: `:style node {color: color, caption: displayName}`\n\n");
        
        cypher.append("// Try to set browser styling (may not work in all Neo4j versions)\n");
        cypher.append("CALL apoc.meta.graphSample(100)\n");
        cypher.append("YIELD nodes, relationships\n");
        cypher.append("RETURN 'BROWSER STYLE: node {color: color, caption: displayName}' as style;\n\n");
        
        // Alternative approach for different Neo4j versions
        cypher.append("// Alternative styling approach (will fail gracefully if not supported)\n");
        cypher.append("MATCH (n) WHERE n.color IS NOT NULL\n");
        cypher.append("WITH n LIMIT 1\n");
        cypher.append("CALL db.createNodeKey('color');\n\n");
        
        return cypher.toString();
    }
    
    /**
     * Generate Cypher queries for nodes
     */
    private Map<String, List<String>> generateNodeQueries(JsonNode nodes, StringBuilder cypher) {
        Map<String, List<String>> nodeIdsByLabel = new HashMap<>();
        Set<String> processedLabels = new HashSet<>();
        
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            cypher.append("// No nodes found in schema\n");
            return nodeIdsByLabel;
        }
        
        // Define a list of distinct colors
        List<String> nodeColors = List.of(
            "#FF5733", // Red
            "#33A1FF", // Blue
            "#33FF57", // Green
            "#9133FF", // Purple
            "#FFDD33", // Yellow
            "#FF33A1", // Pink
            "#33FFDD", // Teal
            "#A1FF33", // Lime
            "#FF8333", // Orange
            "#8333FF"  // Indigo
        );
        int colorIndex = 0;
        
        // First pass: collect all nodes by label to ensure representation
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            if (!node.has("label")) {
                continue; // Skip nodes without labels
            }
            
            String label = node.get("label").asText();
            
            // Skip if we already created a node with this label
            if (processedLabels.contains(label)) {
                continue;
            }
            
            processedLabels.add(label);
            String nodeId = sanitizeId(label);
            
            // Track node ID by label for relationship creation
            nodeIdsByLabel.computeIfAbsent(label, k -> new ArrayList<>()).add(nodeId);
            
            StringBuilder propertiesStr = new StringBuilder();
            
            // Add name property with the label as its value
            propertiesStr.append("name: '").append(label).append("'");
            
            // Add a property for display (will help with Neo4j Browser visualization)
            propertiesStr.append(", displayName: '").append(label).append("'");
            
            // Add a label property to help with identification 
            propertiesStr.append(", label: '").append(label).append("'");
            
            // Assign a color to this node (cycling through the color list)
            String color = nodeColors.get(colorIndex % nodeColors.size());
            colorIndex++;
            propertiesStr.append(", color: '").append(color).append("'");
            
            JsonNode properties = node.get("properties");
            if (properties != null && properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String propName = entry.getKey();
                    JsonNode propDetails = entry.getValue();
                    
                    // Skip internal or special properties
                    if (propName.startsWith("_") || propName.equals("id") || propName.equals("name") || 
                        propName.equals("label") || propName.equals("displayName")) {
                        continue;
                    }
                    
                    String propType = propDetails.has("type") ? propDetails.get("type").asText() : "String";
                    
                    if (propertiesStr.length() > 0) {
                        propertiesStr.append(", ");
                    }
                    
                    // Add property with its type (without constraints)
                    propertiesStr.append(sanitizeId(propName)).append(": '").append(propType).append("'");
                }
            }
            
            // Create Cypher query for this node
            cypher.append("CREATE (").append(nodeId).append(":").append(label);
            
            if (propertiesStr.length() > 0) {
                cypher.append(" {").append(propertiesStr).append("}");
            }
            
            cypher.append(");\n");
        }
        
        return nodeIdsByLabel;
    }
    
    /**
     * Generate Cypher queries for relationships
     */
    private void generateRelationshipQueries(JsonNode relationships, Map<String, List<String>> nodeIdsByLabel, StringBuilder cypher) {
        if (relationships == null || !relationships.isArray() || relationships.isEmpty()) {
            cypher.append("// No relationships found in schema\n");
            return;
        }
        
        // Track processed relationship types to avoid duplicates
        Set<String> processedRelTypes = new HashSet<>();
        
        for (int i = 0; i < relationships.size(); i++) {
            JsonNode rel = relationships.get(i);
            String type = rel.get("type").asText();
            String sourceLabel = rel.get("source").asText();
            String targetLabel = rel.get("target").asText();
            
            // Create a unique key for this relationship direction
            String relKey = sourceLabel + "-" + type + "-" + targetLabel;
            
            // Skip if we've already processed this relationship type between these labels
            if (processedRelTypes.contains(relKey)) {
                continue;
            }
            
            processedRelTypes.add(relKey);
            
            // Build properties string if any
            StringBuilder propertiesStr = new StringBuilder();
            
            // Add relationship type as a name property
            propertiesStr.append("name: '").append(type).append("'");
            
            JsonNode properties = rel.get("properties");
            if (properties != null && properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String propName = entry.getKey();
                    JsonNode propDetails = entry.getValue();
                    
                    String propType = propDetails.has("type") ? propDetails.get("type").asText() : "String";
                    
                    if (propertiesStr.length() > 0) {
                        propertiesStr.append(", ");
                    }
                    
                    // Add property with its type (without constraints)
                    propertiesStr.append(propName).append(": '").append(propType).append("'");
                }
            }
            
            // Make sure the source and target labels exist in the schema
            if (!nodeIdsByLabel.containsKey(sourceLabel) || !nodeIdsByLabel.containsKey(targetLabel)) {
                cypher.append("// Skipping relationship ").append(type)
                      .append(" from ").append(sourceLabel)
                      .append(" to ").append(targetLabel)
                      .append(" - one or both labels not found in schema\n");
                continue;
            }
            
            // Create the relationship between the nodes with specified labels
            // Ensure relationship type is valid for Neo4j by removing spaces and special chars
            String safeType = type.replaceAll("[^a-zA-Z0-9_]", "_").toUpperCase();
            
            cypher.append("MATCH (a:").append(sourceLabel).append("), (b:").append(targetLabel).append(") ");
            cypher.append("CREATE (a)-[r:").append(safeType);
            
            if (propertiesStr.length() > 0) {
                cypher.append(" {").append(propertiesStr).append("}");
            }
            
            cypher.append("]->(b);\n");
        }
    }
    
    /**
     * Sanitize ID for use in Cypher queries
     */
    private String sanitizeId(String id) {
        // Replace spaces and non-alphanumeric chars with underscore
        String sanitized = id.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        
        // Ensure it doesn't start with a number
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "n_" + sanitized;
        }
        
        return sanitized;
    }
    
    /**
     * Writes Cypher queries to a file
     * 
     * @param cypherQueries The Cypher queries
     * @param outputFile Path to output file
     * @throws IOException If file cannot be written
     */
    public void writeCypherToFile(String cypherQueries, String outputFile) throws IOException {
        Files.writeString(Paths.get(outputFile), cypherQueries);
    }
} 