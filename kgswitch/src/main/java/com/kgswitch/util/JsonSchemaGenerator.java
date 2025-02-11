package com.kgswitch.util;

import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.constraints.PropertyConstraint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;

public class JsonSchemaGenerator {
    private final ObjectMapper mapper;
    
    public JsonSchemaGenerator() {
        this.mapper = new ObjectMapper();
    }
    
    public String generateJson(SchemaGraph schema) {
        try {
            ObjectNode rootNode = mapper.createObjectNode();
            
            // Create nodes array
            ArrayNode nodesArray = rootNode.putArray("nodes");
            for (SchemaNode node : schema.getNodes()) {
                if (!node.getLabels().isEmpty()) {
                    ObjectNode nodeObj = nodesArray.addObject();
                    
                    // Add label (using first label)
                    nodeObj.put("label", node.getLabels().iterator().next());
                    
                    // Add properties
                    ObjectNode propsObj = nodeObj.putObject("properties");
                    for (Map.Entry<String, PropertyConstraint> entry : 
                         node.getPropertyConstraints().entrySet()) {
                        String propertyName = entry.getKey();
                        PropertyConstraint constraint = entry.getValue();
                        String dataType = getSimpleDataType(constraint.getDataType());
                        propsObj.put(propertyName, dataType);
                    }
                }
            }
            
            // Create relationships array
            ArrayNode relsArray = rootNode.putArray("relationships");
            for (SchemaEdge edge : schema.getEdges()) {
                ObjectNode relObj = relsArray.addObject();
                
                // Add relationship type
                relObj.put("type", edge.getLabel());
                
                // Add source and target
                relObj.put("source", edge.getSource().getLabels().iterator().next());
                relObj.put("target", edge.getTarget().getLabels().iterator().next());
                
                // Add properties if any
                ObjectNode relProps = relObj.putObject("properties");
                Map<String, Object> edgeProps = edge.getProperties();
                for (Map.Entry<String, Object> entry : edgeProps.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("minCount") && !key.equals("maxCount")) {
                        Object value = entry.getValue();
                        if (value != null) {
                            relProps.put(key, value.toString());
                        }
                    }
                }
            }
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            
        } catch (Exception e) {
            System.err.println("Error generating JSON: " + e.getMessage());
            return "{}";
        }
    }
    
    private String getSimpleDataType(String fullDataType) {
        // Convert XML Schema datatypes to simple types
        if (fullDataType.contains("#")) {
            String type = fullDataType.substring(fullDataType.indexOf("#") + 1);
            switch (type.toLowerCase()) {
                case "string":
                    return "String";
                case "integer":
                case "int":
                    return "Integer";
                case "float":
                case "double":
                    return "Float";
                case "boolean":
                    return "Boolean";
                case "date":
                    return "Date";
                case "datetime":
                    return "DateTime";
                default:
                    return "String";
            }
        }
        return "String";
    }
} 