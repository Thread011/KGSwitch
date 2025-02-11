package com.kgswitch.transforms.rdf;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

import com.kgswitch.models.constraints.PropertyConstraint;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;

import java.util.*;

public class RDFSchemaTransformer {
    private Model rdfModel;
    private SchemaGraph statementGraph;
    private Map<String, SchemaNode> nodeStatements;

    public SchemaGraph transformToStatementGraph(String ttlFile) {
        try {
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(ttlFile, "TURTLE");
            statementGraph = new SchemaGraph("rdf");
            nodeStatements = new HashMap<>();

            // Process all node shapes
            ResIterator nodeShapes = rdfModel.listSubjectsWithProperty(
                RDF.type, 
                rdfModel.createProperty("http://www.w3.org/ns/shacl#NodeShape")
            );

            // First pass: Create all nodes
            while (nodeShapes.hasNext()) {
                Resource nodeShape = nodeShapes.next();
                createNodeFromShape(nodeShape);
            }

            // Second pass: Process properties
            nodeShapes = rdfModel.listSubjectsWithProperty(
                RDF.type, 
                rdfModel.createProperty("http://www.w3.org/ns/shacl#NodeShape")
            );
            
            while (nodeShapes.hasNext()) {
                Resource nodeShape = nodeShapes.next();
                processNodeProperties(nodeShape);
            }

            return statementGraph;
        } catch (Exception e) {
            System.err.println("Error transforming TTL to statement graph: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to transform TTL file", e);
        }
    }

    private void createNodeFromShape(Resource nodeShape) {
        Statement targetClass = nodeShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#targetClass")
        );
        
        if (targetClass != null) {
            String nodeId = getLocalName(targetClass.getObject().toString());
            String className = nodeId;
            
            SchemaNode typeStatement = new SchemaNode(nodeId);
            typeStatement.addLabel(className);
            
            statementGraph.addNode(typeStatement);
            nodeStatements.put(nodeId, typeStatement);
            
            System.out.println("Created node: " + nodeId + " with class: " + className);
        }
    }

    private void processNodeProperties(Resource nodeShape) {
        Statement targetClass = nodeShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#targetClass")
        );
        
        if (targetClass != null) {
            String nodeId = getLocalName(targetClass.getObject().toString());
            SchemaNode node = nodeStatements.get(nodeId);
            
            if (node != null) {
                StmtIterator properties = nodeShape.listProperties(
                    rdfModel.createProperty("http://www.w3.org/ns/shacl#property")
                );
                
                while (properties.hasNext()) {
                    Resource propertyShape = properties.next().getObject().asResource();
                    processPropertyShape(propertyShape, node);
                }
            }
        }
    }

    private void processPropertyShape(Resource propertyShape, SchemaNode sourceNode) {
        Statement pathStmt = propertyShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#path")
        );
        
        if (pathStmt != null) {
            String path = pathStmt.getObject().toString();
            String propertyName = getLocalName(path);
            
            // Check if this is a relationship or a property
            Statement classStmt = propertyShape.getProperty(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#class")
            );
            
            if (classStmt != null) {
                // This is a relationship
                String targetClassName = getLocalName(classStmt.getObject().toString());
                SchemaNode targetNode = nodeStatements.get(targetClassName);
                
                if (targetNode != null) {
                    SchemaEdge edge = new SchemaEdge(
                        propertyName.toUpperCase(),
                        sourceNode,
                        targetNode,
                        propertyName.toUpperCase()
                    );
                    
                    // Add cardinality constraints to the edge
                    Statement minCount = propertyShape.getProperty(
                        rdfModel.createProperty("http://www.w3.org/ns/shacl#minCount")
                    );
                    Statement maxCount = propertyShape.getProperty(
                        rdfModel.createProperty("http://www.w3.org/ns/shacl#maxCount")
                    );
                    
                    if (minCount != null) {
                        edge.addProperty("minCount", String.valueOf(minCount.getInt()));
                    }
                    if (maxCount != null) {
                        edge.addProperty("maxCount", String.valueOf(maxCount.getInt()));
                    }
                    
                    statementGraph.addEdge(edge);
                    System.out.println("Added relationship: " + propertyName.toUpperCase() + 
                                     " from " + sourceNode.getId() + 
                                     " to " + targetNode.getId());
                }
            } else {
                // This is a property
                Statement datatypeStmt = propertyShape.getProperty(
                    rdfModel.createProperty("http://www.w3.org/ns/shacl#datatype")
                );
                
                if (datatypeStmt != null) {
                    PropertyConstraint constraint = new PropertyConstraint(
                        propertyName,
                        datatypeStmt.getObject().toString()
                    );
                    
                    // Add cardinality to constraint
                    Statement minCount = propertyShape.getProperty(
                        rdfModel.createProperty("http://www.w3.org/ns/shacl#minCount")
                    );
                    Statement maxCount = propertyShape.getProperty(
                        rdfModel.createProperty("http://www.w3.org/ns/shacl#maxCount")
                    );
                    
                    int min = minCount != null ? minCount.getInt() : 0;
                    int max = maxCount != null ? maxCount.getInt() : -1;
                    constraint.setCardinality(min, max);
                    
                    sourceNode.addPropertyConstraint(constraint);
                }
            }
        }
    }

    private String getLocalName(String uri) {
        if (uri.contains("#")) {
            return uri.substring(uri.lastIndexOf("#") + 1);
        } else if (uri.contains("/")) {
            return uri.substring(uri.lastIndexOf("/") + 1);
        }
        return uri;
    }

    public boolean validateSHACL(Model dataModel) {
        Model shapesModel = rdfModel;
        Resource report = ValidationUtil.validateModel(dataModel, shapesModel, true);
        
        // Check if the report contains any validation results
        if (report.hasProperty(RDF.type, SH.ValidationReport)) {
            // Check if there are any validation results with severity of sh:Violation
            StmtIterator violations = report.listProperties(SH.result);
            while (violations.hasNext()) {
                Resource result = violations.next().getResource();
                if (result.hasProperty(SH.severity, SH.Violation)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public boolean validatePropertyGraphSchema(SchemaGraph schema) {
        // Basic validation rules
        for (SchemaNode node : schema.getNodes()) {
            // Validate required properties
            for (PropertyConstraint constraint : node.getPropertyConstraints().values()) {
                if (constraint.isRequired()) {
                    if (!node.hasProperty(constraint.getName())) {
                        return false;
                    }
                }
            }
            
            // Validate cardinality
            for (Map.Entry<String, Object> property : node.getProperties().entrySet()) {
                PropertyConstraint constraint = node.getPropertyConstraints().get(property.getKey());
                if (constraint != null) {
                    // Handle single value vs list of values
                    int count = 1;
                    if (property.getValue() instanceof List<?>) {
                        count = ((List<?>) property.getValue()).size();
                    }
                    
                    if (count < constraint.getMinCardinality() || 
                        (constraint.getMaxCardinality() != -1 && count > constraint.getMaxCardinality())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
