package com.kgswitch.models.graph;

import java.util.Map;
import java.util.HashMap;

public class SchemaEdge {
    private String id;
    private SchemaNode source;
    private SchemaNode target;
    private String label;
    private Map<String, Object> properties;

    public SchemaEdge(String id, SchemaNode source, SchemaNode target, String label) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.label = label;
        this.properties = new HashMap<>();
    }

    // Getters
    public String getId() { return id; }
    public SchemaNode getSource() { return source; }
    public SchemaNode getTarget() { return target; }
    public String getLabel() { return label; }
    public Map<String, Object> getProperties() { return properties; }

    // Property management
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
}