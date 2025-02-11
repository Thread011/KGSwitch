package com.kgswitch.transforms.pg;

import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.constraints.PropertyConstraint;
import java.util.*;

public class PGSchemaToStatementTransformer {
    private final SchemaGraph pgSchema;
    private final SchemaGraph statementGraph;
    private final Map<String, SchemaNode> nodeStatements;
    
    public PGSchemaToStatementTransformer(SchemaGraph pgSchema) {
        this.pgSchema = pgSchema;
        this.statementGraph = new SchemaGraph("rdf");
        this.nodeStatements = new HashMap<>();
    }
    
    public SchemaGraph transformToStatementGraph() {
        // First create type statements for all nodes
        for (SchemaNode node : pgSchema.getNodes()) {
            createTypeStatements(node);
        }
        
        // Then create property statements
        for (SchemaNode node : pgSchema.getNodes()) {
            createPropertyStatements(node);
        }
        
        // Finally create relationship statements
        for (SchemaEdge edge : pgSchema.getEdges()) {
            processEdge(edge, statementGraph);
        }
        
        return statementGraph;
    }
    
    private void createTypeStatements(SchemaNode node) {
        for (String label : node.getLabels()) {
            SchemaNode typeStatement = new SchemaNode("type_stmt_" + node.getId());
            typeStatement.addLabel("TypeStatement");
            typeStatement.addProperty("subject", node.getId());
            typeStatement.addProperty("predicate", "type");
            typeStatement.addProperty("object", "http://schema.org/" + label);
            
            statementGraph.addNode(typeStatement);
            nodeStatements.put(node.getId(), typeStatement);
        }
    }
    
    private void createPropertyStatements(SchemaNode node) {
        for (Map.Entry<String, PropertyConstraint> entry : 
             node.getPropertyConstraints().entrySet()) {
            String propertyName = entry.getKey();
            PropertyConstraint constraint = entry.getValue();
            
            SchemaNode propertyStatement = new SchemaNode(
                "prop_stmt_" + node.getId() + "_" + propertyName);
            propertyStatement.addLabel("PropertyStatement");
            propertyStatement.addProperty("subject", node.getId());
            propertyStatement.addProperty("predicate", propertyName);
            propertyStatement.addProperty("datatype", constraint.getDataType());
            propertyStatement.addProperty("minCount", 
                String.valueOf(constraint.getMinCardinality()));
            propertyStatement.addProperty("maxCount", 
                String.valueOf(constraint.getMaxCardinality()));
            
            statementGraph.addNode(propertyStatement);
        }
    }
    
    private void processEdge(SchemaEdge edge, SchemaGraph statementGraph) {
        SchemaNode statement = new SchemaNode("rel_" + edge.getSource().getId() + "_" + edge.getType().toLowerCase());
        statement.addLabel("EdgeStatement");
        
        statement.addProperty("predicate", edge.getType().toLowerCase());
        statement.addProperty("subject", edge.getSource().getId());
        statement.addProperty("object", edge.getTarget().getId());
        
        if (edge.hasProperty("minCount")) {
            statement.addProperty("minCount", edge.getProperty("minCount").toString());
        }
        if (edge.hasProperty("maxCount")) {
            statement.addProperty("maxCount", edge.getProperty("maxCount").toString());
        }
        
        // Transfer property constraints
        edge.getPropertyConstraints().forEach((key, constraint) -> {
            statement.addPropertyConstraint(constraint);
        });
        
        statementGraph.addNode(statement);
    }
}