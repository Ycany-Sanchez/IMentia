package util;

import people.Person;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private static final String DATA_FOLDER = "imentia_data";
    private static final String PERSONS_FILE = "persons.dat";

    public FileHandler() {
        System.out.println("FileHandler created");
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            System.out.println("Creating data folder: " + DATA_FOLDER);
            folder.mkdir();
        }
    }

    public void savePersons(List<Person> persons) {
        try {
            File file = new File(DATA_FOLDER, PERSONS_FILE);
            System.out.println("Saving " + persons.size() + " person(s) to " + file.getAbsolutePath());

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(persons);
            oos.close();

            System.out.println("✓ Save successful");
        } catch (IOException e) {
            System.out.println("✗ Error saving persons:");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Person> loadPersons() {
        File file = new File(DATA_FOLDER, PERSONS_FILE);
        System.out.println("Loading persons from: " + file.getAbsolutePath());

        if (!file.exists()) {
            System.out.println("File does not exist - returning empty list");
            return new ArrayList<>();
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            List<Person> persons = (List<Person>) ois.readObject();
            ois.close();
            System.out.println("✓ Loaded " + persons.size() + " person(s)");
            return persons;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("✗ Error loading persons:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}