package util;

import people.Person;

import java.io.File;
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
            String ID = p.getId().substring(6);
            int integerID = Integer.parseInt(ID);
            if(integerID > maxID)
                maxID = integerID;
        }
        return "Person" + (maxID + 1);
    }
}
