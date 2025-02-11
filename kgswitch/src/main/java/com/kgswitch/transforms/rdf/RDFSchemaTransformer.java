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
    private static final String SHACL_NS = "http://www.w3.org/ns/shacl#";

    public SchemaGraph transformToStatementGraph(String ttlFile) {
        try {
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(ttlFile, "TURTLE");
            statementGraph = new SchemaGraph("rdf");
            nodeStatements = new HashMap<>();

            // Process all node shapes
            ResIterator nodeShapes = rdfModel.listSubjectsWithProperty(
                RDF.type, 
                rdfModel.createResource(SHACL_NS + "NodeShape")
            );

            // First pass: Create all nodes
            while (nodeShapes.hasNext()) {
                Resource nodeShape = nodeShapes.next();
                createNodeFromShape(nodeShape);
            }

            // Second pass: Process properties and relationships
            nodeShapes = rdfModel.listSubjectsWithProperty(
                RDF.type, 
                rdfModel.createResource(SHACL_NS + "NodeShape")
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
            rdfModel.createProperty(SHACL_NS + "targetClass")
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
            rdfModel.createProperty(SHACL_NS + "targetClass")
        );
        
        if (targetClass != null) {
            String nodeId = getLocalName(targetClass.getObject().toString());
            SchemaNode node = nodeStatements.get(nodeId);
            
            if (node != null) {
                // Get all property shapes
                StmtIterator properties = nodeShape.listProperties(
                    rdfModel.createProperty(SHACL_NS + "property")
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
            rdfModel.createProperty(SHACL_NS + "path")
        );
        
        if (pathStmt != null) {
            String path = pathStmt.getObject().toString();
            String propertyName = getLocalName(path);
            
            // Check if this is a relationship
            Statement classStmt = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "class")
            );
            
            if (classStmt != null) {
                // This is a relationship
                processRelationshipShape(propertyShape, sourceNode, propertyName, classStmt);
            } else {
                // This is a regular property
                processRegularPropertyShape(propertyShape, sourceNode, propertyName);
            }
        }
    }

    private void processRelationshipShape(Resource propertyShape, SchemaNode sourceNode, 
                                        String propertyName, Statement classStmt) {
        String targetClassName = getLocalName(classStmt.getObject().toString());
        SchemaNode targetNode = nodeStatements.get(targetClassName);
        
        if (targetNode != null) {
            SchemaEdge edge = new SchemaEdge(
                propertyName.toUpperCase(),
                sourceNode,
                targetNode,
                propertyName.toUpperCase()
            );

            // Process relationship properties
            StmtIterator nestedProps = propertyShape.listProperties(
                rdfModel.createProperty(SHACL_NS + "property")
            );
            
            System.out.println("Processing relationship: " + propertyName);
            
            while (nestedProps.hasNext()) {
                Resource nestedShape = nestedProps.next().getObject().asResource();
                Statement nestedPath = nestedShape.getProperty(
                    rdfModel.createProperty(SHACL_NS + "path")
                );
                
                if (nestedPath != null) {
                    String nestedPropertyName = getLocalName(nestedPath.getObject().toString());
                    System.out.println("Found nested property: " + nestedPropertyName);
                    
                    Statement datatypeStmt = nestedShape.getProperty(
                        rdfModel.createProperty(SHACL_NS + "datatype")
                    );
                    
                    if (datatypeStmt != null) {
                        PropertyConstraint constraint = new PropertyConstraint(
                            nestedPropertyName,
                            datatypeStmt.getObject().toString()
                        );
                        
                        // Add cardinality
                        Statement minCount = nestedShape.getProperty(
                            rdfModel.createProperty(SHACL_NS + "minCount")
                        );
                        Statement maxCount = nestedShape.getProperty(
                            rdfModel.createProperty(SHACL_NS + "maxCount")
                        );
                        
                        if (minCount != null || maxCount != null) {
                            int min = minCount != null ? minCount.getInt() : 0;
                            int max = maxCount != null ? maxCount.getInt() : -1;
                            constraint.setCardinality(min, max);
                        }
                        
                        edge.addPropertyConstraint(constraint);
                        System.out.println("Added property constraint: " + nestedPropertyName + 
                                         " to relationship: " + propertyName);
                    }
                }
            }
            
            // Add relationship cardinality
            Statement minCount = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "minCount")
            );
            Statement maxCount = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "maxCount")
            );
            
            if (minCount != null) {
                edge.addProperty("minCount", String.valueOf(minCount.getInt()));
            }
            if (maxCount != null) {
                edge.addProperty("maxCount", String.valueOf(maxCount.getInt()));
            }
            
            statementGraph.addEdge(edge);
            System.out.println("Added relationship: " + propertyName + 
                             " with " + edge.getPropertyConstraints().size() + " properties");
        }
    }

    private void processRegularPropertyShape(Resource propertyShape, SchemaNode node, String propertyName) {
        Statement datatypeStmt = propertyShape.getProperty(
            rdfModel.createProperty(SHACL_NS + "datatype")
        );
        
        if (datatypeStmt != null) {
            PropertyConstraint constraint = new PropertyConstraint(
                propertyName,
                datatypeStmt.getObject().toString()
            );
            
            Statement minCount = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "minCount")
            );
            Statement maxCount = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "maxCount")
            );
            
            if (minCount != null || maxCount != null) {
                int min = minCount != null ? minCount.getInt() : 0;
                int max = maxCount != null ? maxCount.getInt() : -1;
                constraint.setCardinality(min, max);
            }
            
            node.addPropertyConstraint(constraint);
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
