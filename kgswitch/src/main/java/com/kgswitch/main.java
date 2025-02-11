package com.kgswitch;

import com.kgswitch.core.SchemaWatcher;

public class main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar kgswitch.jar <schemas-directory>");
            System.exit(1);
        }

        try {
            SchemaWatcher watcher = new SchemaWatcher(args[0]);
            watcher.startWatching();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}