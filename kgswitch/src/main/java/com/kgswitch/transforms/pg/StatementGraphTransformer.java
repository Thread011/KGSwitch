package com.kgswitch.transforms.pg;

import com.kgswitch.models.constraints.PropertyConstraint;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;

import java.util.*;

public class StatementGraphTransformer {
    private SchemaGraph statementGraph;
    private Map<String, SchemaNode> nodeMap;

    public StatementGraphTransformer(SchemaGraph statementGraph) {
        this.statementGraph = statementGraph;
        this.nodeMap = new HashMap<>();
    }

    public SchemaGraph transformToPGSchema() {
        SchemaGraph pgSchema = new SchemaGraph("pg");
        System.out.println("Starting PG Schema transformation with " + 
                          statementGraph.getNodes().size() + " nodes");
        
        // Transform nodes
        for (SchemaNode originalNode : statementGraph.getNodes()) {
            System.out.println("Transforming node: " + originalNode.getId());
            System.out.println("  Labels: " + originalNode.getLabels());
            System.out.println("  Properties: " + originalNode.getProperties());
            
            SchemaNode pgNode = transformNode(originalNode);
            pgSchema.addNode(pgNode);
            nodeMap.put(originalNode.getId(), pgNode);
        }
    
        System.out.println("Created PG Schema with " + 
                          pgSchema.getNodes().size() + " nodes");

        // Transform edges
        for (SchemaEdge edge : statementGraph.getEdges()) {
            System.out.println("Transforming edge: " + edge.getId());
            SchemaNode sourceNode = nodeMap.get(edge.getSource().getId());
            SchemaNode targetNode = nodeMap.get(edge.getTarget().getId());
            
            if (sourceNode != null && targetNode != null) {
                SchemaEdge pgEdge = new SchemaEdge(
                    edge.getId(),
                    sourceNode,
                    targetNode,
                    edge.getLabel()
                );
                
                // Copy edge properties and constraints
                edge.getProperties().forEach(pgEdge::addProperty);
                
                // Copy property constraints
                edge.getPropertyConstraints().forEach((key, constraint) -> {
                    PropertyConstraint newConstraint = new PropertyConstraint(
                        constraint.getName(),
                        constraint.getDataType()
                    );
                    newConstraint.setCardinality(
                        constraint.getMinCardinality(),
                        constraint.getMaxCardinality()
                    );
                    pgEdge.addPropertyConstraint(newConstraint);
                    System.out.println("  Copied edge constraint: " + key);
                });
                
                pgSchema.addEdge(pgEdge);
                System.out.println("  Added edge: " + pgEdge.getId() + 
                                 " with " + pgEdge.getPropertyConstraints().size() + 
                                 " property constraints");
            }
        }
        
        return pgSchema;
    }

    private SchemaNode transformNode(SchemaNode originalNode) {
        SchemaNode pgNode = new SchemaNode(originalNode.getId());
        
        // Copy labels
        originalNode.getLabels().forEach(label -> {
            System.out.println("Copying label: " + label);
            pgNode.addLabel(label);
        });
        
        // Transform property constraints
        for (Map.Entry<String, PropertyConstraint> entry : 
             originalNode.getPropertyConstraints().entrySet()) {
            PropertyConstraint originalConstraint = entry.getValue();
            System.out.println("Copying constraint: " + entry.getKey());
            
            PropertyConstraint newConstraint = new PropertyConstraint(
                originalConstraint.getName(),
                originalConstraint.getDataType()
            );
            newConstraint.setCardinality(
                originalConstraint.getMinCardinality(),
                originalConstraint.getMaxCardinality()
            );
            pgNode.addPropertyConstraint(newConstraint);
        }
        
        // Copy properties
        originalNode.getProperties().forEach((key, value) -> {
            System.out.println("Copying property: " + key);
            pgNode.addProperty(key, value);
        });
        
        return pgNode;
    }
}