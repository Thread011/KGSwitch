package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.RDF;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.transforms.rdf.RDFStatementTransformer;
import org.apache.jena.rdf.model.StmtIterator;

class RDFStatementTransformerTest {
    private RDFStatementTransformer transformer;
    private SchemaGraph pgStatementGraph;
    private static final String SHACL_NS = "http://www.w3.org/ns/shacl#";
    private static final String SCHEMA_NS = "http://schema.org/";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

    @BeforeEach
    void setUp() {
        pgStatementGraph = new SchemaGraph("test");
        transformer = new RDFStatementTransformer(pgStatementGraph);
    }

    @Test
    void testTransformToRDF() {
        Model result = transformer.transformToRDF();
        assertNotNull(result, "RDF Model should not be null");
        assertTrue(result.isEmpty(), "Model should be empty for empty statement graph");
    }

    @Test
    void testCardinalityConstraints() {
        // Create a property statement with cardinality
        SchemaNode propertyNode = new SchemaNode("test_property");
        propertyNode.addLabel("PropertyStatement");
        propertyNode.addProperty("subject", "FlightReservation");
        propertyNode.addProperty("predicate", "reservationId");
        propertyNode.addProperty("minCount", "1");
        propertyNode.addProperty("datatype", XSD_NS + "string");
        pgStatementGraph.addNode(propertyNode);

        // Create a type statement
        SchemaNode typeNode = new SchemaNode("test_type");
        typeNode.addLabel("TypeStatement");
        typeNode.addProperty("subject", "FlightReservation");
        typeNode.addProperty("predicate", "type");
        typeNode.addProperty("object", SCHEMA_NS + "FlightReservation");
        pgStatementGraph.addNode(typeNode);

        Model result = transformer.transformToRDF();
        
        // Verify shape creation
        Resource shape = result.getResource(SCHEMA_NS + "FlightReservationShape");
        assertNotNull(shape, "Shape should exist");
    }

    @Test
    void testDataTypeConstraints() {
        // Create a property statement with datatype
        SchemaNode propertyNode = new SchemaNode("test_property");
        propertyNode.addLabel("PropertyStatement");
        propertyNode.addProperty("subject", "FlightReservation");
        propertyNode.addProperty("predicate", "reservationId");
        propertyNode.addProperty("datatype", XSD_NS + "string");
        pgStatementGraph.addNode(propertyNode);

        Model result = transformer.transformToRDF();
        
        assertNotNull(result, "RDF Model should not be null");
        assertFalse(result.isEmpty(), "Model should not be empty");
        
        // Verify shape creation
        Resource shape = result.getResource(SCHEMA_NS + "FlightReservationShape");
        assertNotNull(shape, "Shape should exist");
        
        // Get the property shape
        Property propertyProp = result.createProperty(SHACL_NS + "property");
        StmtIterator propertyStmts = shape.listProperties(propertyProp);
        assertTrue(propertyStmts.hasNext(), "Shape should have property statements");
        
        Resource propertyShape = propertyStmts.nextStatement().getObject().asResource();
        
        // Verify datatype property on the property shape
        Property datatype = result.createProperty(SHACL_NS + "datatype");
        Resource stringType = result.createResource(XSD_NS + "string");
        
        System.out.println("\nProperty shape properties:");
        propertyShape.listProperties().forEachRemaining(System.out::println);
        
        assertTrue(propertyShape.hasProperty(datatype, stringType), 
            "Property shape should have string datatype");
    }

    @Test
    void testNodeShape() {
        // Create a type statement
        SchemaNode typeNode = new SchemaNode("test_type");
        typeNode.addLabel("TypeStatement");
        typeNode.addProperty("subject", "FlightReservation");
        typeNode.addProperty("predicate", "type");
        typeNode.addProperty("object", SCHEMA_NS + "FlightReservation");
        pgStatementGraph.addNode(typeNode);

        Model result = transformer.transformToRDF();
        
        assertNotNull(result, "RDF Model should not be null");
        assertFalse(result.isEmpty(), "Model should not be empty");
        
        // Verify shape creation
        Resource shape = result.getResource(SCHEMA_NS + "FlightReservationShape");
        assertNotNull(shape, "Shape should exist");
        
        // Verify node shape type
        assertTrue(shape.hasProperty(RDF.type, result.createResource(SHACL_NS + "NodeShape")),
            "Shape should be of type NodeShape");
    }
}