package com.kgswitch.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.model.Factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.kgswitch.models.graph.SchemaGraph;
import com.kgswitch.models.graph.SchemaNode;
import com.kgswitch.models.graph.SchemaEdge;
import com.kgswitch.models.constraints.PropertyConstraint;

/**
 * Utility class to generate visualization images of graph schemas
 */
public class GraphVisualizer {

    private final ObjectMapper objectMapper;
    private static final Map<Integer, Color> NODE_COLORS = Map.of(
        0, Color.rgb("#FF5733"), // Red
        1, Color.rgb("#33A1FF"), // Blue
        2, Color.rgb("#33FF57"), // Green
        3, Color.rgb("#9133FF"), // Purple
        4, Color.rgb("#FFDD33"), // Yellow
        5, Color.rgb("#FF33A1"), // Pink
        6, Color.rgb("#33FFDD"), // Teal
        7, Color.rgb("#A1FF33"), // Lime
        8, Color.rgb("#FF8333"), // Orange
        9, Color.rgb("#8333FF")  // Indigo
    );

    public GraphVisualizer() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate visualization image from a JSON schema file
     * 
     * @param jsonSchemaFile Path to the JSON schema file
     * @param outputImageFile Path to the output image file (.png, .svg, .jpg supported)
     * @return Path to the generated image file
     * @throws IOException If file cannot be read or processed
     */
    public String generateImageFromFile(String jsonSchemaFile, String outputImageFile) throws IOException {
        String jsonContent = Files.readString(Paths.get(jsonSchemaFile));
        return generateImageFromJson(jsonContent, outputImageFile);
    }
    
    /**
     * Generate visualization image from a JSON string
     * 
     * @param jsonSchema JSON schema as a string
     * @param outputImageFile Path to the output image file (.png, .svg, .jpg supported)
     * @return Path to the generated image file
     * @throws IOException If JSON cannot be parsed or image cannot be created
     */
    public String generateImageFromJson(String jsonSchema, String outputImageFile) throws IOException {
        JsonNode root = objectMapper.readTree(jsonSchema);
        
        // Create a mutable graph
        MutableGraph graph = Factory.mutGraph("Schema").setDirected(true)
            .graphAttrs().add(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
            .graphAttrs().add(GraphAttr.splines(GraphAttr.SplineMode.SPLINE))
            .nodeAttrs().add(Shape.ELLIPSE)
            .linkAttrs().add("arrowhead", "vee");
        
        Map<String, MutableNode> nodes = createNodesFromJson(root.get("nodes"), graph);
        createRelationshipsFromJson(root.get("relationships"), nodes, graph);
        
        // Determine output format
        Format format = determineOutputFormat(outputImageFile);
        
        // Render the graph to an image file
        Graphviz.fromGraph(graph)
            .width(1200)
            .render(format)
            .toFile(new File(outputImageFile));
        
        return outputImageFile;
    }
    
    /**
     * Generate visualization directly from a SchemaGraph
     * 
     * @param schemaGraph The schema graph to visualize
     * @param outputImageFile Path to the output image file (.png, .svg, .jpg supported)
     * @return Path to the generated image file
     * @throws IOException If image cannot be created
     */
    public String generateImageFromSchemaGraph(SchemaGraph schemaGraph, String outputImageFile) throws IOException {
        // Create a mutable graph
        MutableGraph graph = Factory.mutGraph("Schema").setDirected(true)
            .graphAttrs().add(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
            .graphAttrs().add(GraphAttr.splines(GraphAttr.SplineMode.SPLINE))
            .nodeAttrs().add(Shape.RECTANGLE) // Changed to rectangle for better label display
            .linkAttrs().add("arrowhead", "vee");
        
        // First collect all edge properties to filter them out from nodes
        Set<String> edgePropertyNames = new HashSet<>();
        Map<String, Set<String>> edgeTypeProperties = new HashMap<>();
        
        for (SchemaEdge edge : schemaGraph.getEdges()) {
            String edgeType = edge.getType().toLowerCase();
            Set<String> propNames = new HashSet<>();
            
            for (Map.Entry<String, PropertyConstraint> entry : edge.getPropertyConstraints().entrySet()) {
                String propName = entry.getKey();
                // Add the property name to our set of edge properties
                edgePropertyNames.add(propName);
                propNames.add(propName);
                
                // Also add the composite form (edgeType_propName) that might appear in node properties
                edgePropertyNames.add(edgeType + "_" + propName);
            }
            
            edgeTypeProperties.put(edgeType, propNames);
        }
        
        // Generate nodes
        Map<String, MutableNode> nodeMap = new HashMap<>();
        int colorIndex = 0;
        
        for (SchemaNode node : schemaGraph.getNodes()) {
            if (!node.getLabels().isEmpty()) {
                String label = node.getLabels().iterator().next();
                Color color = NODE_COLORS.get(colorIndex % NODE_COLORS.size());
                colorIndex++;
                
                // Build a label that includes the node label and its properties
                StringBuilder nodeLabel = new StringBuilder();
                nodeLabel.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">");
                
                // Table header - node type with simple black text and white background
                nodeLabel.append("<TR><TD COLSPAN=\"2\" BGCOLOR=\"white\"><B>")
                       .append(label).append("</B></TD></TR>");
                
                // Add property constraints as table rows, filtering out edge properties
                for (Map.Entry<String, PropertyConstraint> entry : node.getPropertyConstraints().entrySet()) {
                    String propName = entry.getKey();
                    
                    // Skip if property is an exact match to an edge property
                    if (edgePropertyNames.contains(propName)) {
                        continue;
                    }
                    
                    // Check if property is in the format edgeType_propertyName
                    boolean isEdgeProperty = false;
                    if (propName.contains("_")) {
                        String prefix = propName.substring(0, propName.indexOf("_"));
                        String suffix = propName.substring(propName.indexOf("_") + 1);
                        
                        // Check if the suffix matches any edge property
                        if (edgeTypeProperties.containsKey(prefix.toLowerCase())) {
                            Set<String> propNames = edgeTypeProperties.get(prefix.toLowerCase());
                            if (propNames.contains(suffix)) {
                                isEdgeProperty = true;
                            }
                        }
                    }
                    
                    if (!isEdgeProperty) {
                        String propType = getSimpleTypeFromDataType(entry.getValue().getDataType());
                        nodeLabel.append("<TR><TD>").append(propName).append("</TD><TD>")
                                 .append(propType).append("</TD></TR>");
                    }
                }
                
                nodeLabel.append("</TABLE>");
                
                MutableNode graphNode = Factory.mutNode(label)
                    .add(Shape.NONE) // No shape as we're using HTML table
                    .add(Label.html(nodeLabel.toString()));
                
                graph.add(graphNode);
                nodeMap.put(label, graphNode);
            }
        }
        
        // Generate edges
        for (SchemaEdge edge : schemaGraph.getEdges()) {
            if (edge.getSource() != null && edge.getTarget() != null) {
                String sourceLabel = edge.getSource().getLabels().iterator().next();
                String targetLabel = edge.getTarget().getLabels().iterator().next();
                
                if (nodeMap.containsKey(sourceLabel) && nodeMap.containsKey(targetLabel)) {
                    MutableNode sourceNode = nodeMap.get(sourceLabel);
                    MutableNode targetNode = nodeMap.get(targetLabel);
                    
                    // Create relationship with properties if any
                    StringBuilder edgeLabel = new StringBuilder(edge.getType());
                    
                    // Add relationship properties if any
                    if (!edge.getPropertyConstraints().isEmpty()) {
                        edgeLabel.append("\n(");
                        boolean first = true;
                        for (Map.Entry<String, PropertyConstraint> entry : edge.getPropertyConstraints().entrySet()) {
                            if (!first) {
                                edgeLabel.append(", ");
                            }
                            edgeLabel.append(entry.getKey()).append(": ")
                                     .append(getSimpleTypeFromDataType(entry.getValue().getDataType()));
                            first = false;
                        }
                        edgeLabel.append(")");
                    }
                    
                    sourceNode.addLink(
                        Factory.to(targetNode)
                            .with(Label.of(edgeLabel.toString()), Style.SOLID)
                    );
                }
            }
        }
        
        // Determine output format
        Format format = determineOutputFormat(outputImageFile);
        
        // Render the graph to an image file
        Graphviz.fromGraph(graph)
            .width(1500) // Increased width for better visibility
            .height(1000) // Added height parameter for better layout
            .render(format)
            .toFile(new File(outputImageFile));
        
        return outputImageFile;
    }
    
    /**
     * Create nodes from JSON schema nodes array
     */
    private Map<String, MutableNode> createNodesFromJson(JsonNode nodes, MutableGraph graph) {
        Map<String, MutableNode> nodeMap = new HashMap<>();
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            return nodeMap;
        }
        
        int colorIndex = 0;
        Set<String> processedLabels = new HashSet<>();
        
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            if (!node.has("label")) {
                continue;
            }
            
            String label = node.get("label").asText();
            if (processedLabels.contains(label)) {
                continue;
            }
            
            processedLabels.add(label);
            Color color = NODE_COLORS.get(colorIndex % NODE_COLORS.size());
            colorIndex++;
            
            // Build a label that includes the node label and its properties
            StringBuilder nodeLabel = new StringBuilder();
            nodeLabel.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">");
            
            // Table header - node type with simple black text and white background
            nodeLabel.append("<TR><TD COLSPAN=\"2\" BGCOLOR=\"white\"><B>")
                   .append(label).append("</B></TD></TR>");
            
            // Add properties as table rows
            JsonNode properties = node.get("properties");
            if (properties != null && properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String propName = entry.getKey();
                    
                    // Skip special properties
                    if (propName.equals("id") || propName.equals("name") || 
                        propName.equals("label") || propName.equals("displayName") ||
                        propName.equals("color")) {
                        continue;
                    }
                    
                    JsonNode propDetails = entry.getValue();
                    String propType = propDetails.has("type") ? propDetails.get("type").asText() : "String";
                    
                    nodeLabel.append("<TR><TD>").append(propName).append("</TD><TD>")
                             .append(propType).append("</TD></TR>");
                }
            }
            
            nodeLabel.append("</TABLE>");
            
            MutableNode graphNode = Factory.mutNode(label)
                .add(Shape.NONE) // No shape as we're using HTML table
                .add(Label.html(nodeLabel.toString()));
            
            graph.add(graphNode);
            nodeMap.put(label, graphNode);
        }
        
        return nodeMap;
    }
    
    /**
     * Create relationships from JSON schema relationships array
     */
    private void createRelationshipsFromJson(JsonNode relationships, Map<String, MutableNode> nodes, MutableGraph graph) {
        if (relationships == null || !relationships.isArray() || relationships.isEmpty()) {
            return;
        }
        
        Set<String> processedRels = new HashSet<>();
        
        for (int i = 0; i < relationships.size(); i++) {
            JsonNode rel = relationships.get(i);
            String type = rel.get("type").asText();
            String sourceLabel = rel.get("source").asText();
            String targetLabel = rel.get("target").asText();
            
            String relKey = sourceLabel + "-" + type + "-" + targetLabel;
            if (processedRels.contains(relKey)) {
                continue;
            }
            
            processedRels.add(relKey);
            
            if (nodes.containsKey(sourceLabel) && nodes.containsKey(targetLabel)) {
                MutableNode sourceNode = nodes.get(sourceLabel);
                MutableNode targetNode = nodes.get(targetLabel);
                
                // Create relationship with properties if any
                StringBuilder edgeLabel = new StringBuilder(type);
                
                // Add relationship properties if any
                JsonNode properties = rel.get("properties");
                if (properties != null && properties.isObject() && properties.size() > 0) {
                    edgeLabel.append("\n(");
                    boolean first = true;
                    Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        
                        // Skip name property which is the relationship type
                        if (entry.getKey().equals("name")) {
                            continue;
                        }
                        
                        if (!first) {
                            edgeLabel.append(", ");
                        }
                        
                        String propName = entry.getKey();
                        JsonNode propDetails = entry.getValue();
                        String propType = propDetails.isTextual() ? propDetails.asText() : "String";
                        
                        edgeLabel.append(propName).append(": ").append(propType);
                        first = false;
                    }
                    edgeLabel.append(")");
                }
                
                sourceNode.addLink(
                    Factory.to(targetNode)
                        .with(Label.of(edgeLabel.toString()), Style.SOLID)
                );
            }
        }
    }
    
    /**
     * Determine the appropriate GraphViz format based on file extension
     * 
     * @param outputFile Path to the output file
     * @return The corresponding GraphViz Format
     */
    private Format determineOutputFormat(String outputFile) {
        String lowerCaseFile = outputFile.toLowerCase();
        
        // Use DOT format by default to avoid XML parsing issues
        // DOT files can be viewed with Graphviz viewer or converted to images later
        System.out.println("Note: Using DOT format for output which works more reliably.");
        System.out.println("You can open the DOT file with Graphviz Viewer or convert it with the dot command:");
        System.out.println("  dot -Tpng " + outputFile + " -o output.png");
        
        // Allow SVG format if explicitly requested
        if (lowerCaseFile.endsWith(".svg")) {
            try {
                // Try to validate if SVG output will work
                return Format.SVG;
            } catch (Exception e) {
                System.out.println("Warning: SVG format had issues, using DOT format instead: " + e.getMessage());
                return Format.DOT;
            }
        }
        
        return Format.DOT;
    }
    
    /**
     * Get a simple type name from a full data type URI
     */
    private String getSimpleTypeFromDataType(String dataType) {
        if (dataType == null || dataType.isEmpty()) {
            return "String";
        }
        
        // Handle XML Schema datatypes
        if (dataType.contains("#")) {
            String type = dataType.substring(dataType.indexOf("#") + 1);
            switch (type.toLowerCase()) {
                case "string":
                    return "String";
                case "integer":
                case "int":
                    return "Integer";
                case "float":
                case "double":
                    return "Float";
                case "boolean":
                    return "Boolean";
                case "date":
                    return "Date";
                case "datetime":
                    return "DateTime";
                default:
                    return "String";
            }
        }
        
        // If it's a simple type name already, return it with first letter capitalized
        if (dataType.length() > 0) {
            return dataType.substring(0, 1).toUpperCase() + dataType.substring(1);
        }
        
        return "String";
    }
} 