package com.kgswitch.transforms.rdf;

import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import java.util.*;

public class RDFStatementTransformer {
    private final SchemaGraph pgStatementGraph;
    private Model rdfModel;
    private Map<String, Resource> nodeShapes;
    
    // Add namespace constants
    private static final String SHACL_NS = "http://www.w3.org/ns/shacl#";
    private static final String SCHEMA_NS = "http://schema.org/";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
    
    public RDFStatementTransformer(SchemaGraph pgStatementGraph) {
        this.pgStatementGraph = pgStatementGraph;
        this.rdfModel = ModelFactory.createDefaultModel();
        this.nodeShapes = new HashMap<>();
    }
    
    public Model transformToRDF() {
        // Set up namespaces
        rdfModel.setNsPrefix("sh", SHACL_NS);
        rdfModel.setNsPrefix("schema", SCHEMA_NS);
        rdfModel.setNsPrefix("xsd", XSD_NS);
        
        // Process nodes
        for (SchemaNode node : pgStatementGraph.getNodes()) {
            System.out.println("\nProcessing node: " + node.getId());
            System.out.println("Labels: " + node.getLabels());
            System.out.println("Properties: " + node.getProperties());
            
            if (node.getLabels().contains("PropertyStatement")) {
                processPropertyStatement(node);
            } else if (node.getLabels().contains("TypeStatement")) {
                processTypeStatement(node);
            }
        }
        
        // Debug output final model
        System.out.println("\nFinal RDF Model:");
        rdfModel.listStatements().forEachRemaining(System.out::println);
        
        return rdfModel;
    }
    
    private void processTypeStatement(SchemaNode statement) {
        String subjectUri = statement.getProperties().get("subject").toString();
        String objectUri = statement.getProperties().get("object").toString();
        
        // Create shape URI by appending "Shape"
        String shapeUri = objectUri + "Shape";
        
        // Create the node shape
        Resource shape = rdfModel.createResource(shapeUri)
            .addProperty(RDF.type, rdfModel.createResource("http://www.w3.org/ns/shacl#NodeShape"))  // Add NodeShape type
            .addProperty(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#targetClass"),
                rdfModel.createResource(objectUri)
            );
        
        nodeShapes.put(subjectUri, shape);
    }
    
    private void processPropertyStatement(SchemaNode statement) {
        String subjectUri = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        
        System.out.println("Processing property statement:");
        System.out.println("  Subject: " + subjectUri);
        System.out.println("  Predicate: " + predicate);
        System.out.println("  Properties: " + statement.getProperties());

        
        
        Resource subject = nodeShapes.get(subjectUri);
        if (subject == null) {
            System.out.println("Creating new shape for: " + subjectUri);
            subject = rdfModel.createResource(SCHEMA_NS + subjectUri + "Shape")
                .addProperty(RDF.type, rdfModel.createResource(SHACL_NS + "NodeShape"));
            nodeShapes.put(subjectUri, subject);
        }
        
        // Create property shape
        Resource propertyShape = rdfModel.createResource()
            .addProperty(RDF.type, rdfModel.createResource(SHACL_NS + "PropertyShape"))
            .addProperty(
                rdfModel.createProperty(SHACL_NS + "path"),
                rdfModel.createProperty(SCHEMA_NS + predicate)
            );

        // Add datatype constraint
        if (statement.getProperties().containsKey("datatype")) {
            String datatype = statement.getProperties().get("datatype").toString();
            System.out.println("  Adding datatype: " + datatype);
            propertyShape.addProperty(
                rdfModel.createProperty(SHACL_NS + "datatype"),
                rdfModel.createResource(datatype)
            );
        }
        
        // Add cardinality constraints if present
        if (statement.getProperties().containsKey("minCount")) {
            propertyShape.addProperty(
                rdfModel.createProperty(SHACL_NS + "minCount"),
                rdfModel.createTypedLiteral(
                    Integer.parseInt(statement.getProperties().get("minCount").toString())
                )
            );
        }
        
        if (statement.getProperties().containsKey("maxCount")) {
            propertyShape.addProperty(
                rdfModel.createProperty(SHACL_NS + "maxCount"),
                rdfModel.createTypedLiteral(
                    Integer.parseInt(statement.getProperties().get("maxCount").toString())
                )
            );
        }
        
        // Add property shape to node shape
        subject.addProperty(
            rdfModel.createProperty(SHACL_NS + "property"),
            propertyShape
        );
        
        // Debug output
        System.out.println("Created property shape with properties:");
        propertyShape.listProperties().forEachRemaining(stmt -> 
            System.out.println("  " + stmt.getPredicate() + " -> " + stmt.getObject()));
    }
    
    private void processEdgeStatement(SchemaNode statement) {
        String subjectUri = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        String objectUri = statement.getProperties().get("object").toString();
        
        Resource subject = nodeShapes.get(subjectUri);
        if (subject == null) return;
        
        Resource propertyShape = rdfModel.createResource();
        propertyShape.addProperty(RDF.type, 
            rdfModel.createResource("http://www.w3.org/ns/shacl#PropertyShape"));
        
        // Add path for relationship
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#path"),
            rdfModel.createProperty("http://schema.org/", predicate)
        );
        
        // Add target class constraint
        propertyShape.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#class"),
            rdfModel.createResource(objectUri)
        );
        
        // Add cardinality constraints if present
        if (statement.getProperties().containsKey("minCount")) {
            propertyShape.addProperty(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#minCount"),
                rdfModel.createTypedLiteral(
                    Integer.parseInt(statement.getProperties().get("minCount").toString()))
            );
        }
        
        if (statement.getProperties().containsKey("maxCount")) {
            String maxCount = statement.getProperties().get("maxCount").toString();
            if (!maxCount.equals("-1")) {
                propertyShape.addProperty(
                    rdfModel.createProperty("http://www.w3.org/ns/shacl#maxCount"),
                    rdfModel.createTypedLiteral(Integer.parseInt(maxCount))
                );
            }
        }
        
        subject.addProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#property"),
            propertyShape
        );
        
        System.out.println("Added relationship property shape for: " + predicate);
    }
}