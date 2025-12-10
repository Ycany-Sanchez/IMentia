// >>> FILE: src/main/java/people/MeetingRecord.java
package people;

import util.FileHandler;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

public class MeetingRecord {

    private Person p;
    private String conv;
    private final String FolderName = "Meeting_Notes";
    private FileHandler fileHandler;

    public MeetingRecord(Person p, String conv) {
        this.p = p;
        this.conv = conv;
        this.fileHandler = new FileHandler();
        File folder = Paths.get(fileHandler.getDataFolder(), FolderName).toAbsolutePath().toFile();

        System.out.println("[MeetingRecord] Targeted Folder Path: " + folder.getAbsolutePath());
        System.out.println("[MeetingRecord] Does folder exist? " + folder.exists());

        if (!folder.exists()) {
            System.out.println("Attempting to create directory at: " + folder.getAbsolutePath());
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Directory created successfully.");
            } else {
                System.err.println("FAILED to create directory.");
            }
        }
    }

    public void createFile() {
        String fileName = Paths.get(fileHandler.getDataFolder(), FolderName, p.getId() + ".txt").toString();
        LocalTime time = LocalTime.now();
        LocalDate date = LocalDate.now();

        // UPDATED: Proper exception handling
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){
            bw.write("----- NOTE START -----\n");
            bw.write(date + "\n");
            bw.write(time + "\n");
            bw.write(conv + "\n");
            bw.write("----- NOTE END -----\n");
            bw.write("\n");
        } catch (IOException e) {
            System.err.println("Error writing meeting record to file: " + fileName);
            e.printStackTrace();
        }
    }
}