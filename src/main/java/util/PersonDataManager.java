// src/main/java/util/PersonDataManager.java
package util;

import people.Person;
import java.io.*;
import java.util.*;

public class PersonDataManager extends FileHandler
{
    // CSV file for person data
    private static final String PERSON_FILE = "Person_File.csv";

    // In-memory map to track persons by name
    private final Map<String, Boolean> personMap = new HashMap<>();

    // Save a list of persons (append mode if needed)
    public boolean savePersons(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        boolean savedNewPerson = false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            for (Person p : persons) {
                String name = FileHandler.capitalizeLabel(p.getName());
                if (!personMap.containsKey(name)) {
                    bw.write(p.getId() + "," + name + "," +
                            FileHandler.capitalizeLabel(p.getRelationship()) + "\n");
                    personMap.put(name, true);
                    savedNewPerson = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savedNewPerson;
    }

    public List<Person> loadPersons() {
        List<Person> personList = new ArrayList<>();
        File file = new File(DATA_FOLDER, PERSON_FILE);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split(",");
                String name = FileHandler.capitalizeLabel(arr[1]);
                String relationship = FileHandler.capitalizeLabel(arr[2]);

                personMap.put(name, true);

                Person p = new Person(name, relationship);
                p.setId(arr[0]);
                personList.add(p);
            }
        } catch (IOException e) {
            System.out.println("Empty or missing file.");
        }
        return personList;
    }

    public void updatePersonFile(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        personMap.clear();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (Person p : persons) {
                bw.write(p.getId() + "," +
                        FileHandler.capitalizeLabel(p.getName()) + "," +
                        FileHandler.capitalizeLabel(p.getRelationship()) + "\n");
                personMap.put(FileHandler.capitalizeLabel(p.getName()), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
