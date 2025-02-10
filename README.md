# KGSwitch

## Overview

KGSwitch is a Java-based tool for transforming and validating schemas between RDF (Resource Description Framework) and Property Graph representations.

## Project Configuration

### Prerequisites

- Java 17
- Maven 3.6+

### Dependencies

The project uses the following key libraries:

- Apache Jena (4.10.0)
  - RDF processing and manipulation
- TopBraid SHACL (1.4.2)
  - SHACL validation
- JGraphT (1.5.2)
  - Graph processing
- JUnit Jupiter (5.10.1)
  - Unit testing

### Build Configuration

#### Java Version
- Source and Target Compatibility: Java 17
- Encoding: UTF-8

## Project Structure

```
kgswitch/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/kgswitch/
│   │           ├── models/
│   │           └── transforms/
│   └── test/
│       ├── java/
│       │   └── com/kgswitch/transforms/
│       └── resources/
└── pom.xml
```

## Building the Project

To build the project, use Maven:

```bash
# Clean the project
mvn clean

# Compile the source code
mvn compile

# Run tests
mvn test

# Package the application
mvn package
```

## Running Tests

```bash
mvn test
```

## Logging

The project uses:
- SLF4J API (2.0.9)
- Logback Classic (1.4.14)

## Key Modules

- `models/`: Core data models for schema representation
  - `SchemaGraph`
  - `SchemaNode`
  - `SchemaEdge`
  - `PropertyConstraint`

- `transforms/`: Schema transformation utilities
  - `RDFSchemaTransformer`: Converts RDF/SHACL schemas to custom graph representation
  - `StatementGraphTransformer`: Transforms between different graph schemas

## Sample Usage

```java
// Create a transformer
RDFSchemaTransformer transformer = new RDFSchemaTransformer();

// Transform RDF schema to statement graph
SchemaGraph statementGraph = transformer.transformToStatementGraph("path/to/schema.ttl");

// Validate an RDF data model
boolean isValid = transformer.validateSHACL(dataModel);
```


