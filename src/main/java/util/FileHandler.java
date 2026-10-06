package util;

import people.Person;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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


    public static String capitalizeLabel(String s) {
        if (s == null || s.isEmpty()) return s;
        s = s.toLowerCase();
        StringBuilder temp = new StringBuilder();
        temp.append(Character.toUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i - 1))) {
                temp.append(Character.toUpperCase(s.charAt(i)));
            } else {
                temp.append(s.charAt(i));
            }
        }
        return temp.toString();
    }

    public static String generateId(List<Person> persons) {
        int maxID = 0;
        for (Person p : persons) {
            if (p == null || p.getId() == null) {
                continue;
            }
            String id = p.getId();
            if (!id.startsWith("Person")) {
                continue;
            }
            try {
                int integerID = Integer.parseInt(id.substring("Person".length()));
                if (integerID > maxID) {
                    maxID = integerID;
                }
            } catch (NumberFormatException e) {
                // Malformed id (e.g. test ids like "PersonTest-xxxx"): ignore it.
            }
        }
        return "Person" + (maxID + 1);
    }

    /**
     * Quotes a CSV field when it contains a comma, quote or newline.
     * Plain values (ids, normal names) are written unchanged, so existing
     * Person_File.csv files stay compatible.
     */
    public static String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * Splits one CSV line, honouring double-quoted fields.
     * Lines written without quotes parse exactly like String.split(",").
     */
    public static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }
}
