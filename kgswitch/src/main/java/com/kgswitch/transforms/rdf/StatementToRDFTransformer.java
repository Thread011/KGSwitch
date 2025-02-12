package com.kgswitch.transforms.rdf;

import com.kgswitch.models.graph.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import java.util.*;

public class StatementToRDFTransformer {
    private final SchemaGraph statementGraph;
    private Model rdfModel;
    private Map<String, Resource> nodeShapes;
    
    public StatementToRDFTransformer(SchemaGraph statementGraph) {
        this.statementGraph = statementGraph;
        this.rdfModel = ModelFactory.createDefaultModel();
        this.nodeShapes = new HashMap<>();
    }
    
    public Model transformToRDF() {
        // Initialize namespaces
        rdfModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        rdfModel.setNsPrefix("schema", "http://schema.org/");
        
        // First pass: Process type statements to create node shapes
        for (SchemaNode node : statementGraph.getNodes()) {
            if (node.getLabels().contains("TypeStatement")) {
                processTypeStatement(node);
            }
        }
        
        // Second pass: Process property statements
        for (SchemaNode node : statementGraph.getNodes()) {
            if (node.getLabels().contains("PropertyStatement")) {
                processPropertyStatement(node);
            }
        }
        
        // Third pass: Process edge statements
        for (SchemaNode node : statementGraph.getNodes()) {
            if (node.getLabels().contains("EdgeStatement")) {
                processEdgeStatement(node);
            }
        }
        
        return rdfModel;
    }
    
    private void processTypeStatement(SchemaNode statement) {
        String subjectUri = statement.getProperties().get("subject").toString();
        String objectUri = statement.getProperties().get("object").toString();
        
        Resource nodeShape = rdfModel.createResource(subjectUri);
        nodeShapes.put(subjectUri, nodeShape);
        
        // Add type as NodeShape
        nodeShape.addProperty(RDF.type, 
            rdfModel.createResource("http://www.w3.org/ns/shacl#NodeShape"));
        
        // Add target class
        nodeShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#targetClass"),
            rdfModel.createResource(objectUri)
        );
    }
    
    private void processPropertyStatement(SchemaNode statement) {
        String subjectUri = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        
        Resource subject = nodeShapes.get(subjectUri);
        if (subject == null) {
            System.err.println("Warning: No node shape found for " + subjectUri);
            return;
        }
        
        // Create property shape
        Resource propertyShape = rdfModel.createResource();
        propertyShape.addProperty(RDF.type, 
            rdfModel.createResource("http://www.w3.org/ns/shacl#PropertyShape"));
        
        // Add path (property name)
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#path"),
            rdfModel.createProperty("http://schema.org/", predicate)
        );
        
        // Add cardinality constraints
        String minCount = statement.getProperties().get("minCount").toString();
        String maxCount = statement.getProperties().get("maxCount").toString();
        
        if (!minCount.equals("0")) {
            propertyShape.addProperty(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#minCount"),
                rdfModel.createTypedLiteral(Integer.parseInt(minCount))
            );
        }
        
        if (!maxCount.equals("-1")) {
            propertyShape.addProperty(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#maxCount"),
                rdfModel.createTypedLiteral(Integer.parseInt(maxCount))
            );
        }
        
        // Add datatype if present
        if (statement.getProperties().containsKey("datatype")) {
            propertyShape.addProperty(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#datatype"),
                rdfModel.createResource(statement.getProperties().get("datatype").toString())
            );
        }
        
        subject.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#property"),
            propertyShape
        );
    }
    
    private void processEdgeStatement(SchemaNode statement) {
        String subjectUri = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        String objectUri = statement.getProperties().get("object").toString();
        
        Resource subject = nodeShapes.get(subjectUri);
        if (subject == null) {
            System.out.println("Warning: No node shape found for " + subjectUri);
            return;
        }
        
        // Create property shape for relationship
        Resource propertyShape = rdfModel.createResource();
        propertyShape.addProperty(RDF.type, 
            rdfModel.createResource("http://www.w3.org/ns/shacl#PropertyShape"));
        
        // Add path (relationship name)
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#path"),
            rdfModel.createProperty("http://schema.org/", predicate)
        );
        
        // Add target class constraint
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#class"),
            rdfModel.createResource(objectUri)
        );
        
        // Add cardinality constraints
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#minCount"),
            rdfModel.createTypedLiteral(1)
        );
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#maxCount"),
            rdfModel.createTypedLiteral(1)
        );
        
        // Add property shape to the node shape using sh:property
        subject.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#property"),
            propertyShape
        );
        
        System.out.println("Added relationship property shape: " + predicate + 
                          " from " + subjectUri + " to " + objectUri);
    }
}