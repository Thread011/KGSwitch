package com.kgswitch.transforms.pg;

import com.kgswitch.models.constraints.PropertyConstraint;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;

import java.util.*;

public class PGSchemaTransformer {
    private final SchemaGraph statementGraph;
    
    public PGSchemaTransformer(SchemaGraph statementGraph) {
        this.statementGraph = statementGraph;
    }
    
    public SchemaGraph transformToPGSchema() {
        SchemaGraph pgSchema = new SchemaGraph("pg");
        Map<String, SchemaNode> nodeMap = new HashMap<>();

        // Process nodes
        for (SchemaNode node : statementGraph.getNodes()) {
            // Process statement nodes that represent type definitions
            if (hasTypeDefinition(node)) {
                SchemaNode pgNode = createPGNode(node);
                pgSchema.addNode(pgNode);
                nodeMap.put(node.getId(), pgNode);
                System.out.println("Created PG node: " + pgNode.getId() + 
                                 " with labels: " + pgNode.getLabels());
            }
        }

        // Process relationships
        for (SchemaEdge edge : statementGraph.getEdges()) {
            if (isValidRelationship(edge)) {
                SchemaNode source = nodeMap.get(edge.getSource().getId());
                SchemaNode target = nodeMap.get(edge.getTarget().getId());
                
                if (source != null && target != null) {
                    String normalizedLabel = normalizeRelationshipType(edge.getLabel());
                    SchemaEdge pgEdge = new SchemaEdge(
                        edge.getId(),
                        source,
                        target,
                        normalizedLabel
                    );
                    
                    // Copy properties
                    edge.getProperties().forEach(pgEdge::addProperty);
                    pgSchema.addEdge(pgEdge);
                    
                    System.out.println("Created PG edge: " + pgEdge.getLabel() + 
                                     " between " + source.getId() + " and " + target.getId());
                }
            }
        }
        
        return pgSchema;
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
}