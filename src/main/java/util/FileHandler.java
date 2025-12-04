// >>> FILE: src/main/java/util/FileHandler.java
package util;

import people.Person;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling file-based persistence of the application's Person data
 * using standard Java serialization.
 */
public class FileHandler {
    private static final String DATA_FOLDER = "imentia_data";
    private static final String PERSONS_FILE = "persons.dat";
    private static final String ID_GENERATOR = "IDGen";

    public FileHandler() {
        System.out.println("FileHandler created");
        // Ensure the data directory exists
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            System.out.println("Creating data folder: " + DATA_FOLDER);
            folder.mkdir();
        }
    }

    /**
     * Saves the current list of Person objects to the persons.dat file.
     * @param persons The list of Person objects to serialize.
     */
    public void savePersons(List<Person> persons) {
        try {
            File file = new File(DATA_FOLDER, PERSONS_FILE);
            System.out.println("Saving " + persons.size() + " person(s) to " + file.getAbsolutePath());

            // Use ObjectOutputStream to serialize the List<Person>
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(persons);
            oos.close();
            System.out.println("✓ Save successful");
        } catch (IOException e) {
            System.out.println("✗ Error saving persons:");
            e.printStackTrace();
        }
    }
//
    public static <bw> String generateId(){
        try(BufferedReader br = new BufferedReader(new FileReader("PersonDetails.csv"));
        BufferedWriter bw = new BufferedWriter(new FileWriter("PersonDetails.csv", true))){

        }catch(FileNotFoundException e){
            System.out.println("Murag Siyag uyab nimo, dont exist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the list of Person objects from the persons.dat file.
     * @return The deserialized list of Person objects, or an empty list if loading fails.
     */
    @SuppressWarnings("unchecked")
    public List<Person> loadPersons() {
        File file = new File(DATA_FOLDER, PERSONS_FILE);
        System.out.println("Loading persons from: " + file.getAbsolutePath());

        if (!file.exists()) {
            System.out.println("File does not exist - returning empty list");
            return new ArrayList<>();
        }

        try {
            // Use ObjectInputStream to deserialize the List<Person>
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            List<Person> persons = (List<Person>) ois.readObject();
            ois.close();
            System.out.println("✓ Loaded " + persons.size() + " person(s)");
            return persons;
        } catch (IOException | ClassNotFoundException e) {
            // Handle file errors or class version mismatch errors
            System.out.println("✗ Error loading persons:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}