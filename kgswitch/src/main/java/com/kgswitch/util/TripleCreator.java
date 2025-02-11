package com.kgswitch.util;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

public class TripleCreator {
    private Model model;
    private Resource currentShape;
    private Resource currentProperty;

    public TripleCreator() {
        model = ModelFactory.createDefaultModel();
    }

    public TripleCreator createModel() {
        model = ModelFactory.createDefaultModel();
        return this;
    }

    public TripleCreator addPrefix(String prefix, String uri) {
        model.setNsPrefix(prefix, uri);
        return this;
    }

    public TripleCreator addNodeShape(String name) {
        currentShape = model.createResource("http://schema.org/" + name + "Shape")
            .addProperty(RDF.type, model.createResource("http://www.w3.org/ns/shacl#NodeShape"))
            .addProperty(
                model.createProperty("http://www.w3.org/ns/shacl#targetClass"),
                model.createResource("http://schema.org/" + name)
            );
        return this;
    }

    public TripleCreator addProperty(String name, String datatype) {
        currentProperty = model.createResource()
            .addProperty(
                model.createProperty("http://www.w3.org/ns/shacl#path"),
                model.createProperty("http://schema.org/" + name)
            )
            .addProperty(
                model.createProperty("http://www.w3.org/ns/shacl#datatype"),
                model.createResource("http://www.w3.org/2001/XMLSchema#" + datatype)
            );
        currentShape.addProperty(
            model.createProperty("http://www.w3.org/ns/shacl#property"),
            currentProperty
        );
        return this;
    }

    public TripleCreator addRelationshipProperty(String name, String targetClass) {
        currentProperty = model.createResource()
            .addProperty(
                model.createProperty("http://www.w3.org/ns/shacl#path"),
                model.createProperty("http://schema.org/" + name)
            )
            .addProperty(
                model.createProperty("http://www.w3.org/ns/shacl#class"),
                model.createResource("http://schema.org/" + targetClass)
            );
        currentShape.addProperty(
            model.createProperty("http://www.w3.org/ns/shacl#property"),
            currentProperty
        );
        return this;
    }

    public TripleCreator addMinCount(int count) {
        currentProperty.addProperty(
            model.createProperty("http://www.w3.org/ns/shacl#minCount"),
            model.createTypedLiteral(count)
        );
        return this;
    }

    public TripleCreator addMaxCount(int count) {
        currentProperty.addProperty(
            model.createProperty("http://www.w3.org/ns/shacl#maxCount"),
            model.createTypedLiteral(count)
        );
        return this;
    }

    public TripleCreator endShape() {
        currentProperty = null;
        return this;
    }

    public Model getModel() {
        return model;
    }
} 