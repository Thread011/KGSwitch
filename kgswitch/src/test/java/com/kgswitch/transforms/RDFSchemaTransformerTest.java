package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import com.kgswitch.models.*;

class RDFSchemaTransformerTest {
    private RDFSchemaTransformer transformer;
    private static final String TEST_SCHEMA_PATH = "src/test/resources/flight-schema.ttl";

    @BeforeEach
    void setUp() {
        transformer = new RDFSchemaTransformer();
    }

    @Test
    void testBasicSchemaTransformation() {
        SchemaGraph result = transformer.transformToStatementGraph(TEST_SCHEMA_PATH);
        
        assertNotNull(result);
        assertFalse(result.getNodes().isEmpty());
        
        // Test specific node presence
        boolean hasFlightReservation = result.getNodes().stream()
            .anyMatch(node -> node.getLabels().contains("http://schema.org/FlightReservation"));
        assertTrue(hasFlightReservation);
    }

    @Test
    void testPropertyConstraints() {
        SchemaGraph result = transformer.transformToStatementGraph(TEST_SCHEMA_PATH);
        
        SchemaNode flightNode = result.getNodes().stream()
            .filter(node -> node.getLabels().contains("http://schema.org/FlightReservation"))
            .findFirst()
            .orElse(null);
            
        assertNotNull(flightNode);
        
        // Test reservationId property
        PropertyConstraint idConstraint = flightNode.getPropertyConstraints().get("reservationId");
        assertNotNull(idConstraint);
        assertEquals(1, idConstraint.getMinCardinality());
        assertEquals(1, idConstraint.getMaxCardinality());
        assertTrue(idConstraint.isRequired());
    }

    @Test
    void testSHACLValidation() {
        // Create test data model
        Model dataModel = ModelFactory.createDefaultModel();
        dataModel.read("src/test/resources/flight-instance.ttl");
        
        assertTrue(transformer.validateSHACL(dataModel));
    }
}