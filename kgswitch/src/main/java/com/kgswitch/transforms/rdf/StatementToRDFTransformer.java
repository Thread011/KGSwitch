package com.kgswitch.transforms.rdf;

import com.kgswitch.models.graph.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import java.util.*;
import com.kgswitch.models.constraints.PropertyConstraint;

public class StatementToRDFTransformer {
    private static final String SHACL_NS = "http://www.w3.org/ns/shacl#";
    private static final String SCHEMA_NS = "http://schema.org/";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
    
    private final SchemaGraph statementGraph;
    private final Model rdfModel;
    private Map<String, Resource> nodeShapes;
    
    public StatementToRDFTransformer(SchemaGraph statementGraph) {
        this.statementGraph = statementGraph;
        this.rdfModel = ModelFactory.createDefaultModel();
        this.nodeShapes = new HashMap<>();
        
        // Set up common prefixes
        rdfModel.setNsPrefix("sh", SHACL_NS);
        rdfModel.setNsPrefix("schema", SCHEMA_NS);
        rdfModel.setNsPrefix("xsd", XSD_NS);
    }
    
    public Model transformToRDF() {
        for (SchemaNode statement : statementGraph.getNodes()) {
            if (statement.getLabels().contains("TypeStatement")) {
                processTypeStatement(statement);
            } else if (statement.getLabels().contains("PropertyStatement")) {
                processPropertyStatement(statement);
            } else if (statement.getLabels().contains("EdgeStatement")) {
                processEdgeStatement(statement);
            }
        }
        return rdfModel;
    }
    
    private void processTypeStatement(SchemaNode statement) {
        if (!statement.getProperties().containsKey("subject") || 
            !statement.getProperties().containsKey("object")) {
            System.out.println("Warning: Skipping incomplete type statement: " + statement.getId());
            return;
        }
        
        String subject = statement.getProperties().get("subject").toString();
        String object = statement.getProperties().get("object").toString();
        
        Resource subjectResource = rdfModel.createResource(SCHEMA_NS + subject);
        Resource objectResource = rdfModel.createResource(object);
        
        subjectResource.addProperty(RDF.type, objectResource);
    }
    
    private void processPropertyStatement(SchemaNode statement) {
        // Validate required properties exist
        if (!validateRequiredProperties(statement, "subject", "predicate", "datatype")) {
            System.out.println("Warning: Skipping incomplete property statement: " + statement.getId());
            return;
        }
        
        String subject = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        String datatype = statement.getProperties().get("datatype").toString();
        
        Resource propertyShape = rdfModel.createResource()
            .addProperty(rdfModel.createProperty(SHACL_NS + "path"), 
                        rdfModel.createProperty(SCHEMA_NS + predicate))
            .addProperty(rdfModel.createProperty(SHACL_NS + "datatype"), 
                        rdfModel.createResource(datatype));
        
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
        Resource nodeShape = rdfModel.createResource(SCHEMA_NS + subject + "Shape")
            .addProperty(RDF.type, rdfModel.createResource(SHACL_NS + "NodeShape"))
            .addProperty(rdfModel.createProperty(SHACL_NS + "targetClass"), 
                        rdfModel.createResource(SCHEMA_NS + subject))
            .addProperty(rdfModel.createProperty(SHACL_NS + "property"), propertyShape);
    }
    
    private void processEdgeStatement(SchemaNode statement) {
        if (!validateRequiredProperties(statement, "subject", "predicate", "object")) {
            System.out.println("Warning: Skipping incomplete edge statement: " + statement.getId());
            return;
        }
        
        String subject = statement.getProperties().get("subject").toString();
        String predicate = statement.getProperties().get("predicate").toString();
        String object = statement.getProperties().get("object").toString();
        
        Resource propertyShape = rdfModel.createResource()
            .addProperty(rdfModel.createProperty(SHACL_NS + "path"), 
                        rdfModel.createProperty(SCHEMA_NS + predicate))
            .addProperty(rdfModel.createProperty(SHACL_NS + "class"), 
                        rdfModel.createResource(SCHEMA_NS + object));
        
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
        
        // Add nested property constraints
        for (PropertyConstraint constraint : statement.getPropertyConstraints().values()) {
            Resource nestedProperty = rdfModel.createResource()
                .addProperty(rdfModel.createProperty(SHACL_NS + "path"), 
                            rdfModel.createProperty(SCHEMA_NS + constraint.getName()))
                .addProperty(rdfModel.createProperty(SHACL_NS + "datatype"), 
                            rdfModel.createResource(constraint.getDataType()));
            
            if (constraint.getMinCardinality() > 0) {
                nestedProperty.addProperty(
                    rdfModel.createProperty(SHACL_NS + "minCount"),
                    rdfModel.createTypedLiteral(constraint.getMinCardinality())
                );
            }
            
            if (constraint.getMaxCardinality() != -1) {
                nestedProperty.addProperty(
                    rdfModel.createProperty(SHACL_NS + "maxCount"),
                    rdfModel.createTypedLiteral(constraint.getMaxCardinality())
                );
            }
            
            propertyShape.addProperty(rdfModel.createProperty(SHACL_NS + "property"), 
                                    nestedProperty);
        }
        
        // Add property shape to node shape
        Resource nodeShape = rdfModel.createResource(SCHEMA_NS + subject + "Shape")
            .addProperty(RDF.type, rdfModel.createResource(SHACL_NS + "NodeShape"))
            .addProperty(rdfModel.createProperty(SHACL_NS + "targetClass"), 
                        rdfModel.createResource(SCHEMA_NS + subject))
            .addProperty(rdfModel.createProperty(SHACL_NS + "property"), propertyShape);
    }
    
    private boolean validateRequiredProperties(SchemaNode statement, String... requiredProps) {
        for (String prop : requiredProps) {
            if (!statement.getProperties().containsKey(prop) || 
                statement.getProperties().get(prop) == null) {
                return false;
            }
        }
        return true;
    }
}