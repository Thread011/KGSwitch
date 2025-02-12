package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.kgswitch.models.graph.*;
import com.kgswitch.models.constraints.PropertyConstraint;
import com.kgswitch.transforms.pg.StatementGraphTransformer;
import java.util.Map;

class StatementGraphTransformerTest {
    private SchemaGraph statementGraph;
    private StatementGraphTransformer transformer;

    @BeforeEach
    void setUp() {
        statementGraph = new SchemaGraph("test");
        System.out.println("\n=== Starting New Test ===");
    }

    @Test
    void testBasicNodeTransformation() {
        System.out.println("\n=== Testing Basic Node Transformation ===");
        
        // Create a test node
        SchemaNode node = new SchemaNode("Person");
        node.addLabel("Person");
        node.addPropertyConstraint(new PropertyConstraint("name", "String"));
        statementGraph.addNode(node);
        
        System.out.println("Created source node:");
        System.out.println("  ID: " + node.getId());
        System.out.println("  Labels: " + node.getLabels());
        System.out.println("  Properties: " + node.getPropertyConstraints());

        // Transform
        transformer = new StatementGraphTransformer(statementGraph);
        SchemaGraph pgSchema = transformer.transformToPGSchema();

        // Verify
        SchemaNode transformedNode = pgSchema.getNodes().iterator().next();
        System.out.println("Transformed node:");
        System.out.println("  ID: " + transformedNode.getId());
        System.out.println("  Labels: " + transformedNode.getLabels());
        System.out.println("  Properties: " + transformedNode.getPropertyConstraints());
        
        assertEquals(1, pgSchema.getNodes().size());
        assertEquals("Person", transformedNode.getId());
        assertTrue(transformedNode.getLabels().contains("Person"));
        assertTrue(transformedNode.getPropertyConstraints().containsKey("name"));
    }

    @Test
    void testEdgeTransformation() {
        System.out.println("\n=== Testing Edge Transformation ===");
        
        // Create source and target nodes
        SchemaNode person = new SchemaNode("Person");
        person.addLabel("Person");
        SchemaNode org = new SchemaNode("Organization");
        org.addLabel("Organization");
        
        System.out.println("Created source nodes:");
        System.out.println("  Person: " + person.getId() + " with labels: " + person.getLabels());
        System.out.println("  Organization: " + org.getId() + " with labels: " + org.getLabels());
        
        // Create edge with properties
        SchemaEdge edge = new SchemaEdge("WORKS_AT", person, org, "worksAt");
        PropertyConstraint startDate = new PropertyConstraint("startDate", "Date");
        startDate.setCardinality(1, 1);
        edge.addPropertyConstraint(startDate);
        
        System.out.println("Created edge:");
        System.out.println("  ID: " + edge.getId());
        System.out.println("  Label: " + edge.getLabel());
        System.out.println("  Properties: " + edge.getPropertyConstraints());

        // Add to graph
        statementGraph.addNode(person);
        statementGraph.addNode(org);
        statementGraph.addEdge(edge);

        // Transform
        transformer = new StatementGraphTransformer(statementGraph);
        SchemaGraph pgSchema = transformer.transformToPGSchema();

        // Verify
        SchemaEdge transformedEdge = pgSchema.getEdges().iterator().next();
        System.out.println("Transformed edge:");
        System.out.println("  ID: " + transformedEdge.getId());
        System.out.println("  Label: " + transformedEdge.getLabel());
        System.out.println("  Properties: " + transformedEdge.getPropertyConstraints());
        
        assertEquals(2, pgSchema.getNodes().size());
        assertEquals(1, pgSchema.getEdges().size());
        assertEquals("WORKS_AT", transformedEdge.getId());
        assertEquals("worksAt", transformedEdge.getLabel());
        assertTrue(transformedEdge.getPropertyConstraints().containsKey("startDate"));
    }

    @Test
    void testComplexPropertyConstraints() {
        System.out.println("\n=== Testing Complex Property Constraints ===");
        
        // Create a node with complex property constraints
        SchemaNode flight = new SchemaNode("Flight");
        flight.addLabel("Flight");
        
        PropertyConstraint flightNumber = new PropertyConstraint("flightNumber", "String");
        flightNumber.setCardinality(1, 1);
        PropertyConstraint passengers = new PropertyConstraint("passengers", "Integer");
        passengers.setCardinality(0, -1);
        
        flight.addPropertyConstraint(flightNumber);
        flight.addPropertyConstraint(passengers);
        
        System.out.println("Created source node with complex constraints:");
        System.out.println("  ID: " + flight.getId());
        System.out.println("  Properties:");
        flight.getPropertyConstraints().forEach((key, constraint) -> {
            System.out.println("    " + key + ": " + 
                             "min=" + constraint.getMinCardinality() + 
                             ", max=" + constraint.getMaxCardinality());
        });
        
        statementGraph.addNode(flight);

        // Transform
        transformer = new StatementGraphTransformer(statementGraph);
        SchemaGraph pgSchema = transformer.transformToPGSchema();

        // Verify
        SchemaNode transformedNode = pgSchema.getNodes().iterator().next();
        System.out.println("Transformed node constraints:");
        transformedNode.getPropertyConstraints().forEach((key, constraint) -> {
            System.out.println("    " + key + ": " + 
                             "min=" + constraint.getMinCardinality() + 
                             ", max=" + constraint.getMaxCardinality());
        });
        
        Map<String, PropertyConstraint> constraints = transformedNode.getPropertyConstraints();
        assertTrue(constraints.containsKey("flightNumber"));
        assertTrue(constraints.containsKey("passengers"));
        assertEquals(1, constraints.get("flightNumber").getMinCardinality());
        assertEquals(1, constraints.get("flightNumber").getMaxCardinality());
        assertEquals(0, constraints.get("passengers").getMinCardinality());
        assertEquals(-1, constraints.get("passengers").getMaxCardinality());
    }

    @Test
    void testNestedProperties() {
        System.out.println("\n=== Testing Nested Properties ===");
        
        // Create a node with nested properties
        SchemaNode airport = new SchemaNode("Airport");
        airport.addLabel("Airport");
        
        PropertyConstraint addressStreet = new PropertyConstraint("address_street", "String");
        PropertyConstraint addressCity = new PropertyConstraint("address_city", "String");
        addressStreet.setCardinality(1, 1);
        addressCity.setCardinality(1, 1);
        
        airport.addPropertyConstraint(addressStreet);
        airport.addPropertyConstraint(addressCity);
        
        System.out.println("Created source node with nested properties:");
        System.out.println("  ID: " + airport.getId());
        System.out.println("  Nested properties:");
        airport.getPropertyConstraints().forEach((key, constraint) -> {
            System.out.println("    " + key + ": type=" + constraint.getDataType() + 
                             ", min=" + constraint.getMinCardinality() + 
                             ", max=" + constraint.getMaxCardinality());
        });
        
        statementGraph.addNode(airport);

        // Transform
        transformer = new StatementGraphTransformer(statementGraph);
        SchemaGraph pgSchema = transformer.transformToPGSchema();

        // Verify
        SchemaNode transformedNode = pgSchema.getNodes().iterator().next();
        System.out.println("Transformed node nested properties:");
        transformedNode.getPropertyConstraints().forEach((key, constraint) -> {
            System.out.println("    " + key + ": type=" + constraint.getDataType() + 
                             ", min=" + constraint.getMinCardinality() + 
                             ", max=" + constraint.getMaxCardinality());
        });
        
        Map<String, PropertyConstraint> constraints = transformedNode.getPropertyConstraints();
        assertTrue(constraints.containsKey("address_street"));
        assertTrue(constraints.containsKey("address_city"));
        assertEquals("String", constraints.get("address_street").getDataType());
        assertEquals("String", constraints.get("address_city").getDataType());
    }

    @Test
    void testEdgePropertyPreservation() {
        System.out.println("\n=== Testing Edge Property Preservation ===");
        
        // Create nodes
        SchemaNode flight = new SchemaNode("Flight");
        flight.addLabel("Flight");
        SchemaNode airport = new SchemaNode("Airport");
        airport.addLabel("Airport");
        
        System.out.println("Created source nodes:");
        System.out.println("  Flight: " + flight.getId() + " with labels: " + flight.getLabels());
        System.out.println("  Airport: " + airport.getId() + " with labels: " + airport.getLabels());
        
        // Create edge with properties
        SchemaEdge departure = new SchemaEdge("DEPARTS", flight, airport, "departureAirport");
        PropertyConstraint gate = new PropertyConstraint("gate", "String");
        gate.setCardinality(0, 1);
        PropertyConstraint scheduledTime = new PropertyConstraint("scheduledTime", "DateTime");
        scheduledTime.setCardinality(1, 1);
        
        departure.addPropertyConstraint(gate);
        departure.addPropertyConstraint(scheduledTime);
        
        System.out.println("Created edge with properties:");
        System.out.println("  ID: " + departure.getId());
        System.out.println("  Properties:");
        departure.getPropertyConstraints().forEach((key, constraint) -> {
            System.out.println("    " + key + ": type=" + constraint.getDataType() + 
                             ", min=" + constraint.getMinCardinality() + 
                             ", max=" + constraint.getMaxCardinality());
        });
        
        statementGraph.addNode(flight);
        statementGraph.addNode(airport);
        statementGraph.addEdge(departure);

        // Transform
        transformer = new StatementGraphTransformer(statementGraph);
        SchemaGraph pgSchema = transformer.transformToPGSchema();

        // Verify
        SchemaEdge transformedEdge = pgSchema.getEdges().iterator().next();
        System.out.println("Transformed edge properties:");
        transformedEdge.getPropertyConstraints().forEach((key, constraint) -> {
            System.out.println("    " + key + ": type=" + constraint.getDataType() + 
                             ", min=" + constraint.getMinCardinality() + 
                             ", max=" + constraint.getMaxCardinality());
        });
        
        Map<String, PropertyConstraint> constraints = transformedEdge.getPropertyConstraints();
        assertTrue(constraints.containsKey("gate"));
        assertTrue(constraints.containsKey("scheduledTime"));
        assertEquals(0, constraints.get("gate").getMinCardinality());
        assertEquals(1, constraints.get("gate").getMaxCardinality());
        assertEquals(1, constraints.get("scheduledTime").getMinCardinality());
        assertEquals(1, constraints.get("scheduledTime").getMaxCardinality());
    }
}