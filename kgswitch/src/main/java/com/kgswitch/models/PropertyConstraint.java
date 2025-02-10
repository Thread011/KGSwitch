package com.kgswitch.models;

public class PropertyConstraint {
    private String name;
    private String dataType;
    private int minCardinality;
    private int maxCardinality;
    private boolean required;

    public PropertyConstraint(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
        this.minCardinality = 0;
        this.maxCardinality = 1;
        this.required = false;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getDataType() { return dataType; }
    public int getMinCardinality() { return minCardinality; }
    public int getMaxCardinality() { return maxCardinality; }
    public boolean isRequired() { return required; }

    public void setCardinality(int min, int max) {
        this.minCardinality = min;
        this.maxCardinality = max;
        this.required = min > 0;
    }
}