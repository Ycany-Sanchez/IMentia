package util;

import people.Person;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PersonDataManager extends FileHandler {
    private static final String FILE_NAME = "Person_File.csv";

    public List<Person> loadPersonFile() {
        List<Person> persons = new ArrayList<>();
        File file = Paths.get(DATA_FOLDER, FILE_NAME).toFile();

        if (!file.exists()) return persons;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    Person p = new Person(parts[1], parts[2]);
                    p.setId(parts[0]);
                    persons.add(p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return persons;
    }

    public boolean savePersons(List<Person> persons) {
        File file = Paths.get(DATA_FOLDER, FILE_NAME).toFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (Person p : persons) {
                bw.write(p.getId() + "," + p.getName() + "," + p.getRelationship());
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}