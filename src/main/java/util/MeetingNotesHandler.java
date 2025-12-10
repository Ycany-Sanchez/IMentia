package util;

import people.Person;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MeetingNotesHandler extends FileHandler {
    private static final String NOTE_FOLDER = "Meeting_Notes";

    public MeetingNotesHandler() {
        File folder = Paths.get(DATA_FOLDER, NOTE_FOLDER).toFile();
        if (!folder.exists()) folder.mkdirs();
    }

    public void saveNote(Person p, String content) {
        String path = Paths.get(DATA_FOLDER, NOTE_FOLDER, p.getId() + ".txt").toString();

        LocalTime time = LocalTime.now();
        String timeStr = time.format(DateTimeFormatter.ofPattern("hh:mm a"));
        LocalDate date = LocalDate.now();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path, true))) {
            bw.write("----- NOTE START -----\n");
            bw.write(date + "\n");
            bw.write(timeStr + "\n\n");
            bw.write(content + "\n");
            bw.write("----- NOTE END -----\n");
            bw.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> loadNotes(Person p) {
        List<String> allNotes = new ArrayList<>();
        String path = Paths.get(DATA_FOLDER, NOTE_FOLDER, p.getId() + ".txt").toString();
        File file = new File(path);

        if (!file.exists()) return allNotes;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder currentNote = null;

            while ((line = br.readLine()) != null) {
                if (line.equals("----- NOTE START -----")) {
                    currentNote = new StringBuilder();
                } else if (line.equals("----- NOTE END -----")) {
                    if (currentNote != null) allNotes.add(currentNote.toString());
                    currentNote = null;
                } else if (currentNote != null) {
                    currentNote.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allNotes;
    }
}