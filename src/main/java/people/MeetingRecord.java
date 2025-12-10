package people;
import util.FileHandler;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate; // import the LocalDate clas
import java.time.LocalDateTime;
import java.time.LocalTime; // import the LocalTime class
import java.time.format.DateTimeFormatter;


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
            boolean created = folder.mkdirs(); // mkdirs() creates parent directories too
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
        DateTimeFormatter formatter12 = DateTimeFormatter.ofPattern("hh:mm a");
        String timeIn12H = time.format(formatter12);
        LocalDate date = LocalDate.now();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){
            bw.write("----- NOTE START -----\n");
            bw.write(date + "\n");
            bw.write(timeIn12H + "\n\n");
            bw.write(conv + "\n");   // conv already contains all user-entered newlines
            bw.write("----- NOTE END -----\n");
            bw.write("\n"); // spacing between notes
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
