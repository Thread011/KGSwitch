package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.kgswitch.models.graph.*;
import com.kgswitch.models.constraints.PropertyConstraint;
import com.kgswitch.transforms.pg.*;

class PGSchemaTransformerTest {
    private PGSchemaTransformer transformer;
    private SchemaGraph inputGraph;

    @BeforeEach
    void setUp() {
        inputGraph = new SchemaGraph("test");
        transformer = new PGSchemaTransformer(inputGraph);
    }

    @Test
    void testTransformToPGSchema() {
        // Create a FlightReservation node
        SchemaNode flightNode = new SchemaNode("http://schema.org/FlightReservationShape");
        flightNode.addLabel("http://schema.org/FlightReservation");
        
        // Add property constraints for FlightReservation
        PropertyConstraint idConstraint = new PropertyConstraint("reservationId", 
            "http://www.w3.org/2001/XMLSchema#string");
        idConstraint.setCardinality(1, 1);
        flightNode.addPropertyConstraint(idConstraint);

        PropertyConstraint statusConstraint = new PropertyConstraint("reservationStatus", 
            "http://www.w3.org/2001/XMLSchema#string");
        statusConstraint.setCardinality(1, -1);
        flightNode.addPropertyConstraint(statusConstraint);

        inputGraph.addNode(flightNode);

        SchemaGraph result = transformer.transformToPGSchema();
        assertNotNull(result);
        assertEquals("pg", result.getNamespace());
        assertFalse(result.getNodes().isEmpty());
        
        // Verify transformed node
        SchemaNode transformedNode = result.getNodes().iterator().next();
        assertTrue(transformedNode.getLabels().contains("FlightReservation"));
        assertEquals(2, transformedNode.getPropertyConstraints().size());
    }

    @Test
    void testNodeTransformation() {
        // Create a Person node with properties
        SchemaNode personNode = new SchemaNode("http://schema.org/PersonShape");
        personNode.addLabel("http://schema.org/Person");
        
        // Add property constraints
        PropertyConstraint givenNameConstraint = new PropertyConstraint("givenName", 
            "http://www.w3.org/2001/XMLSchema#string");
        givenNameConstraint.setCardinality(1, -1);
        personNode.addPropertyConstraint(givenNameConstraint);

        PropertyConstraint emailConstraint = new PropertyConstraint("email", 
            "http://www.w3.org/2001/XMLSchema#string");
        emailConstraint.setCardinality(1, 1);
        personNode.addPropertyConstraint(emailConstraint);

        inputGraph.addNode(personNode);

        SchemaGraph result = transformer.transformToPGSchema();
        assertFalse(result.getNodes().isEmpty());
        
        SchemaNode transformedNode = result.getNodes().iterator().next();
        assertTrue(transformedNode.getLabels().contains("Person"));
        assertTrue(transformedNode.getPropertyConstraints().containsKey("email"));
        assertTrue(transformedNode.getPropertyConstraints().containsKey("givenName"));
    }

    @Test
    void testRelationshipTransformation() {
        // Create Person and FlightReservation nodes
        SchemaNode personNode = new SchemaNode("http://schema.org/PersonShape");
        personNode.addLabel("http://schema.org/Person");
        
        SchemaNode flightNode = new SchemaNode("http://schema.org/FlightReservationShape");
        flightNode.addLabel("http://schema.org/FlightReservation");
        
        // Create relationship
        SchemaEdge edge = new SchemaEdge(
            "rel_underName",
            flightNode,
            personNode,
            "underName"
        );
        edge.addProperty("type", "relationship");
        
        inputGraph.addNode(personNode);
        inputGraph.addNode(flightNode);
        inputGraph.addEdge(edge);

        SchemaGraph result = transformer.transformToPGSchema();
        assertFalse(result.getEdges().isEmpty());
        
        // Verify relationship
        SchemaEdge transformedEdge = result.getEdges().iterator().next();
        assertEquals("UNDER_NAME", transformedEdge.getLabel());
        assertEquals("FlightReservation", 
            transformedEdge.getSource().getLabels().iterator().next());
        assertEquals("Person", 
            transformedEdge.getTarget().getLabels().iterator().next());
    }
}