// src/main/java/util/FileHandler.java
package util;

import java.io.File;

public abstract class FileHandler {
    protected static final String DATA_FOLDER = "imentia_data";

    public FileHandler() {
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public String getDataFolder() {
        return DATA_FOLDER;
    }

    // Subclasses must implement their own save/load
    public abstract boolean save(Object obj);
    public abstract Object load();
}
