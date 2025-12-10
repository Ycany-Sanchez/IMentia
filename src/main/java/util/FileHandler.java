// >>> FILE: src/main/java/util/FileHandler.java
package util;

import people.Person;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


/**
 * Utility class for handling file-based persistence of the application's Person data
 * using standard Java serialization.
 */
public class FileHandler {
    private static final String DATA_FOLDER = "imentia_data";
    //private static final String PERSONS_FILE = "persons.dat";
    //private static final String ID_GENERATOR = "IDGen";
    private static final String PERSON_FILE = "Person_File.csv";
    private final HashMap<String, Boolean> personMap = new HashMap<String, Boolean>();

    public FileHandler() {
        System.out.println("FileHandler created");
        // Ensure the data directory exists
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            System.out.println("Creating data folder: " + DATA_FOLDER);
            folder.mkdirs();
        }
    }



    public boolean savePersons(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);
        boolean savedNewPerson = false;
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))){
            for (Person p : persons){
                if(!personMap.containsKey(capitalizeLabel(p.getName()))){
                    bw.write(p.getId() + "," + capitalizeLabel(p.getName()) +  "," +  capitalizeLabel(p.getRelationship()) + "\n");
                    personMap.put(capitalizeLabel(p.getName()), true);
                    savedNewPerson = true;
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        return savedNewPerson;
    }

    public String generateId(List<Person> persons){
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


    public static String capitalizeLabel(String s){
        if (s == null || s.isEmpty()) return s;
        s = s.toLowerCase();
        //return s.substring(0, 1).toUpperCase() + s.substring(1);
        s.substring(0, 1).toUpperCase();
        String temp = "";
        temp += s.substring(0, 1).toUpperCase();
        for (int i = 1; i<s.length(); i++){
            if(Character.isWhitespace(s.charAt(i-1))){
                temp+=s.substring(i, i+1).toUpperCase();
            } else {
                temp+=s.charAt(i);
            }
        }
        return temp;
    }


    public List<Person> loadPersonFile(){
        List<Person> personList = new ArrayList<>();
        File file = new File(DATA_FOLDER, PERSON_FILE);
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String temp;
            while((temp = br.readLine())!=null){
                String[] arr = temp.split(",");

                personMap.put(capitalizeLabel(arr[1]), true);
                String id = arr[0];
                Person p = new Person(capitalizeLabel(arr[1]), capitalizeLabel(arr[2]));
                p.setId(id);
                try {
                    String directoryPath = Paths.get(DATA_FOLDER, "imentia_data/saved_faces/").toString();
                    String filePath = Paths.get(directoryPath, id + ".png").toString();
                    File imgFile = new File(filePath);
                    if(imgFile.exists()){
                        BufferedImage img = ImageIO.read(imgFile);
                        p.setPersonImage(img);
                    }
                } catch (IOException e){
                    System.out.println("Could not find image");
                }
                personList.add(p);
            }
        } catch (IOException e){
            System.out.println("Empty file.");
        }
        return personList;
    }

    public String getDataFolder(){
        return DATA_FOLDER;
    }

    // In util/FileHandler.java

    public void updatePersonFile(List<Person> persons) {
        File file = new File(DATA_FOLDER, PERSON_FILE);

        personMap.clear();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (Person p : persons) {
                String key = capitalizeLabel(p.getName());

                bw.write(p.getId() + "," + capitalizeLabel(p.getName()) + "," + capitalizeLabel(p.getRelationship()) + "\n");

                personMap.put(key, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
