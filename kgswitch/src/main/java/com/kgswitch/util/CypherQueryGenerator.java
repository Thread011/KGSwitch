package com.kgswitch.util;

import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.constraints.PropertyConstraint;

import java.util.*;

public class CypherQueryGenerator {
    
    public List<String> generateQueries(SchemaGraph schema) {
        List<String> queries = new ArrayList<>();
        System.out.println("Generating Cypher queries for schema with " + 
                          schema.getNodes().size() + " nodes");

        // First create nodes with their properties
        for (SchemaNode node : schema.getNodes()) {
            generateNodeQueries(node, queries);
        }

        // Then create relationship constraints
        for (SchemaEdge edge : schema.getEdges()) {
            generateRelationshipQueries(edge, queries);
        }

        return queries;
    }

    private void generateNodeQueries(SchemaNode node, List<String> queries) {
        // Get the simple label without URI
        String label = node.getLabels().stream()
            .map(l -> l.substring(l.lastIndexOf("/") + 1))
            .findFirst()
            .orElse("");

        System.out.println("Generating constraints for node: " + node.getId());

        // Create node with required properties
        StringBuilder createQuery = new StringBuilder("CREATE (n:" + label + " {");
        List<String> props = new ArrayList<>();
        for (Map.Entry<String, PropertyConstraint> entry : 
             node.getPropertyConstraints().entrySet()) {
            props.add(entry.getKey() + ": \"\"");
        }
        createQuery.append(String.join(", ", props)).append("});");
        queries.add(createQuery.toString());

        // Generate property constraints
        for (Map.Entry<String, PropertyConstraint> entry : 
             node.getPropertyConstraints().entrySet()) {
            String property = entry.getKey();
            PropertyConstraint constraint = entry.getValue();

            // Required property constraint
            if (constraint.getMinCardinality() > 0) {
                String query = String.format(
                    "CREATE CONSTRAINT ON (n:%s) ASSERT exists(n.%s);",
                    label, property);
                System.out.println("Adding required constraint: " + query);
                queries.add(query);
            }

            // Unique property constraint
            if (constraint.getMaxCardinality() == 1) {
                String query = String.format(
                    "CREATE CONSTRAINT ON (n:%s) ASSERT n.%s IS UNIQUE;",
                    label, property);
                System.out.println("Adding unique constraint: " + query);
                queries.add(query);
            }
        }
    }

    private void generateRelationshipQueries(SchemaEdge edge, List<String> queries) {
        String sourceLabel = edge.getSource().getLabels().stream()
            .map(l -> l.substring(l.lastIndexOf("/") + 1))
            .findFirst()
            .orElse("");
        
        String targetLabel = edge.getTarget().getLabels().stream()
            .map(l -> l.substring(l.lastIndexOf("/") + 1))
            .findFirst()
            .orElse("");
        
        String relType = edge.getLabel().toUpperCase();
        
        // Create relationship pattern query
        String query = String.format(
            "MATCH (a:%s)-[r:%s]->(b:%s) RETURN r;",
            sourceLabel, relType, targetLabel);
        queries.add(query);
    }
}