// >>> FILE: src/main/java/people/MeetingRecord.java (Updated)

package people;
import util.FileHandler;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate; // import the LocalDate clas
import java.time.LocalDateTime;
import java.time.LocalTime; // import the LocalTime class
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import util.FileHandler;
import util.PersonDataManager;
import java.time.temporal.ChronoUnit;

public class MeetingRecord {

    private Person p;
    private String conv;
    private final String FolderName = "Meeting_Notes";
    private FileHandler fileHandler;


    public MeetingRecord(Person p, String conv) {
        this.p = p;
        this.conv = conv;
        this.fileHandler = new PersonDataManager();
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

    /**
     * Reads all notes for the current person from the file system.
     * @return A list of strings, where each string is a complete note block.
     * @throws IOException if file reading fails.
     */
    public List<String> readAllNotes() throws IOException {
        String filePath = Paths.get(fileHandler.getDataFolder(), FolderName, p.getId() + ".txt").toString();
        File notesFile = new File(filePath);

        List<String> allNotes = new ArrayList<>();

        if (notesFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                StringBuilder currentNote = null;
                boolean isReadingNote = false;

                while ((line = br.readLine()) != null) {
                    if (line.equals("----- NOTE START -----")) {
                        currentNote = new StringBuilder();
                        isReadingNote = true;
                        currentNote.append(line).append("\n"); // Include the start delimiter
                    } else if (line.equals("----- NOTE END -----")) {
                        if (currentNote != null) {
                            currentNote.append(line).append("\n"); // Include the end delimiter
                            allNotes.add(currentNote.toString());
                        }
                        isReadingNote = false;
                        currentNote = null;
                    } else if (isReadingNote) {
                        currentNote.append(line).append("\n");
                    }
                }
            }
        }
        return allNotes;
    }
}