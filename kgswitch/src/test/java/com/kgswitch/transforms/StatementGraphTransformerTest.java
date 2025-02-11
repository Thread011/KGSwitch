// package com.kgswitch.transforms;

// import static org.junit.jupiter.api.Assertions.*;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import com.kgswitch.models.graph.*;
// import com.kgswitch.transforms.pg.*;
// import com.kgswitch.transforms.rdf.*;
// import java.util.Map;


// class StatementGraphTransformerTest {
//     private RDFSchemaTransformer rdfTransformer;
//     private StatementGraphTransformer transformer;
//     private static final String TEST_SCHEMA_PATH = "src/test/resources/flight-schema.ttl";

//     @BeforeEach
//     void setUp() {
//         rdfTransformer = new RDFSchemaTransformer();
//     }

//     @Test
//     void testBasicTransformation() {
//         SchemaGraph statementGraph = rdfTransformer.transformToStatementGraph(TEST_SCHEMA_PATH);
//         transformer = new StatementGraphTransformer(statementGraph);
        
//         SchemaGraph pgSchema = transformer.transformToPGSchema();
        
//         assertNotNull(pgSchema);
//         assertEquals("pg", pgSchema.getNamespace());
//         assertFalse(pgSchema.getNodes().isEmpty());
//     }

//     @Test
//     void testNodePropertyPreservation() {
//         SchemaGraph statementGraph = rdfTransformer.transformToStatementGraph(TEST_SCHEMA_PATH);
//         transformer = new StatementGraphTransformer(statementGraph);
        
//         SchemaGraph pgSchema = transformer.transformToPGSchema();
        
//         SchemaNode originalNode = findFlightReservationNode(statementGraph);
//         SchemaNode transformedNode = findFlightReservationNode(pgSchema);
        
//         assertNotNull(originalNode, "Original FlightReservation node should exist");
//         assertNotNull(transformedNode, "Transformed FlightReservation node should exist");
        
//         // Check if properties exist
//         assertFalse(originalNode.getPropertyConstraints().isEmpty(), 
//             "Original node should have properties");
        
//         // Verify specific properties
//         assertTrue(transformedNode.getPropertyConstraints().containsKey("reservationId"),
//             "Should have reservationId property");
//         assertTrue(transformedNode.getPropertyConstraints().containsKey("reservationStatus"),
//             "Should have reservationStatus property");
//     }

//     private SchemaNode findFlightReservationNode(SchemaGraph graph) {
//         return graph.getNodes().stream()
//             .filter(node -> node.getLabels().stream()
//                 .anyMatch(label -> label.contains("FlightReservation")))
//             .findFirst()
//             .orElse(null);
//     }

//     @Test
//     void testRelationshipTransformation() {
//         SchemaGraph statementGraph = rdfTransformer.transformToStatementGraph(TEST_SCHEMA_PATH);
//         transformer = new StatementGraphTransformer(statementGraph);
        
//         SchemaGraph pgSchema = transformer.transformToPGSchema();
        
//         // In the simplified test schema, we don't have any relationships
//         assertTrue(pgSchema.getEdges().isEmpty(), "Should not have relationships in simplified schema");
        
//         // Verify the FlightReservation node exists and has its properties
//         SchemaNode flightNode = findFlightReservationNode(pgSchema);
//         assertNotNull(flightNode, "FlightReservation node should exist");
//         assertTrue(flightNode.getPropertyConstraints().containsKey("reservationId"),
//             "Should have reservationId property");
//         assertTrue(flightNode.getPropertyConstraints().containsKey("reservationStatus"),
//             "Should have reservationStatus property");
//     }

//     @Test
//     void testRelationshipPropertyPreservation() {
//         // Create a statement graph with relationship properties
//         SchemaNode edgeNode = new SchemaNode("test_edge");
//         edgeNode.addLabel("EdgeStatement");
//         edgeNode.addProperty("predicate", "undername");
//         edgeNode.addProperty("subject", "FlightReservation");
//         edgeNode.addProperty("object", "Person");
//         edgeNode.addProperty("bookingTime", "dateTime");
//         edgeNode.addProperty("bookingAgent", "string");
//         SchemaGraph pgStatementGraph = new SchemaGraph("pg");
//         pgStatementGraph.addNode(edgeNode);

//         transformer = new StatementGraphTransformer(pgStatementGraph);
//         SchemaGraph result = transformer.transformToPGSchema();
        
//         // Verify relationship properties are preserved
//         boolean hasProperties = result.getEdges().stream()
//             .filter(edge -> edge.getLabel().equals("undername"))
//             .findFirst()
//             .map(edge -> {
//                 Map<String, String> props = edge.getProperties();
//                 return props.containsKey("bookingTime") && 
//                        props.get("bookingTime").equals("dateTime") &&
//                        props.containsKey("bookingAgent") &&
//                        props.get("bookingAgent").equals("string");
//             })
//             .orElse(false);
        
//         assertTrue(hasProperties, 
//             "Relationship properties should be preserved in PG schema");
//     }

//     @Test
//     void testDebugOutput() {
//         SchemaGraph result = transformer.transformToStatementGraph(TEST_SCHEMA_PATH);
//         result.getNodes().stream()
//             .filter(node -> node.getLabels().contains("EdgeStatement"))
//             .forEach(node -> {
//                 System.out.println("Edge Statement Node:");
//                 System.out.println("  Labels: " + node.getLabels());
//                 System.out.println("  Properties: " + node.getProperties());
//             });
//     }
// }