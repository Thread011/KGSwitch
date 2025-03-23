package com.kgswitch.util;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.exceptions.Neo4jException;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for connecting to Neo4j and executing Cypher queries.
 * This enables direct visualization of property graph schemas in Neo4j.
 */
public class Neo4jConnector implements Closeable {
    private final Driver driver;
    
    /**
     * Creates a new Neo4j connector with default localhost configuration.
     */
    public Neo4jConnector() {
        this("bolt://localhost:7687", "neo4j", "password");
    }
    
    /**
     * Creates a new Neo4j connector with custom connection parameters.
     * 
     * @param uri The Neo4j server URI
     * @param username The username for authentication
     * @param password The password for authentication
     */
    public Neo4jConnector(String uri, String username, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
    
    /**
     * Execute a single Cypher query.
     * 
     * @param cypher The Cypher query to execute
     * @return Result summary message
     */
    public String executeCypher(String cypher) {
        try (Session session = driver.session()) {
            return session.writeTransaction(tx -> {
                Result result = tx.run(cypher);
                return "Query executed, affected " + 
                       result.consume().counters().nodesCreated() + " nodes and " +
                       result.consume().counters().relationshipsCreated() + " relationships";
            });
        } catch (Neo4jException e) {
            return "Query failed: " + e.getMessage();
        }
    }
    
    /**
     * Execute multiple Cypher queries from a string.
     * Statements are split by semicolons.
     * 
     * @param cypher Multiple Cypher queries as a string
     * @return List of result summary messages
     */
    public List<String> executeMultipleCypherStatements(String cypher) {
        List<String> results = new ArrayList<>();
        String[] statements = cypher.split(";");
        
        try (Session session = driver.session()) {
            for (String stmt : statements) {
                final String statement = stmt.trim();
                if (statement.isEmpty() || statement.startsWith("//")) {
                    continue;
                }
                
                try {
                    String result = session.writeTransaction(tx -> {
                        Result queryResult = tx.run(statement);
                        return "Query executed successfully";
                    });
                    results.add(result);
                } catch (Neo4jException e) {
                    results.add("Query failed: " + e.getMessage());
                }
            }
        }
        
        return results;
    }
    
    /**
     * Execute Cypher queries from a file.
     * 
     * @param cypherFile Path to file containing Cypher queries
     * @return List of result summary messages
     * @throws Exception If file cannot be read
     */
    public List<String> executeCypherFromFile(String cypherFile) throws Exception {
        String cypher = Files.readString(Paths.get(cypherFile));
        return executeMultipleCypherStatements(cypher);
    }
    
    /**
     * Load and visualize a JSON schema file in Neo4j.
     * 
     * @param jsonSchemaFile Path to the JSON schema file
     * @return Result summary message
     * @throws Exception If file cannot be read or queries fail
     */
    public String visualizeJsonSchema(String jsonSchemaFile) throws Exception {
        CypherQueryGenerator generator = new CypherQueryGenerator();
        String cypher = generator.generateCypherFromFile(jsonSchemaFile);
        
        List<String> results = executeMultipleCypherStatements(cypher);
        
        // Set the style for nodes in Neo4j Browser to use our color property
        setNodeStyling();
        
        StringBuilder summary = new StringBuilder("Schema visualization complete:\n");
        
        for (String result : results) {
            summary.append("- ").append(result).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Sets Neo4j Browser styling to use the color property for nodes
     */
    private void setNodeStyling() {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                // This Cypher sets the style for Neo4j Browser visualization
                // The style uses the color property to determine node color
                String browserStyleCypher = 
                    "CALL apoc.meta.graphSample(100) " +
                    "YIELD nodes, relationships " +
                    "WITH nodes, relationships " +
                    "CALL db.schema.visualization() " +
                    "YIELD nodes as schemaNodes, relationships as schemaRels " +
                    "RETURN 'BROWSER STYLE: node {color: color, caption: displayName}' as style";
                
                Result result = tx.run(browserStyleCypher);
                return result.list();
            });
        } catch (Exception e) {
            // If this fails, it's not critical - just log it
            System.out.println("Note: Browser styling could not be set: " + e.getMessage());
        }
        
        // Alternative approach using Call stream for Neo4j 4.4+
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                // Execute the browser style command directly
                String grassStyleQuery = 
                    "CALL db.schema.visualization() " +
                    "YIELD nodes, relationships " +
                    "CALL apoc.export.grass.style(nodes, relationships, {}) " + 
                    "YIELD cypherStyle " +
                    "RETURN cypherStyle";
                
                Result result = tx.run(grassStyleQuery);
                return result.list();
            });
        } catch (Exception e) {
            // If APOC is not available or other error occurs, try simpler approach
            try (Session session = driver.session()) {
                session.writeTransaction(tx -> {
                    // Basic query to set node color styling using APOC when available
                    // This is a simple fallback that may or may not work depending on Neo4j setup
                    String basicStyleQuery = 
                        "MATCH (n) WHERE n.color IS NOT NULL " +
                        "CALL apoc.create.setProperty(n, '__style__', 'node { color: ' + n.color + '; }') " +
                        "YIELD node RETURN count(node)";
                    
                    Result result = tx.run(basicStyleQuery);
                    return result.list();
                });
            } catch (Exception ex) {
                // Final fallback - ignore if even this fails
                System.out.println("Note: Alternative browser styling could not be set");
            }
        }
    }
    
    /**
     * Close the Neo4j driver connection.
     */
    @Override
    public void close() {
        driver.close();
    }
} 