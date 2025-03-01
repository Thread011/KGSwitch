package com.kgswitch.transforms.pg;

import com.kgswitch.models.graph.*;
import com.kgswitch.models.constraints.PropertyConstraint;

import java.util.*;

public class PGStatementToSchemaTransformer {
    private final SchemaGraph statementGraph;
    private Map<String, SchemaNode> nodeMap;

    public PGStatementToSchemaTransformer(SchemaGraph statementGraph) {
        this.statementGraph = statementGraph;
        this.nodeMap = new HashMap<>();
    }

    public SchemaGraph transformToPGSchema() {
        SchemaGraph pgSchema = new SchemaGraph("pgschema");
        
        System.out.println("Starting PG Schema transformation with " + statementGraph.getNodes().size() + " nodes");
        
        processTypeStatements(pgSchema);
        System.out.println("After type statements: " + pgSchema.getNodes().size() + " nodes");
        
        processPropertyStatements();
        System.out.println("After property statements: " + pgSchema.getNodes().size() + " nodes");
        
        processRelationshipStatements(pgSchema);
        System.out.println("After relationship statements: " + pgSchema.getNodes().size() + " nodes");
        
        System.out.println("Final node map size: " + nodeMap.size());
        System.out.println("Final schema nodes: " + pgSchema.getNodes().size());
        
        return pgSchema;
    }

    private void processTypeStatements(SchemaGraph pgSchema) {
        for (SchemaNode statement : statementGraph.getNodes()) {
            // Debug the statement content
            System.out.println("Processing statement: " + statement.getId());
            System.out.println("Labels: " + statement.getLabels());
            System.out.println("Properties: " + statement.getProperties());

            // The issue might be here - check if we're correctly identifying TypeStatements
            if (statement.getLabels().contains("TypeStatement") || 
                statement.hasProperty("type")) {  // Add this condition
                String subject = statement.getProperties().get("subject").toString();
                String type = statement.getProperties().get("object").toString();
                
                // Remove http://schema.org/ prefix from the type
                type = removePrefix(type);
                
                SchemaNode node = nodeMap.computeIfAbsent(subject, k -> {
                    SchemaNode newNode = new SchemaNode(k);
                    pgSchema.addNode(newNode);  // Make sure this is called
                    return newNode;
                });
                
                node.addLabel(type);
                System.out.println("Created node: " + node.getId() + " with label: " + type);
            }
        }
    }
    
    // Helper method to remove prefixes from URIs
    private String removePrefix(String uri) {
        if (uri.startsWith("http://schema.org/")) {
            return uri.substring("http://schema.org/".length());
        }
        
        if (uri.contains("#")) {
            return uri.substring(uri.lastIndexOf("#") + 1);
        }
        
        if (uri.contains("/")) {
            return uri.substring(uri.lastIndexOf("/") + 1);
        }
        
        return uri;
    }

    private void processPropertyStatements() {
        for (SchemaNode statement : statementGraph.getNodes()) {
            if (statement.getLabels().contains("PropertyStatement")) {
                String subject = statement.getProperties().get("subject").toString();
                String predicate = statement.getProperties().get("predicate").toString();
                String datatype = statement.getProperties().get("datatype").toString();
                
                // Remove prefix from predicate
                predicate = removePrefix(predicate);
                
                SchemaNode node = nodeMap.get(subject);
                if (node != null) {
                    PropertyConstraint constraint = new PropertyConstraint(predicate, datatype);
                    
                    int minCount = Integer.parseInt(
                        statement.getProperties().get("minCount").toString());
                    int maxCount = Integer.parseInt(
                        statement.getProperties().get("maxCount").toString());
                    constraint.setCardinality(minCount, maxCount);
                    
                    node.addPropertyConstraint(constraint);
                }
            }
        }
    }

    private void processRelationshipStatements(SchemaGraph pgSchema) {
        for (SchemaNode statement : statementGraph.getNodes()) {
            if (statement.getLabels().contains("EdgeStatement")) {
                String source = statement.getProperties().get("subject").toString();
                String target = statement.getProperties().get("object").toString();
                String relationship = statement.getProperties().get("predicate").toString();
                
                // Remove prefix from relationship type
                relationship = removePrefix(relationship);
                
                SchemaNode sourceNode = nodeMap.get(source);
                SchemaNode targetNode = nodeMap.get(target);
                
                if (sourceNode != null && targetNode != null) {
                    SchemaEdge edge = new SchemaEdge(
                        source + "_" + relationship + "_" + target,
                        sourceNode,
                        targetNode,
                        relationship
                    );

                    // Transfer property constraints from the statement
                    statement.getPropertyConstraints().forEach((key, constraint) -> {
                        System.out.println("Transferring property constraint from statement: " + key);
                        edge.addPropertyConstraint(constraint);
                    });

                    // Transfer cardinality if present
                    if (statement.getProperties().containsKey("minCount")) {
                        edge.addProperty("minCount", statement.getProperties().get("minCount").toString());
                    }
                    if (statement.getProperties().containsKey("maxCount")) {
                        edge.addProperty("maxCount", statement.getProperties().get("maxCount").toString());
                    }

                    // Look for related property statements that define relationship properties
                    for (SchemaNode propStatement : statementGraph.getNodes()) {
                        // Check if this is a property statement for a relationship
                        if (propStatement.getLabels().contains("PropertyStatement") &&
                            propStatement.getProperties().containsKey("subject") &&
                            propStatement.getProperties().get("subject").toString().equals(relationship)) {
                            
                            String propertyName = propStatement.getProperties().get("predicate").toString();
                            // Remove prefix from property name
                            propertyName = removePrefix(propertyName);
                            
                            String dataType = propStatement.getProperties().get("datatype").toString();
                            
                            System.out.println("Found relationship property statement: " + propertyName);
                            
                            PropertyConstraint constraint = new PropertyConstraint(propertyName, dataType);
                            
                            // Set cardinality if present
                            if (propStatement.getProperties().containsKey("minCount") && 
                                propStatement.getProperties().containsKey("maxCount")) {
                                int minCount = Integer.parseInt(propStatement.getProperties().get("minCount").toString());
                                int maxCount = Integer.parseInt(propStatement.getProperties().get("maxCount").toString());
                                constraint.setCardinality(minCount, maxCount);
                            }
                            
                            edge.addPropertyConstraint(constraint);
                            System.out.println("Added property constraint to edge: " + propertyName);
                        }
                        
                        // NEW CODE: Also check for property statements that might be related to this relationship
                        // This handles the case where relationship properties are defined with a compound name
                        // like "relationshipName_propertyName"
                        if (propStatement.getLabels().contains("PropertyStatement") &&
                            propStatement.getProperties().containsKey("predicate") &&
                            propStatement.getProperties().get("predicate").toString().startsWith(relationship + "_")) {
                            
                            String propertyName = propStatement.getProperties().get("predicate").toString();
                            // Remove prefix from property name
                            propertyName = removePrefix(propertyName);
                            
                            String dataType = propStatement.getProperties().get("datatype").toString();
                            
                            System.out.println("Found compound relationship property: " + propertyName);
                            
                            PropertyConstraint constraint = new PropertyConstraint(propertyName, dataType);
                            
                            // Set cardinality if present
                            if (propStatement.getProperties().containsKey("minCount") && 
                                propStatement.getProperties().containsKey("maxCount")) {
                                int minCount = Integer.parseInt(propStatement.getProperties().get("minCount").toString());
                                int maxCount = Integer.parseInt(propStatement.getProperties().get("maxCount").toString());
                                constraint.setCardinality(minCount, maxCount);
                            }
                            
                            edge.addPropertyConstraint(constraint);
                            System.out.println("Added compound property constraint to edge: " + propertyName);
                        }
                    }
                    
                    pgSchema.addEdge(edge);
                    System.out.println("Added edge with " + edge.getPropertyConstraints().size() + 
                                     " property constraints: " + edge.getPropertyConstraints().keySet());
                }
            }
        }
    }
}