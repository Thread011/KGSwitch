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
import java.nio.file.Path;
import java.nio.file.Paths;

public class RDFSchemaTransformer {
    private Model rdfModel;
    private SchemaGraph statementGraph;
    private Map<String, SchemaNode> nodeStatements;
    private static final String SHACL_NS = "http://www.w3.org/ns/shacl#";

    public SchemaGraph transformToStatementGraph(String ttlFile) {
        try {
            // Normalize file path by converting to URI format
            Path normalizedPath = Paths.get(ttlFile).toAbsolutePath().normalize();
            String fileUri = normalizedPath.toUri().toString();
            
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(fileUri, "TURTLE");
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
            
            // Check if this is a relationship or nested shape
            Statement classStmt = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "class")
            );
            
            if (classStmt != null) {
                // Create the relationship regardless of nested properties
                processRelationshipShape(propertyShape, sourceNode, propertyName, classStmt);
                
                // Also process nested properties if they exist
                StmtIterator nestedProps = propertyShape.listProperties(
                    rdfModel.createProperty(SHACL_NS + "property")
                );
                
                while (nestedProps.hasNext()) {
                    Resource nestedShape = nestedProps.next().getObject().asResource();
                    Statement nestedPath = nestedShape.getProperty(
                        rdfModel.createProperty(SHACL_NS + "path")
                    );
                    
                    if (nestedPath != null) {
                        String nestedPropertyName = getLocalName(nestedPath.getObject().toString());
                        String compoundName = propertyName + "_" + nestedPropertyName;
                        
                        Statement datatypeStmt = nestedShape.getProperty(
                            rdfModel.createProperty(SHACL_NS + "datatype")
                        );
                        
                        if (datatypeStmt != null) {
                            PropertyConstraint constraint = new PropertyConstraint(
                                compoundName,
                                datatypeStmt.getObject().toString()
                            );
                            
                            // Add cardinality constraints if present
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
                            
                            sourceNode.addPropertyConstraint(constraint);
                            System.out.println("Added nested property: " + compoundName);
                        }
                    }
                }
            } else {
                // regular property
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
            
            // Check if there are any properties defined for this relationship in the RDF model
            // This handles the case where relationship properties are defined at the same level, not nested
            String relationshipIRI = propertyShape.getProperty(
                rdfModel.createProperty(SHACL_NS + "path")
            ).getObject().toString();
            
            // Find all property statements where the subject is the relationship IRI
            StmtIterator relProps = rdfModel.listStatements(
                null,
                rdfModel.createProperty(SHACL_NS + "path"),
                rdfModel.createResource(relationshipIRI)
            );
            
            while (relProps.hasNext()) {
                Resource relPropShape = relProps.next().getSubject();
                
                // Check if this is a property statement (has datatype)
                Statement datatypeStmt = relPropShape.getProperty(
                    rdfModel.createProperty(SHACL_NS + "datatype")
                );
                
                if (datatypeStmt != null) {
                    // This is a property for the relationship
                    String relPropName = getLocalName(relationshipIRI) + "_property";
                    
                    PropertyConstraint constraint = new PropertyConstraint(
                        relPropName,
                        datatypeStmt.getObject().toString()
                    );
                    
                    // Add cardinality if present
                    Statement minCount = relPropShape.getProperty(
                        rdfModel.createProperty(SHACL_NS + "minCount")
                    );
                    Statement maxCount = relPropShape.getProperty(
                        rdfModel.createProperty(SHACL_NS + "maxCount")
                    );
                    
                    if (minCount != null || maxCount != null) {
                        int min = minCount != null ? minCount.getInt() : 0;
                        int max = maxCount != null ? maxCount.getInt() : -1;
                        constraint.setCardinality(min, max);
                    }
                    
                    edge.addPropertyConstraint(constraint);
                    System.out.println("Added relationship property constraint: " + relPropName);
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
