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
    private static int countOfPersons = 0;
    private static final String DATA_FOLDER = "imentia_data";
    //private static final String PERSONS_FILE = "persons.dat";
    //private static final String ID_GENERATOR = "IDGen";
    private static final String PERSON_FILE = "Person_File.csv";

    public FileHandler() {
        System.out.println("FileHandler created");
        // Ensure the data directory exists
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            System.out.println("Creating data folder: " + DATA_FOLDER);
            folder.mkdir();
        }
    }

    public void savePersons(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            for (Person p : persons){
                bw.write(p.getId() + "," + p.getName() +  "," +  p.getRelationship() + "\n");
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static String generateId(List<Person> persons){
        int maxID = 0;
        if(!persons.isEmpty()) {
            for (Person p : persons) {
                String ID = p.getId().substring(6);
                int idNumber = Integer.parseInt(ID);
                maxID = idNumber;
            }
        }
        maxID++;
        return "Person" + maxID;
    }


    public List<Person> loadPersonFile(){
        List<Person> personList = new ArrayList<>();
        File file = new File(DATA_FOLDER, PERSON_FILE);
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String temp;
            while((temp = br.readLine())!=null){
                String[] arr = temp.split(",");
                Person p = new Person(arr[1], arr[2]);
                p.setId(arr[0]);
                personList.add(p);
            }
        } catch (IOException e){
            System.out.println("Empty file.");
        }
        return personList;
    }
}
