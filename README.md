# KGSwitch

## Overview

KGSwitch is a Java-based tool for transforming and validating schemas between RDF (Resource Description Framework) and Property Graph representations. It provides bidirectional transformations through an intermediate layer, allowing users to leverage the best features of both paradigms.

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
│   │       │   └── graph/
│   │       │       ├── SchemaEdge.java
│   │       │       ├── SchemaGraph.java
│   │       │       └── SchemaNode.java
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
│   │       └── util/
│   │           ├── CypherQueryGenerator.java
│   │           ├── JsonSchemaGenerator.java
│   │           ├── TripleCreator.java
│   │           └── main.java
│   └── test/
│       ├── java/com/kgswitch/transforms/
│       │   ├── PGSchemaTransformerTest.java
│       │   ├── PGStatementToSchemaTransformerTest.java
│       │   ├── RDFSchemaTransformerTest.java
│       │   ├── RDFStatementTransformerTest.java
│       │   ├── SchemaTransformationServiceTest.java
│       │   └── StatementGraphTransformerTest.java
│       └── resources/
│           ├── flight-instance.ttl
│           └── flight-schema.ttl
└── pom.xml
```

## Core Components

### Core Services
- **SchemaTransformationService**: Central service orchestrating the entire transformation pipeline:
  - Manages RDF → Statement Graph → Property Graph conversions
  - Handles validation at each transformation step
  - Generates output formats (JSON, Cypher)

- **SchemaWatcher**: Monitors schema files for changes and triggers transformations
  - Watches directories for new/modified schema files
  - Initiates transformation process automatically
  - Provides real-time schema conversion

- **SchemaTransformationException**: Custom exception handling for transformation errors

### Models
- **SchemaGraph**: Core graph representation containing:
  - Nodes and edges collections
  - Graph manipulation methods
  - Validation logic

- **SchemaNode**: Node model with:
  - Labels and properties
  - Property constraints
  - Relationship definitions

- **SchemaEdge**: Edge model defining:
  - Source and target nodes
  - Edge properties and constraints
  - Cardinality rules

- **PropertyConstraint**: Defines constraints for properties:
  - Data types
  - Cardinality (min/max)
  - Validation rules

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
- **CypherQueryGenerator**: Generates Cypher queries for Neo4j
- **JsonSchemaGenerator**: Creates JSON schema representations
- **TripleCreator**: Helps create RDF triples
- **main**: Application entry point

## Transformation Process

The schema transformation process occurs in several steps, implemented in `SchemaTransformationService`:

### 1. RDF to RDF Statement Graph
```java
// Step 1: Transform RDF schema to statement graph representation
SchemaGraph rdfStatementGraph = rdfTransformer.transformToStatementGraph(schemaFile);
```
Converts SHACL/RDF schema into an intermediate statement graph representation. For example:
```turtle
# Input RDF/SHACL
schema:FlightReservationShape
    a sh:NodeShape ;
    sh:targetClass schema:FlightReservation ;
    sh:property [
        sh:path schema:reservationId ;
        sh:datatype xsd:string ;
        sh:minCount 1 ;
    ] .
```

### 2. RDF Statement Graph to PG Statement Graph
```java
// Step 2: Transform RDF statement graph to PG statement graph
PGSchemaToStatementTransformer pgTransformer = new PGSchemaToStatementTransformer(rdfStatementGraph);
SchemaGraph pgStatementGraph = pgTransformer.transformToStatementGraph();
```
Converts the RDF statement representation into a property graph-compatible statement structure, preserving:
- Node types and properties
- Edge relationships
- Property constraints (cardinality, data types)

### 3. PG Statement Graph to Property Graph Schema
```java
// Step 3: Transform PG statement graph to final PG schema
PGStatementToSchemaTransformer schemaTransformer = new PGStatementToSchemaTransformer(pgStatementGraph);
SchemaGraph pgSchema = schemaTransformer.transformToPGSchema();
```
Generates the final property graph schema with:
- Node definitions with labels
- Relationship types with properties
- Property constraints and validations

### Example Output
The transformation produces a JSON schema representing the property graph:
```json
{
  "nodes": [
    {
      "label": "FlightReservation",
      "properties": {
        "reservationId": {
          "type": "String",
          "minCount": 1,
          "maxCount": 1
        }
      }
    }
  ],
  "relationships": [
    {
      "type": "UNDERNAME",
      "source": "FlightReservation",
      "target": "Person",
      "properties": {
        "bookingTime": {
          "type": "DateTime",
          "minCount": 1
        }
      }
    }
  ]
}
```

## Validation
The service includes validation at each transformation step:
```java
private void validateGraph(SchemaGraph graph, String phase) {
    if (graph == null) {
        throw new IllegalStateException(phase + " produced null graph");
    }
    // Validates node and edge counts
    System.out.println(phase + " contains " + graph.getNodes().size() + 
                      " nodes and " + graph.getEdges().size() + " edges");
}
```

## Building and Testing

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package
```

## Dependencies

- **Apache Jena** (4.10.0): RDF processing
- **TopBraid SHACL** (1.4.2): SHACL validation
- **JGraphT** (1.5.2): Graph processing
- **JUnit Jupiter** (5.10.1): Testing
- **SLF4J** (2.0.9): Logging API
- **Logback** (1.4.14): Logging Implementation