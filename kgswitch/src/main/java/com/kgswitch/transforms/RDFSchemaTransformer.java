package com.kgswitch.transforms;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

import com.kgswitch.models.*;
import java.util.*;

public class RDFSchemaTransformer {
    private Model rdfModel;
    private Map<Resource, SchemaNode> nodeMap;
    private SchemaGraph statementGraph;

    public RDFSchemaTransformer() {
        this.rdfModel = ModelFactory.createDefaultModel();
        this.nodeMap = new HashMap<>();
    }

    public SchemaGraph transformToStatementGraph(String rdfSchemaFile) {
        // Load RDF schema
        rdfModel.read(rdfSchemaFile);
        statementGraph = new SchemaGraph(extractNamespace());
        
        // Process all SHACL node shapes
        processNodeShapes();
        
        // Process property shapes and constraints
        processPropertyShapes();
        
        return statementGraph;
    }

    private void processNodeShapes() {
        ResIterator nodeShapes = rdfModel.listSubjectsWithProperty(RDF.type, 
            rdfModel.createResource("http://www.w3.org/ns/shacl#NodeShape"));
        
        while (nodeShapes.hasNext()) {
            Resource shape = nodeShapes.next();
            SchemaNode node = createSchemaNode(shape);
            nodeMap.put(shape, node);
            statementGraph.addNode(node);
        }
    }

    private SchemaNode createSchemaNode(Resource shape) {
        SchemaNode node = new SchemaNode(shape.getURI());
        
        // Process all target classes
        StmtIterator targetClass = shape.listProperties(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#targetClass"));
        while (targetClass.hasNext()) {
            String targetClassUri = targetClass.next().getObject().toString();
            System.out.println("Extracted target class: " + targetClassUri); // Debugging
            node.addLabel(targetClassUri);
        }
    
        return node;
    }

    private void processPropertyShapes() {
        for (Map.Entry<Resource, SchemaNode> entry : nodeMap.entrySet()) {
            Resource shape = entry.getKey();
            SchemaNode node = entry.getValue();

            StmtIterator properties = shape.listProperties(
                rdfModel.createProperty("http://www.w3.org/ns/shacl#property"));
            
            while (properties.hasNext()) {
                Resource propertyShape = properties.next().getResource();
                processPropertyShape(propertyShape, node);
            }
        }
    }

    private void processPropertyShape(Resource propertyShape, SchemaNode node) {
        // Get property path
        Statement pathStmt = propertyShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#path"));
        if (pathStmt == null) return;
    
        // Extract the local name from the property URI
        String propertyUri = pathStmt.getObject().toString();
        String propertyName = propertyUri;
        
        // Handle both URI formats (with # or /)
        if (propertyUri.contains("#")) {
            propertyName = propertyUri.substring(propertyUri.lastIndexOf('#') + 1);
        } else if (propertyUri.contains("/")) {
            propertyName = propertyUri.substring(propertyUri.lastIndexOf('/') + 1);
        }
        
        String dataType = extractDataType(propertyShape);
        
        PropertyConstraint constraint = new PropertyConstraint(propertyName, dataType);
        
        // Process cardinality
        processCardinality(propertyShape, constraint);
        
        node.addPropertyConstraint(constraint);
    }

    private void processCardinality(Resource propertyShape, PropertyConstraint constraint) {
        Statement minCount = propertyShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#minCount"));
        Statement maxCount = propertyShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#maxCount"));
        
        int min = minCount != null ? minCount.getInt() : 0;
        int max = maxCount != null ? maxCount.getInt() : -1;
        
        constraint.setCardinality(min, max);
    }

    private String extractDataType(Resource propertyShape) {
        Statement dataType = propertyShape.getProperty(
            rdfModel.createProperty("http://www.w3.org/ns/shacl#datatype"));
        return dataType != null ? dataType.getObject().toString() : "http://www.w3.org/2001/XMLSchema#string";
    }

    private String extractNamespace() {
        return rdfModel.getNsPrefixMap().getOrDefault("", "http://example.org/");
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