package util;

import people.Person;
import java.io.File;
import java.util.List;

public abstract class FileHandler {
    protected static final String DATA_FOLDER = "imentia_data";

    // Common utility: Ensure data folder exists
    public FileHandler() {
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public String getDataFolder() {
        return DATA_FOLDER;
    }

    // Common utility: Generate ID
    public String generateId(List<Person> persons) {
        int maxId = 0;
        for (Person p : persons) {
            try {
                int id = Integer.parseInt(p.getId());
                if (id > maxId) maxId = id;
            } catch (NumberFormatException e) {
                // Ignore invalid IDs
            }
        }
        return String.valueOf(maxId + 1);
    }

    // Common utility: Capitalize
    public static String capitalizeLabel(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}