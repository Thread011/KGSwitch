package com.kgswitch.models;

import java.util.HashSet;
import java.util.Set;

public class SchemaGraph {
    private Set<SchemaNode> nodes;
    private Set<SchemaEdge> edges;
    private String namespace;

    public SchemaGraph(String namespace) {
        this.namespace = namespace;
        this.nodes = new HashSet<>();
        this.edges = new HashSet<>();
    }

    public void addNode(SchemaNode node) {
        nodes.add(node);
    }

    public void addEdge(SchemaEdge edge) {
        edges.add(edge);
    }

    // Getters
    public Set<SchemaNode> getNodes() { return nodes; }
    public Set<SchemaEdge> getEdges() { return edges; }
    public String getNamespace() { return namespace; }
}