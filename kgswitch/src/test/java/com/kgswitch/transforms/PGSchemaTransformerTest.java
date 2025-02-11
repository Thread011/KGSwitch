package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.transforms.pg.PGSchemaTransformer;

class PGSchemaTransformerTest {
    private PGSchemaTransformer transformer;
    private SchemaGraph inputGraph;

    @BeforeEach
    void setUp() {
        transformer = new PGSchemaTransformer();
        inputGraph = new SchemaGraph("test");
    }

    @Test
    void testTransformToPGSchema() {
        // Create test nodes
        SchemaNode typeStatement = new SchemaNode("type_stmt_Person");
        typeStatement.addLabel("TypeStatement");
        typeStatement.addProperty("predicate", "type");
        typeStatement.addProperty("subject", "Person");
        typeStatement.addProperty("object", "http://schema.org/Person");
        inputGraph.addNode(typeStatement);

        SchemaNode propertyStatement = new SchemaNode("prop_stmt_Person_name");
        propertyStatement.addLabel("PropertyStatement");
        propertyStatement.addProperty("predicate", "name");
        propertyStatement.addProperty("subject", "Person");
        propertyStatement.addProperty("datatype", "http://www.w3.org/2001/XMLSchema#string");
        propertyStatement.addProperty("minCount", "1");
        propertyStatement.addProperty("maxCount", "1");
        inputGraph.addNode(propertyStatement);

        // Transform and verify
        SchemaGraph result = transformer.transformToPGSchema(inputGraph);
        
        assertEquals("pg", result.getNamespace());
        assertTrue(result.getNodes().stream()
            .anyMatch(node -> node.getId().equals("Person")));
        
        SchemaNode personNode = result.getNodes().stream()
            .filter(node -> node.getId().equals("Person"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(personNode);
        assertTrue(personNode.getLabels().contains("http://schema.org/Person"));
        assertTrue(personNode.getPropertyConstraints().containsKey("name"));
    }

    @Test
    void testTransformWithRelationship() {
        // Create nodes for relationship test
        SchemaNode typeStatement1 = new SchemaNode("type_stmt_Person");
        typeStatement1.addLabel("TypeStatement");
        typeStatement1.addProperty("predicate", "type");
        typeStatement1.addProperty("subject", "Person");
        typeStatement1.addProperty("object", "http://schema.org/Person");
        inputGraph.addNode(typeStatement1);

        SchemaNode typeStatement2 = new SchemaNode("type_stmt_Organization");
        typeStatement2.addLabel("TypeStatement");
        typeStatement2.addProperty("predicate", "type");
        typeStatement2.addProperty("subject", "Organization");
        typeStatement2.addProperty("object", "http://schema.org/Organization");
        inputGraph.addNode(typeStatement2);

        SchemaNode edgeStatement = new SchemaNode("rel_Person_memberOf");
        edgeStatement.addLabel("EdgeStatement");
        edgeStatement.addProperty("predicate", "memberOf");
        edgeStatement.addProperty("subject", "Person");
        edgeStatement.addProperty("object", "Organization");
        edgeStatement.addProperty("minCount", "0");
        edgeStatement.addProperty("maxCount", "1");
        inputGraph.addNode(edgeStatement);

        // Transform and verify
        SchemaGraph result = transformer.transformToPGSchema(inputGraph);
        
        assertFalse(result.getEdges().isEmpty());
        assertEquals(1, result.getEdges().size());
        
        assertTrue(result.getEdges().stream()
            .anyMatch(edge -> edge.getType().equals("MEMBEROF")));
    }

    @Test
    void testTransformWithPropertyConstraints() {
        // Create test nodes with property constraints
        SchemaNode typeStatement = new SchemaNode("type_stmt_Person");
        typeStatement.addLabel("TypeStatement");
        typeStatement.addProperty("predicate", "type");
        typeStatement.addProperty("subject", "Person");
        typeStatement.addProperty("object", "http://schema.org/Person");
        inputGraph.addNode(typeStatement);

        SchemaNode propertyStatement = new SchemaNode("prop_stmt_Person_email");
        propertyStatement.addLabel("PropertyStatement");
        propertyStatement.addProperty("predicate", "email");
        propertyStatement.addProperty("subject", "Person");
        propertyStatement.addProperty("datatype", "http://www.w3.org/2001/XMLSchema#string");
        propertyStatement.addProperty("minCount", "1");
        propertyStatement.addProperty("maxCount", "1");
        inputGraph.addNode(propertyStatement);

        // Transform and verify
        SchemaGraph result = transformer.transformToPGSchema(inputGraph);
        
        SchemaNode personNode = result.getNodes().stream()
            .filter(node -> node.getId().equals("Person"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(personNode);
        assertTrue(personNode.getPropertyConstraints().containsKey("email"));
        
        // Compare cardinality values as integers
        assertEquals(1, personNode.getPropertyConstraints().get("email")
            .getMinCardinality());
        assertEquals(1, personNode.getPropertyConstraints().get("email")
            .getMaxCardinality());
        
        // Verify data type
        assertEquals("String", personNode.getPropertyConstraints().get("email")
            .getDataType());
    }
}