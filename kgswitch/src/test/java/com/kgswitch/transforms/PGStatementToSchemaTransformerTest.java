package com.kgswitch.transforms;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.kgswitch.models.graph.*;
import com.kgswitch.transforms.*;
import com.kgswitch.transforms.pg.*;

class PGStatementToSchemaTransformerTest {
    private SchemaGraph statementGraph;
    private PGStatementToSchemaTransformer transformer;

    @BeforeEach
    void setUp() {
        statementGraph = new SchemaGraph("test");
        
        SchemaNode typeStatement = new SchemaNode("stmt1");
        typeStatement.addLabel("TypeStatement");
        typeStatement.addProperty("subject", "person1");
        typeStatement.addProperty("object", "Person");
        
        SchemaNode propertyStatement = new SchemaNode("stmt2");
        propertyStatement.addLabel("PropertyStatement");
        propertyStatement.addProperty("subject", "person1");
        propertyStatement.addProperty("predicate", "name");
        propertyStatement.addProperty("datatype", "string");
        propertyStatement.addProperty("minCount", "1");
        propertyStatement.addProperty("maxCount", "1");
        
        statementGraph.addNode(typeStatement);
        statementGraph.addNode(propertyStatement);
        
        transformer = new PGStatementToSchemaTransformer(statementGraph);
    }

    @Test
    void testTransformation() {
        SchemaGraph result = transformer.transformToPGSchema();
        
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        
        SchemaNode node = result.getNodes().iterator().next();
        assertTrue(node.getLabels().contains("Person"));
        assertTrue(node.getPropertyConstraints().containsKey("name"));
    }
}