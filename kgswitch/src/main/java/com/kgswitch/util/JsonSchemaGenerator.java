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
            for (SchemaEdge edge : schema.getEdges()) {
                ObjectNode relObj = relsArray.addObject();
                
                // Add relationship type
                relObj.put("type", edge.getLabel());
                
                // Add source and target
                relObj.put("source", edge.getSource().getLabels().iterator().next());
                relObj.put("target", edge.getTarget().getLabels().iterator().next());
                
                // Add properties
                addRelationshipProperties(relObj, edge);
                
                // Add relationship cardinality if present
                if (edge.getProperties().containsKey("minCount")) {
                    relObj.put("minCount", edge.getProperties().get("minCount").toString());
                }
                if (edge.getProperties().containsKey("maxCount")) {
                    relObj.put("maxCount", edge.getProperties().get("maxCount").toString());
                }
            }
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            
        } catch (Exception e) {
            System.err.println("Error generating JSON: " + e.getMessage());
            return "{}";
        }
    }
    
    private void addRelationshipProperties(ObjectNode relationshipNode, SchemaEdge edge) {
        ObjectNode properties = mapper.createObjectNode();
        
        // Add properties from edge's property constraints
        for (Map.Entry<String, PropertyConstraint> entry : edge.getPropertyConstraints().entrySet()) {
            String propertyName = entry.getKey();
            PropertyConstraint constraint = entry.getValue();
            
            ObjectNode propertyNode = mapper.createObjectNode();
            propertyNode.put("type", getSimpleDataType(constraint.getDataType()));
            
            if (constraint.getMinCardinality() > 0) {
                propertyNode.put("minCount", constraint.getMinCardinality());
            }
            if (constraint.getMaxCardinality() != -1) {
                propertyNode.put("maxCount", constraint.getMaxCardinality());
            }
            
            properties.set(propertyName, propertyNode);
        }
        
        // Add relationship cardinality if present
        if (edge.hasProperty("minCount")) {
            relationshipNode.put("minCount", edge.getProperty("minCount").toString());
        }
        if (edge.hasProperty("maxCount")) {
            relationshipNode.put("maxCount", edge.getProperty("maxCount").toString());
        }
        
        relationshipNode.set("properties", properties);
        
        // Debug output
        if (edge.getPropertyConstraints().isEmpty()) {
            System.out.println("Warning: No property constraints found for relationship: " + edge.getLabel());
        } else {
            System.out.println("Added " + edge.getPropertyConstraints().size() + 
                             " properties to relationship: " + edge.getLabel());
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