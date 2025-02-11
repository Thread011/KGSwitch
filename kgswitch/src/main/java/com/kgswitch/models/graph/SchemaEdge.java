package com.kgswitch.models.graph;

import com.kgswitch.models.constraints.PropertyConstraint;
import java.util.Map;
import java.util.HashMap;

public class SchemaEdge {
    private String id;
    private String type;
    private SchemaNode source;
    private SchemaNode target;
    private String label;
    private Map<String, Object> properties;
    private Map<String, PropertyConstraint> propertyConstraints;

    public SchemaEdge(String id, SchemaNode source, SchemaNode target, String type) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.type = type;
        this.label = type;
        this.properties = new HashMap<>();
        this.propertyConstraints = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public SchemaNode getSource() {
        return source;
    }

    public SchemaNode getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Map<String, PropertyConstraint> getPropertyConstraints() {
        return propertyConstraints;
    }

    public void addPropertyConstraint(PropertyConstraint constraint) {
        propertyConstraints.put(constraint.getName(), constraint);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }
}