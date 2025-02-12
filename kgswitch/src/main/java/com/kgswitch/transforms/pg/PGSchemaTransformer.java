package com.kgswitch.transforms.pg;

import com.kgswitch.models.constraints.PropertyConstraint;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;

import java.util.*;

public class PGSchemaTransformer {
    public SchemaGraph transformToPGSchema(SchemaGraph statementGraph) {
        SchemaGraph pgSchema = new SchemaGraph("pg", "pg");
        
        // Process nodes first
        for (SchemaNode node : statementGraph.getNodes()) {
            if (node.getLabels().contains("TypeStatement")) {
                processTypeStatement(node, pgSchema);
            }
        }
        
        // Process property statements
        for (SchemaNode node : statementGraph.getNodes()) {
            if (node.getLabels().contains("PropertyStatement")) {
                processPropertyStatement(node, pgSchema);
            }
        }
        
        // Process edge statements
        for (SchemaNode node : statementGraph.getNodes()) {
            if (node.getLabels().contains("EdgeStatement")) {
                processEdgeStatement(node, pgSchema, statementGraph);
            }
        }
        
        return pgSchema;
    }

    private void processTypeStatement(SchemaNode statement, SchemaGraph pgSchema) {
        String subject = statement.getProperties().get("subject").toString();
        String object = statement.getProperties().get("object").toString();
        
        SchemaNode node = new SchemaNode(subject);
        node.addLabel(object);
        pgSchema.addNode(node);
        
        System.out.println("Created node: " + subject + " with label: " + object);
    }

    private void processPropertyStatement(SchemaNode statement, SchemaGraph pgSchema) {
        String subject = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        String datatype = statement.getProperties().get("datatype").toString();
        datatype = convertDataType(datatype);
        
        SchemaNode node = pgSchema.getNode(subject);
        if (node != null) {
            PropertyConstraint constraint = new PropertyConstraint(predicate, datatype);
            
            // Set cardinality if present
            if (statement.getProperties().containsKey("minCount") && 
                statement.getProperties().containsKey("maxCount")) {
                int minCount = Integer.parseInt(statement.getProperties().get("minCount").toString());
                int maxCount = Integer.parseInt(statement.getProperties().get("maxCount").toString());
                constraint.setCardinality(minCount, maxCount);
            }
            
            node.addPropertyConstraint(constraint);
        }
    }

    private void processEdgeStatement(SchemaNode statement, SchemaGraph pgSchema, SchemaGraph statementGraph) {
        String source = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        String target = statement.getProperties().get("object").toString();
        
        SchemaNode sourceNode = pgSchema.getNode(source);
        SchemaNode targetNode = pgSchema.getNode(target);

        if (sourceNode != null && targetNode != null) {
            System.out.println("\nDEBUG: Processing edge statement for: " + predicate);
            System.out.println("Statement Properties: " + statement.getProperties());
            System.out.println("Statement Property Constraints: " + statement.getPropertyConstraints());

            SchemaEdge edge = new SchemaEdge(
                source + "_" + predicate + "_" + target,
                sourceNode,
                targetNode,
                predicate.toUpperCase()
            );

            // Transfer property constraints from the statement
            statement.getPropertyConstraints().forEach((key, constraint) -> {
                System.out.println("Adding property constraint: " + key + " to edge: " + predicate);
                edge.addPropertyConstraint(constraint);
            });

            // Add cardinality for the relationship itself
            if (statement.getProperties().containsKey("minCount")) {
                edge.addProperty("minCount", statement.getProperties().get("minCount").toString());
            }
            if (statement.getProperties().containsKey("maxCount")) {
                edge.addProperty("maxCount", statement.getProperties().get("maxCount").toString());
            }

            // Look for related property statements that define relationship properties
            for (SchemaNode node : statementGraph.getNodes()) {
                if (node.getLabels().contains("PropertyStatement") && 
                    node.getProperties().containsKey("subject") &&
                    node.getProperties().get("subject").toString().equals(predicate)) {
                    
                    String propertyName = node.getProperties().get("predicate").toString();
                    String dataType = node.getProperties().get("datatype").toString();
                    
                    System.out.println("Found relationship property: " + propertyName + " for edge: " + predicate);
                    
                    PropertyConstraint constraint = new PropertyConstraint(propertyName, mapDataType(dataType));
                    if (node.getProperties().containsKey("minCount")) {
                        int minCount = Integer.parseInt(node.getProperties().get("minCount").toString());
                        int maxCount = node.getProperties().containsKey("maxCount") ? 
                            Integer.parseInt(node.getProperties().get("maxCount").toString()) : -1;
                        constraint.setCardinality(minCount, maxCount);
                    }
                    
                    edge.addPropertyConstraint(constraint);
                }
            }
            
            pgSchema.addEdge(edge);
            System.out.println("Added edge: " + edge.getLabel() + " with " + 
                             edge.getPropertyConstraints().size() + " property constraints");
        }
    }

    private boolean hasTypeDefinition(SchemaNode node) {
        return !node.getLabels().isEmpty() && 
               node.getLabels().stream()
                   .anyMatch(label -> label.contains("schema.org"));
    }

    private SchemaNode createPGNode(SchemaNode statementNode) {
        SchemaNode pgNode = new SchemaNode(statementNode.getId());
        
        // Transform labels (remove URI prefix)
        statementNode.getLabels().stream()
            .map(label -> label.substring(label.lastIndexOf("/") + 1))
            .forEach(pgNode::addLabel);
        
        // Transform property constraints
        for (Map.Entry<String, PropertyConstraint> entry : 
             statementNode.getPropertyConstraints().entrySet()) {
            PropertyConstraint pc = entry.getValue();
            PropertyConstraint newPc = new PropertyConstraint(
                entry.getKey(),
                mapDataType(pc.getDataType())
            );
            newPc.setCardinality(pc.getMinCardinality(), pc.getMaxCardinality());
            pgNode.addPropertyConstraint(newPc);
        }
        
        // Copy properties
        statementNode.getProperties().forEach(pgNode::addProperty);
        
        return pgNode;
    }

    private boolean isValidRelationship(SchemaEdge edge) {
        return edge.hasProperty("type") &&
               "relationship".equals(edge.getProperties().get("type")) &&
               edge.getSource() != null &&
               edge.getTarget() != null;
    }

    private String normalizeRelationshipType(String label) {
        // Extract the last part of the URI if present
        String baseName = label;
        if (label.contains("/")) {
            baseName = label.substring(label.lastIndexOf("/") + 1);
        }
        
        // Convert camelCase to UPPER_CASE
        String upperCase = baseName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        
        // Clean up any remaining non-alphanumeric characters
        return upperCase.replaceAll("[^A-Z0-9_]", "_");
    }

    private String mapDataType(String rdfType) {
        switch (rdfType) {
            case "http://www.w3.org/2001/XMLSchema#string":
                return "String";
            case "http://www.w3.org/2001/XMLSchema#integer":
                return "Integer";
            case "http://www.w3.org/2001/XMLSchema#boolean":
                return "Boolean";
            default:
                return "String";
        }
    }

    private String convertDataType(String fullDataType) {
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