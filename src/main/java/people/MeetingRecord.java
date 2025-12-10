package people;
import util.FileHandler;

import java.io.*;
import java.nio.file.Paths;

import util.FileHandler;

public class MeetingRecord {


    private Person p;
    private String conv;
    private final String FolderName = "Meeting_Notes";
    private FileHandler fileHandler;


    public MeetingRecord(Person p, String conv) {
        this.p = p;
        this.conv = conv;
        this.fileHandler = new FileHandler();
        File folder = new File(Paths.get(fileHandler.getDataFolder(),FolderName).toString());
        if (!folder.exists()) {
            System.out.println("Creating data folder: " + FolderName);
            folder.mkdir();
        }
    }
    public void createFile() {

        String fileName = Paths.get(fileHandler.getDataFolder(), FolderName, p.getId()  + ".txt").toString();
        boolean fileExists = new File(fileName).exists();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){

            if (p.getMeetingRecord() == null) {
                bw.write(p + "\n\n" + conv + "\n\n");
            } else {
                bw.write(conv + "\n\n");
            }
        } catch (IOException e) {}
    }

}
