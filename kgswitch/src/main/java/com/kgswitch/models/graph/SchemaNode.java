package com.kgswitch.models.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.kgswitch.models.constraints.PropertyConstraint;

public class SchemaNode {
    private String id;
    private Set<String> labels;
    private Map<String, PropertyConstraint> propertyConstraints;
    private Map<String, Object> properties; // Add this

    public SchemaNode(String id) {
        this.id = id;
        this.labels = new HashSet<>();
        this.propertyConstraints = new HashMap<>();
        this.properties = new HashMap<>(); // Initialize properties
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void addPropertyConstraint(PropertyConstraint constraint) {
        propertyConstraints.put(constraint.getName(), constraint);
    }

    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    // Getters
    public String getId() { return id; }
    public Set<String> getLabels() { return labels; }
    public Map<String, PropertyConstraint> getPropertyConstraints() { 
        return propertyConstraints; 
    }
    public Map<String, Object> getProperties() { // Add this getter
        return properties;
    }

    public boolean hasProperty(String propertyName) {
        return properties.containsKey(propertyName);
    }
}