package com.kgswitch.transforms.rdf;

import com.kgswitch.models.graph.*;
import org.apache.jena.rdf.model.*;
import java.util.*;

public class RDFStatementToRDFTransformer {
    private final SchemaGraph statementGraph;
    private final Model rdfModel;
    private final Map<String, Resource> resourceMap;

    public RDFStatementToRDFTransformer(SchemaGraph statementGraph) {
        this.statementGraph = statementGraph;
        this.rdfModel = ModelFactory.createDefaultModel();
        this.resourceMap = new HashMap<>();
    }

    public Model transformToRDF() {
        rdfModel.setNsPrefix("schema", "http://schema.org/");
        rdfModel.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        
        Map<String, List<SchemaNode>> graphs = groupStatementsByGraph();
        
        for (Map.Entry<String, List<SchemaNode>> entry : graphs.entrySet()) {
            Model graphModel = createGraphModel(entry.getValue());
            if (entry.getKey().equals("default")) {
                rdfModel.add(graphModel);
            } else {
                rdfModel.add(graphModel.listStatements());
            }
        }
        
        return rdfModel;
    }

    private Map<String, List<SchemaNode>> groupStatementsByGraph() {
        Map<String, List<SchemaNode>> graphs = new HashMap<>();
        graphs.put("default", new ArrayList<>());

        for (SchemaNode statement : statementGraph.getNodes()) {
            String graphName = statement.getProperties()
                .getOrDefault("graph", "default").toString();
            graphs.computeIfAbsent(graphName, k -> new ArrayList<>())
                .add(statement);
        }

        return graphs;
    }

    private Model createGraphModel(List<SchemaNode> statements) {
        Model graphModel = ModelFactory.createDefaultModel();
        
        for (SchemaNode statement : statements) {
            String subject = statement.getProperties().get("subject").toString();
            String predicate = statement.getProperties().get("predicate").toString();
            String object = statement.getProperties().get("object").toString();
            
            Resource s = getOrCreateResource(subject);
            Property p = rdfModel.createProperty(predicate);
            RDFNode o;
            
            if (statement.getProperties().containsKey("datatype")) {
                String datatype = statement.getProperties().get("datatype").toString();
                o = rdfModel.createTypedLiteral(object, datatype);
            } else {
                o = getOrCreateResource(object);
            }
            
            graphModel.add(s, p, o);
        }
        
        return graphModel;
    }

    private Resource getOrCreateResource(String uri) {
        return resourceMap.computeIfAbsent(uri, 
            k -> rdfModel.createResource(uri));
    }
}