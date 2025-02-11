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
            SchemaNode sourceNode = nodeMap.get(edge.getSource().getId());
            SchemaNode targetNode = nodeMap.get(edge.getTarget().getId());
            
            if (sourceNode != null && targetNode != null) {
                SchemaEdge pgEdge = new SchemaEdge(
                    edge.getId(),
                    sourceNode,
                    targetNode,
                    edge.getLabel()
                );
                
                // Copy edge properties
                edge.getProperties().forEach(pgEdge::addProperty);
                pgSchema.addEdge(pgEdge);
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
            PropertyConstraint constraint = entry.getValue();
            System.out.println("Copying constraint: " + entry.getKey());
            pgNode.addPropertyConstraint(new PropertyConstraint(
                constraint.getName(),
                constraint.getDataType()
            ));
        }
        
        // Copy properties
        originalNode.getProperties().forEach((key, value) -> {
            System.out.println("Copying property: " + key);
            pgNode.addProperty(key, value);
        });
        
        return pgNode;
    }
}