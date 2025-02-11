package com.kgswitch.core;

import java.nio.file.Path;

public class SchemaTransformationException extends Exception {
    private final Path schemaFile;

    public SchemaTransformationException(String message, Path schemaFile, Throwable cause) {
        super(message, cause);
        this.schemaFile = schemaFile;
    }

    public Path getSchemaFile() {
        return schemaFile;
    }

    @Override
    public String toString() {
        return "SchemaTransformationException{" +
                "message=" + getMessage() +
                ", schemaFile=" + schemaFile +
                ", cause=" + getCause() +
                '}';
    }
}