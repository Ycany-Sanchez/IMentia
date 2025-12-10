// src/main/java/util/PersonDataManager.java
package util;

import people.Person;
import java.io.*;
import java.util.*;

public class PersonDataManager extends FileHandler {
    private static final String PERSON_FILE = "Person_File.csv";
    private final HashMap<String, Boolean> personMap = new HashMap<>();

    @Override
    public boolean save(Object obj) {
        if (!(obj instanceof List)) return false;
        return savePersons((List<Person>) obj);
    }

    @Override
    public Object load() {
        return loadPersonFile();
    }

    public boolean savePersons(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        boolean savedNewPerson = false;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            for (Person p : persons) {
                if (!personMap.containsKey(capitalizeLabel(p.getName()))) {
                    bw.write(p.getId() + "," + capitalizeLabel(p.getName()) + "," + capitalizeLabel(p.getRelationship()) + "\n");
                    personMap.put(capitalizeLabel(p.getName()), true);
                    savedNewPerson = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savedNewPerson;
    }

    public List<Person> loadPersonFile() {
        List<Person> personList = new ArrayList<>();
        File file = new File(DATA_FOLDER, PERSON_FILE);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String temp;
            while ((temp = br.readLine()) != null) {
                String[] arr = temp.split(",");
                personMap.put(capitalizeLabel(arr[1]), true);
                Person p = new Person(capitalizeLabel(arr[1]), capitalizeLabel(arr[2]));
                p.setId(arr[0]);
                personList.add(p);
            }
        } catch (IOException e) {
            System.out.println("Empty file.");
        }
        return personList;
    }

    public void updatePersonFile(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        personMap.clear();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (Person p : persons) {
                bw.write(p.getId() + "," + capitalizeLabel(p.getName()) + "," + capitalizeLabel(p.getRelationship()) + "\n");
                personMap.put(capitalizeLabel(p.getName()), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateId(List<Person> persons) {
        int maxID = 0;
        for (Person p : persons) {
            String ID = p.getId().substring(6);
            maxID = Integer.parseInt(ID);
        }
        return "Person" + (maxID + 1);
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
}
