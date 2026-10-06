package util;

import people.Person;
import java.io.*;
import java.util.*;

public class PersonDataManager extends FileHandler {
    private static final String PERSON_FILE = "Person_File.csv";
    private final Map<String, Boolean> personMap = new HashMap<>();

    public boolean savePersons(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        boolean savedNewPerson = false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            for (Person p : persons) {
                String name = FileHandler.capitalizeLabel(p.getName());
                if (!personMap.containsKey(name)) {
                    bw.write(FileHandler.escapeCsv(p.getId()) + "," + FileHandler.escapeCsv(name) + "," +
                            FileHandler.escapeCsv(FileHandler.capitalizeLabel(p.getRelationship())) + "\n");
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
                if (line.isBlank()) {
                    continue;
                }
                String[] arr = FileHandler.parseCsvLine(line);
                if (arr.length < 3) {
                    System.out.println("Skipping malformed line in Person_File.csv: " + line);
                    continue;
                }
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
                bw.write(FileHandler.escapeCsv(p.getId()) + "," +
                        FileHandler.escapeCsv(FileHandler.capitalizeLabel(p.getName())) + "," +
                        FileHandler.escapeCsv(FileHandler.capitalizeLabel(p.getRelationship())) + "\n");
                personMap.put(FileHandler.capitalizeLabel(p.getName()), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
