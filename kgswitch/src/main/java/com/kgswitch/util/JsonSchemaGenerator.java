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
                        ObjectNode propertyObj = propsObj.putObject(propertyName);
                        propertyObj.put("type", getSimpleDataType(constraint.getDataType()));
                        
                        // Add cardinality if present
                        if (constraint.getMinCardinality() > 0) {
                            propertyObj.put("minCount", constraint.getMinCardinality());
                        }
                        if (constraint.getMaxCardinality() != -1) {
                            propertyObj.put("maxCount", constraint.getMaxCardinality());
                        }
                    }
                }
            }
            
            // Create relationships array
            ArrayNode relsArray = rootNode.putArray("relationships");
            relsArray.addAll(generateRelationships(schema));
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            
        } catch (Exception e) {
            System.err.println("Error generating JSON: " + e.getMessage());
            return "{}";
        }
    }
    
    private ArrayNode generateRelationships(SchemaGraph schema) {
        ArrayNode relationships = mapper.createArrayNode();
        
        for (SchemaEdge edge : schema.getEdges()) {
            ObjectNode relationship = mapper.createObjectNode();
            relationship.put("type", edge.getType().toLowerCase());
            relationship.put("source", edge.getSource().getLabels().iterator().next());
            relationship.put("target", edge.getTarget().getLabels().iterator().next());
            
            // Create properties object for relationship properties
            ObjectNode properties = mapper.createObjectNode();
            
            System.out.println("DEBUG: JsonSchemaGenerator - Processing relationship: " + edge.getType());
            System.out.println("Property constraints: " + edge.getPropertyConstraints());
            
            // Add property constraints to the properties object
            edge.getPropertyConstraints().forEach((key, constraint) -> {
                ObjectNode propertyDetails = mapper.createObjectNode();
                propertyDetails.put("type", getJsonType(constraint.getDataType()));
                
                if (constraint.getMinCardinality() > 0) {
                    propertyDetails.put("minCount", constraint.getMinCardinality());
                }
                if (constraint.getMaxCardinality() != -1) {
                    propertyDetails.put("maxCount", constraint.getMaxCardinality());
                }
                
                properties.set(key, propertyDetails);
            });
            
            if (edge.getPropertyConstraints().isEmpty()) {
                System.out.println("Warning: No property constraints found for relationship: " + edge.getType());
            }
            
            relationship.set("properties", properties);
            relationships.add(relationship);
        }
        
        return relationships;
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

    private String getJsonType(String dataType) {
        if (dataType == null) return "String";
        
        switch (dataType.toLowerCase()) {
            case "http://www.w3.org/2001/xmlschema#string":
                return "String";
            case "http://www.w3.org/2001/xmlschema#integer":
                return "Integer";
            case "http://www.w3.org/2001/xmlschema#boolean":
                return "Boolean";
            case "http://www.w3.org/2001/xmlschema#datetime":
                return "DateTime";
            case "http://www.w3.org/2001/xmlschema#date":
                return "Date";
            default:
                return "String";
        }
    }
} 