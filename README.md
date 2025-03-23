# KGSwitch

## Overview

KGSwitch is a Java-based tool for transforming and validating schemas between RDF (Resource Description Framework) and Property Graph representations. Provides bidirectional transformations through an intermediate layer

## Table of Contents
- [Project Structure](#project-structure)
- [Architecture](#architecture)
  - [Core Components](#core-components)
  - [Models](#models)
  - [Transformers](#transformers)
  - [Utilities](#utilities)
- [Transformation Process Architecture](#transformation-process-architecture)
  - [Transformation Steps](#transformation-steps)
  - [Class Responsibilities in Transformation](#class-responsibilities-in-transformation)
- [Visualization](#visualization)
  - [Schema Visualization](#schema-visualization)
  - [Neo4j Visualization](#neo4j-visualization)
  - [Visualization Process](#visualization-process)
  - [Output Formats](#output-formats)
- [Test Use Cases](#test-use-cases)
  - [Unit Tests](#unit-tests)
  - [Integration Tests](#integration-tests)
- [Benchmarking](#benchmarking)
  - [Performance Metrics](#performance-metrics)
  - [Accuracy Metrics](#accuracy-metrics)
  - [Benchmark Results](#benchmark-results)
- [Building and Running](#building-and-running)
  - [Building the Project](#building-the-project)
  - [Running Transformations](#running-transformations)
  - [Visualizing Schemas](#visualizing-schemas)
  - [Running Tests](#running-tests)
  - [Running Benchmarks](#running-benchmarks)
- [Dependencies](#dependencies)

## Project Structure

```
kgswitch/
├── src/
│   ├── main/
│   │   └── java/com/kgswitch/
│   │       ├── core/
│   │       │   ├── SchemaTransformationException.java
│   │       │   ├── SchemaTransformationService.java
│   │       │   └── SchemaWatcher.java
│   │       ├── models/
│   │       │   ├── constraints/
│   │       │   │   └── PropertyConstraint.java
│   │       │   └── schema/
│   │       │       ├── SchemaEdge.java
│   │       │       ├── SchemaGraph.java
│   │       │       ├── SchemaNode.java
│   │       │       └── SchemaRelationship.java
│   │       ├── transforms/
│   │       │   ├── pg/
│   │       │   │   ├── PGSchemaToStatementTransformer.java
│   │       │   │   ├── PGSchemaTransformer.java
│   │       │   │   ├── PGStatementToSchemaTransformer.java
│   │       │   │   └── StatementGraphTransformer.java
│   │       │   └── rdf/
│   │       │       ├── RDFSchemaTransformer.java
│   │       │       ├── RDFStatementGraphTransformer.java
│   │       │       ├── RDFStatementTransformer.java
│   │       │       └── StatementToRDFTransformer.java
│   │       ├── util/
│   │       │   ├── CypherQueryGenerator.java
│   │       │   ├── GraphVisualizer.java
│   │       │   ├── JsonSchemaGenerator.java
│   │       │   ├── Neo4jConnector.java
│   │       │   └── TripleCreator.java
│   │       ├── benchmark/
│   │       │   └── KGSwitchBenchmark.java
│   │       └── Main.java
│   └── test/
│       ├── java/com/kgswitch/transforms/
│       │   ├── PGSchemaTransformerTest.java
│       │   ├── PGStatementToSchemaTransformerTest.java
│       │   ├── RDFSchemaTransformerTest.java
│       │   ├── RDFStatementTransformerTest.java
│       │   ├── SchemaTransformationServiceTest.java
│       │   └── StatementGraphTransformerTest.java
│       └── resources/
│           ├── datasets/
│           │   ├── biolink_model.shacl.ttl
│           │   ├── Dbpedia-SHACL-Shape.ttl
│           │   ├── flight-schema.ttl
│           │   └── academic-schema.ttl
│           ├── flight-instance.ttl
│           ├── test.ttl
│           ├── test_pg_schema.json
│           ├── test_transformed.ttl
│           └── transformations/
│               ├── pg/
│               └── rdf/
└── pom.xml
```

## Architecture

KGSwitch follows a modular architecture designed to handle the complex process of bidirectional schema transformation between RDF and Property Graph models. The architecture is organized into several key components:

### Core Components

- **SchemaTransformationService**: Central service orchestrating the entire transformation pipeline:
  - Manages RDF → Statement Graph → Property Graph conversions
  - Handles validation at each transformation step
  - Generates output formats (JSON, Cypher)
  - Provides schema visualization capabilities

- **SchemaWatcher**: Monitors schema files for changes and triggers transformations
  - Watches directories for new/modified schema files
  - Initiates transformation process automatically
  - Provides real-time schema conversion

- **SchemaTransformationException**: Custom exception handling for transformation errors
  - Captures schema file information
  - Provides detailed error messages
  - Supports debugging of transformation failures

### Models

- **SchemaGraph**: Core graph representation containing:
  - Nodes and edges collections
  - Graph manipulation methods
  - Validation logic
  - Serves as the intermediate representation between RDF and Property Graph models

- **SchemaNode**: Node model with:
  - Labels and properties
  - Property constraints
  - Relationship definitions
  - Supports both RDF resources and Property Graph nodes

- **SchemaEdge**: Edge model defining:
  - Source and target nodes
  - Edge properties and constraints
  - Cardinality rules
  - Represents relationships in both models

- **PropertyConstraint**: Defines constraints for properties:
  - Data types
  - Cardinality (min/max)
  - Validation rules
  - Ensures semantic integrity during transformations

### Transformers

#### Property Graph (PG) Transformers
- **PGSchemaToStatementTransformer**: Converts PG schemas to statement representation
- **PGSchemaTransformer**: Main PG schema transformation logic
- **PGStatementToSchemaTransformer**: Converts statement representation back to PG schema
- **StatementGraphTransformer**: Handles graph-level transformations

#### RDF Transformers
- **RDFSchemaTransformer**: Converts RDF schemas to internal representation
- **RDFStatementGraphTransformer**: Handles RDF statement graph conversions
- **RDFStatementTransformer**: Processes RDF statements
- **StatementToRDFTransformer**: Converts internal representation back to RDF

### Utilities

- **JsonSchemaGenerator**: Creates JSON schema representations
  - Formats nodes with labels and properties
  - Structures relationships with source/target information
  - Preserves property constraints in JSON format

- **GraphVisualizer**: Generates visual representations of schema graphs
  - Creates DOT format output for visualization
  - Renders nodes with properties and data types
  - Displays relationships and their constraints
  - Supports different output formats (DOT, PNG, SVG)

- **CypherQueryGenerator**: Generates Cypher queries for Neo4j import
  - Creates schema constraints and indexes
  - Generates node creation statements
  - Builds relationship creation statements
  - Preserves property constraints

- **Neo4jConnector**: Manages connections to Neo4j database
  - Establishes database connections
  - Executes Cypher queries
  - Handles transactions
  - Provides error handling

## Transformation Process Architecture

The transformation process in KGSwitch is designed as a pipeline with intermediate representations to ensure accurate and complete conversion between RDF and Property Graph schemas.

### Transformation Steps

The schema transformation process occurs in several steps, implemented in `SchemaTransformationService`:

1. **RDF Schema to RDF Statement Graph**
   - Parses RDF/SHACL schema files
   - Extracts node shapes and property shapes
   - Creates an intermediate statement graph representation

2. **RDF Statement Graph to PG Statement Graph**
   - Converts RDF statements to PG-compatible statements
   - Preserves semantic relationships
   - Maintains property constraints

3. **PG Statement Graph to Property Graph Schema**
   - Generates final property graph schema
   - Creates node and relationship definitions
   - Preserves all constraints and validations

4. **Output Generation**
   - Produces JSON schema representation
   - Generates Cypher queries for Neo4j
   - Creates visualization-ready formats

### Class Responsibilities in Transformation

Each class in the transformation process has specific responsibilities:

#### RDF to Statement Graph Phase
- **RDFSchemaTransformer**:
  - Reads and parses TTL files using Apache Jena
  - Identifies NodeShape and PropertyShape elements
  - Extracts targetClass, property paths, and constraints
  - Creates SchemaNode objects for each type statement
  - Creates SchemaNode objects for each property statement
  - Builds the initial statement graph

#### Statement Graph to Property Graph Phase
- **PGSchemaToStatementTransformer**:
  - Processes type statements to create node definitions
  - Transforms property statements to property constraints
  - Handles relationship statements for edge creation
  - Preserves cardinality constraints
  - Creates a PG-compatible statement graph

- **PGStatementToSchemaTransformer**:
  - Converts statement nodes to schema nodes
  - Processes property constraints
  - Creates relationship definitions
  - Builds the final property graph schema

#### Output Generation Phase
- **JsonSchemaGenerator**:
  - Creates JSON structure for nodes and relationships
  - Formats property constraints in JSON
  - Handles data type mapping
  - Preserves cardinality information

- **CypherQueryGenerator**:
  - Creates Cypher CREATE statements for nodes
  - Generates relationship patterns
  - Builds property constraints
  - Creates uniqueness constraints

#### Bidirectional Transformation
- **RDFStatementTransformer**:
  - Converts PG statement graph back to RDF model
  - Creates SHACL shapes for nodes
  - Builds property shapes with constraints
  - Preserves relationship information

- **StatementToRDFTransformer**:
  - Processes statement graph to create RDF/SHACL
  - Handles type statements, property statements, and edge statements
  - Creates complete RDF model with all constraints

## Visualization

KGSwitch includes robust visualization capabilities to help users understand complex schema structures. The visualization component transforms schema graphs into visual representations that clearly display nodes, relationships, and their properties.

### Schema Visualization

The `GraphVisualizer` class provides the core visualization functionality:

- **Schema Graph Visualization**: Renders complete schema graphs with all nodes and relationships
  - Displays node labels and properties in an easy-to-read tabular format
  - Shows relationships between nodes with their property constraints
  - Uses HTML-based labels to display detailed property information with data types
  - Supports large-scale schema graphs with many entities

- **Property Display**: Visualizes property constraints in a structured format
  - Shows property names with their data types
  - Indicates required properties (based on minCount constraints)
  - Displays property cardinality information
  - Presents nested properties in a hierarchical manner

- **Relationship Visualization**: Clearly depicts relationships between nodes
  - Shows relationship type and direction
  - Displays relationship properties
  - Indicates cardinality constraints on relationships
  - Maintains visual clarity even with complex interconnections

### Neo4j Visualization

KGSwitch provides direct integration with Neo4j for interactive visualization and exploration of schema graphs:

- **Interactive Schema Exploration**: Visualizes schema graphs directly in Neo4j's browser interface
  - Enables interactive exploration of complex schemas
  - Allows zooming, filtering, and focusing on specific parts of the schema
  - Supports schema discovery through intuitive navigation of relationships
  - Provides a dynamic view of the knowledge graph structure

- **Neo4j Integration**: The `Neo4jConnector` class handles all database operations:
  - Establishes secure connections to Neo4j instances
  - Manages authentication and sessions
  - Executes Cypher queries generated from the schema
  - Provides error handling and connection management
  - Supports both local and remote Neo4j instances

- **Schema Import**: Automatically imports the schema graph into Neo4j:
  - Creates node labels with properties and constraints
  - Establishes relationship types with their properties
  - Preserves all cardinality and data type constraints
  - Optimizes the graph with appropriate indexes

- **Customizable Visualization**: The Neo4j visualization offers configurable options:
  - Custom node colors based on node types
  - Control over relationship display
  - Node and relationship property filtering
  - Layout adjustments for optimal viewing

### Visualization Process

The visualization process consists of several key steps:

1. **Schema Graph Processing**:
   - Processes `SchemaGraph` objects with nodes and relationships
   - Extracts labels, properties, and constraints for each node
   - Maps relationships between nodes with their properties

2. **Graph Construction**:
   - Creates a mutable graph representation using the GraphViz library
   - Constructs nodes as HTML tables showing properties and data types
   - Builds edges with relationship types and property information
   - Optimizes layout for readability

3. **Output Generation**:
   - Generates DOT format output for maximum compatibility
   - Supports conversion to various image formats via GraphViz tools
   - Provides clear user instructions for viewing and converting outputs

### Output Formats

KGSwitch supports the following visualization output formats:

- **DOT**: Native GraphViz format that can be viewed with GraphViz tools
  - Provides complete schema representation
  - Can be converted to other formats using GraphViz command-line tools
  - Recommended default format for maximum compatibility

- **PNG/SVG/PDF**: Image formats for documentation and presentation
  - Can be generated from DOT files using the GraphViz dot command
  - `dot -Tpng schema.dot -o schema.png`
  - `dot -Tsvg schema.dot -o schema.svg`
  - `dot -Tpdf schema.dot -o schema.pdf`

- **Neo4j Browser**: Interactive visualization within Neo4j
  - Displays the schema as an interactive, navigable graph
  - Allows querying and exploration of specific subgraphs
  - Supports custom styling and filtering
  - Enables sharing of visualization views with team members

The visualization output shows:
- Nodes with labeled headers
- Property names and their data types in table format
- Relationships with their types and properties
- A clean, structured representation of the entire knowledge graph schema

## Test Use Cases

KGSwitch includes comprehensive test cases to ensure the correctness and robustness of the transformation process.

### Unit Tests

#### RDFSchemaTransformerTest
- **Test Case**: `testTransformToRDF`
  - **Purpose**: Verifies that an empty statement graph produces an empty RDF model
  - **Assertion**: The resulting RDF model should not be null and should be empty

- **Test Case**: `testCardinalityConstraints`
  - **Purpose**: Tests that cardinality constraints are correctly transformed to RDF
  - **Input**: SchemaNode with minCount=1 property constraint
  - **Assertion**: The resulting RDF model should contain a property shape with sh:minCount 1

- **Test Case**: `testDataTypeConstraints`
  - **Purpose**: Verifies that datatype constraints are correctly transformed
  - **Input**: SchemaNode with xsd:string datatype constraint
  - **Assertion**: The resulting property shape should have sh:datatype xsd:string

- **Test Case**: `testNodeShape`
  - **Purpose**: Tests creation of node shapes from type statements
  - **Input**: TypeStatement SchemaNode
  - **Assertion**: The resulting RDF model should contain a sh:NodeShape with correct targetClass

#### RDFStatementTransformerTest
- **Test Case**: `testTransformToRDF`
  - **Purpose**: Tests transformation of an empty statement graph to RDF
  - **Assertion**: The resulting RDF model should not be null and should be empty

- **Test Case**: `testCardinalityConstraints`
  - **Purpose**: Verifies that cardinality constraints are correctly transformed
  - **Input**: PropertyStatement with minCount=1 constraint
  - **Assertion**: The resulting RDF model should contain a shape with the correct constraint

- **Test Case**: `testDataTypeConstraints`
  - **Purpose**: Tests transformation of datatype constraints
  - **Input**: PropertyStatement with xsd:string datatype
  - **Assertion**: The property shape should have the correct datatype property

- **Test Case**: `testNodeShape`
  - **Purpose**: Tests creation of node shapes from type statements
  - **Input**: TypeStatement for FlightReservation
  - **Assertion**: The resulting RDF model should contain a NodeShape with correct type

#### PGSchemaTransformerTest
- **Test Case**: `testTransformToPGSchema`
  - **Purpose**: Tests basic transformation of statement graph to PG schema
  - **Input**: Statement graph with type and property statements
  - **Assertion**: The resulting PG schema should contain nodes with correct labels and properties

- **Test Case**: `testTransformWithRelationship`
  - **Purpose**: Tests transformation of relationships between nodes
  - **Input**: Statement graph with two type statements and an edge statement
  - **Assertion**: The resulting PG schema should contain a relationship with correct type

- **Test Case**: `testTransformWithPropertyConstraints`
  - **Purpose**: Tests preservation of property constraints during transformation
  - **Input**: Statement graph with property constraints (cardinality, data type)
  - **Assertion**: The resulting PG schema should preserve all property constraints

#### PGStatementToSchemaTransformerTest
- **Test Case**: `testTransformToPGSchema`
  - **Purpose**: Tests conversion from statement graph to PG schema
  - **Input**: Statement graph with type and property statements
  - **Assertion**: The resulting PG schema should contain nodes with correct labels and properties

#### StatementGraphTransformerTest
- **Test Case**: `testBasicNodeTransformation`
  - **Purpose**: Tests transformation of basic nodes with properties
  - **Input**: SchemaNode with label and property constraints
  - **Assertion**: Verifies that node properties and labels are preserved

- **Test Case**: `testEdgeTransformation`
  - **Purpose**: Tests transformation of edges with properties
  - **Input**: SchemaEdge connecting two nodes with property constraints
  - **Assertion**: Verifies that edge properties and relationships are preserved

- **Test Case**: `testComplexPropertyConstraints`
  - **Purpose**: Tests handling of complex property constraints
  - **Input**: Node with various cardinality constraints (required, optional, multi-valued)
  - **Assertion**: Verifies that all cardinality constraints are correctly preserved

- **Test Case**: `testNestedProperties`
  - **Purpose**: Tests transformation of nested properties
  - **Input**: Node with nested property structure (address_street, address_city)
  - **Assertion**: Verifies that nested property structure is maintained

- **Test Case**: `testEdgePropertyPreservation`
  - **Purpose**: Tests preservation of edge properties during transformation
  - **Input**: Edge with multiple property constraints and cardinality rules
  - **Assertion**: Verifies that all edge property constraints are preserved

#### SchemaTransformationServiceTest
- **Test Case**: `testFlightSchemaTransformation`
  - **Purpose**: End-to-end test of the transformation service
  - **Input**: flight-schema.ttl (SHACL/RDF schema)
  - **Assertion**: Verifies JSON schema output with correct nodes, relationships, and properties
  - **Specific Checks**:
    - Verifies "underName" relationship between FlightReservation and Person
    - Checks property constraints like bookingTime (DateTime) and bookingAgent (String)
    - Validates cardinality constraints (minCount, maxCount)

### Integration Tests

#### StatementGraphTransformerTest
- **Test Case**: `testTransformBiolinkModel`
  - **Purpose**: Tests transformation of complex biolink model
  - **Input**: biolink_model.shacl.ttl
  - **Assertion**: Verifies preservation of entities, relationships, and constraints

- **Test Case**: `testRoundTripTransformation`
  - **Purpose**: Tests bidirectional transformation (RDF → PG → RDF)
  - **Input**: flight-schema.ttl
  - **Assertion**: Verifies that the final RDF model preserves all original constraints

## Benchmarking

KGSwitch includes a comprehensive benchmarking framework to evaluate the performance and accuracy of schema transformations.

### Performance Metrics
- **Execution Time**: Measures the time taken for RDF to Property Graph (PG) and PG to RDF transformations
  - RDF parsing time
  - Statement graph creation time
  - PG schema generation time
  - Output format generation time

- **Memory Usage**: Tracks memory consumption during transformations
  - Peak memory usage
  - Average memory consumption
  - Memory efficiency across different schema sizes

- **Scalability**: Tests performance with schemas of varying sizes and complexity
  - Small schemas (< 100 nodes)
  - Medium schemas (100-1000 nodes)
  - Large schemas (> 1000 nodes)

### Accuracy Metrics
- **Entity Preservation**: Percentage of entities (classes, node types) preserved during transformation
  - Class/node type count before and after transformation
  - Missing or added entities

- **Relationship Preservation**: Percentage of relationships preserved during transformation
  - Relationship count before and after transformation
  - Relationship type preservation
  - Source/target preservation

- **Property Preservation**: Percentage of properties and attributes preserved during transformation
  - Property count before and after transformation
  - Data type preservation
  - Required property preservation

- **Cardinality Preservation**: Percentage of cardinality constraints (minCount, maxCount) preserved during transformation
  - minCount constraint preservation
  - maxCount constraint preservation
  - Required property identification

### Benchmark Results

The benchmark generates detailed reports including:
- Comparative analysis between different transformation tools (KGSwitch, NeoSemantics)
- Detailed accuracy metrics for each schema component
- Performance statistics across multiple iterations
- Visualizations of transformation quality

#### Cardinality Preservation

A key focus of the benchmark is evaluating how well cardinality constraints are preserved during transformation. The framework:
1. Extracts minCount and maxCount constraints from RDF schemas (SHACL shapes)
2. Tracks how these constraints are represented in the Property Graph model
3. Calculates a cardinality preservation percentage
4. Identifies any lost or modified constraints

This ensures that semantic integrity is maintained throughout the transformation process, particularly for data validation rules.

## Building and Running

### Building the Project

```bash
# Clean and compile
mvn clean compile

# Package
mvn package
```

### Running Transformations

To transform an RDF schema to a Property Graph schema, you need to configure the Maven exec plugin in your pom.xml file and then use the main class with the path to your SHACL file:

1. First, ensure your pom.xml has the exec plugin configured with the main class:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>com.kgswitch.Main</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

2. Then, run the transformation by providing the path to your SHACL file as an argument:

```bash
# Transform a schema file
mvn clean compile exec:java -Dexec.args="/path/to/schema.ttl"
```

Example with a specific schema:

```bash
# Transform the flight schema
mvn clean compile exec:java -Dexec.args="src/test/resources/datasets/flight-schema.ttl"
```

The transformation process will:
1. Read the input SHACL file
2. Transform it to both RDF and Property Graph schemas
3. Generate output files in the same directory as the input file:
   - RDF output: `<input-filename>_transformed.ttl`
   - PG Schema output: `<input-filename>_pg_schema.json`

If the transformation is successful, you'll see a confirmation message indicating where to find the output files.

### Visualizing Schemas

KGSwitch provides two main approaches for schema visualization:

#### Static Image Visualization

Use the following command to generate a DOT file visualization:

```bash
# Generate a visual representation of a schema
mvn clean compile exec:java -Dexec.args="src/test/resources/datasets/schema.ttl --image --output schema.dot"
```

This will:
1. Read the input SHACL file
2. Transform it to a schema graph
3. Generate a DOT file visualization

To convert the DOT file to an image format, use GraphViz:

```bash
# Convert DOT file to PNG
dot -Tpng schema.dot -o schema.png

# Convert DOT file to SVG
dot -Tsvg schema.dot -o schema.svg
```

#### Interactive Neo4j Visualization

For a more interactive experience, you can visualize the schema directly in Neo4j:

```bash
# Visualize schema in Neo4j (using default connection settings)
mvn clean compile exec:java -Dexec.args="src/test/resources/datasets/schema.ttl --neo4j"

# Visualize schema in Neo4j with custom connection settings
mvn clean compile exec:java -Dexec.args="src/test/resources/datasets/schema.ttl --neo4j --uri bolt://localhost:7687 --user neo4j --password your_password"
```

This will:
1. Read the input SHACL file
2. Transform it to a schema graph
3. Connect to the specified Neo4j instance
4. Import the schema using Cypher queries
5. Provide a link to view the visualization in Neo4j Browser

To view the visualization:
1. Open Neo4j Browser (typically at http://localhost:7474)
2. Login with your credentials
3. Run the Cypher query: `MATCH (n) RETURN n`
4. Explore the schema interactively

Both visualization approaches show:
- Nodes with their labels and properties (including data types)
- Relationships between nodes with their constraints
- A structured representation of the entire schema

The Neo4j visualization additionally allows interactive exploration, filtering, and custom views of the schema graph.

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SchemaTransformationServiceTest

# Run specific test method
mvn test -Dtest=RDFSchemaTransformerTest#testCardinalityConstraints
```

### Running Benchmarks

```bash
# Run all benchmarks
mvn clean compile exec:java -Dexec.mainClass="com.kgswitch.benchmark.KGSwitchBenchmark"

# Run benchmark with specific dataset
mvn clean compile exec:java -Dexec.mainClass="com.kgswitch.benchmark.KGSwitchBenchmark"
```

## Dependencies

- **Apache Jena** (4.10.0): RDF processing
- **TopBraid SHACL** (1.4.2): SHACL validation
- **JGraphT** (1.5.2): Graph processing
- **Jackson** (2.15.2): JSON processing
- **GraphViz-Java** (0.18.1): Graph visualization
- **JUnit Jupiter** (5.10.1): Testing
- **SLF4J** (2.0.9): Logging API
- **Logback** (1.4.14): Logging Implementation