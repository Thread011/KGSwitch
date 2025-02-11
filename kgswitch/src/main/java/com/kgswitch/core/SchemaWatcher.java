package com.kgswitch.core;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.IOException;

public class SchemaWatcher {
    private final Path schemasDir;
    private final WatchService watchService;
    private final SchemaTransformationService transformationService;

    public SchemaWatcher(String schemasDirPath) throws IOException {
        this.schemasDir = Paths.get(schemasDirPath);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.transformationService = new SchemaTransformationService();
        
        schemasDir.register(watchService, ENTRY_CREATE);
    }

    public void startWatching() {
        System.out.println("Watching for schema files in: " + schemasDir);
        
        while (true) {
            WatchKey key;
            try {
                key = watchService.take(); // Blocks until events occur
            } catch (InterruptedException e) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                
                if (fileName.toString().endsWith(".ttl")) {
                    processSchemaFile(schemasDir.resolve(fileName));
                }
            }

            if (!key.reset()) {
                break;
            }
        }
    }

    private boolean isTransformedFile(Path path) {
        String filename = path.getFileName().toString();
        return filename.contains("_transformed") || 
               filename.contains("_cypher");
    }
    
    private void processSchemaFile(Path schemaFile) {
        try {
            // Skip if this is a transformed file
            if (isTransformedFile(schemaFile)) {
                return;
            }

            System.out.println("Processing new schema file: " + schemaFile);
            transformationService.transformSchema(schemaFile);
        } catch (Exception e) {
            System.err.println("Error processing schema file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SchemaWatcher <schemas-directory>");
            System.exit(1);
        }

        try {
            SchemaWatcher watcher = new SchemaWatcher(args[0]);
            watcher.startWatching();
        } catch (IOException e) {
            System.err.println("Error starting schema watcher: " + e.getMessage());
            System.exit(1);
        }
    }
}