package com.kgswitch.models.graph;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class SchemaGraph {
    private String name;
    private String namespace;
    private Set<SchemaNode> nodes;
    private Set<SchemaEdge> edges;
    private Map<String, SchemaNode> nodeMap;

    public SchemaGraph(String name) {
        this(name, "http://schema.org/");
    }

    public SchemaGraph(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
        this.nodes = new HashSet<>();
        this.edges = new HashSet<>();
        this.nodeMap = new HashMap<>();
    }

    public void addNode(SchemaNode node) {
        nodes.add(node);
        nodeMap.put(node.getId(), node);
    }

    public void addEdge(SchemaEdge edge) {
        edges.add(edge);
    }

    // Getters
    public Set<SchemaNode> getNodes() { return nodes; }
    public Set<SchemaEdge> getEdges() { return edges; }
    public String getName() { return name; }
    public String getNamespace() { return namespace; }

    public SchemaNode getNode(String id) {
        return nodeMap.get(id);
    }

    public boolean hasNode(String id) {
        return nodeMap.containsKey(id);
    }
}