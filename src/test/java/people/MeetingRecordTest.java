package people;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 0 baseline for the Meeting_Notes txt format.
 */
class MeetingRecordTest {

    private String personId;

    @AfterEach
    void cleanupNotesFile() {
        if (personId != null) {
            String filePath = Paths.get("imentia_data", "Meeting_Notes",
                    personId + ".txt").toString();
            new File(filePath).delete();
        }
    }

    @Test
    void createThenRead_returnsWrittenNotes() throws Exception {
        personId = "PersonTest-" + UUID.randomUUID().toString().substring(0, 8);
        Person p = new Person("Test Person", "Friend");
        p.setId(personId);

        MeetingRecord first = new MeetingRecord(p, "First visit went well.");
        first.createFile();

        MeetingRecord second = new MeetingRecord(p, "Second visit\nwith two lines.");
        second.createFile();

        MeetingRecord reader = new MeetingRecord(p, "");
        List<String> notes = reader.readAllNotes();

        assertThat(notes).hasSize(2);
        assertThat(notes.get(0)).contains("First visit went well.");
        assertThat(notes.get(1)).contains("Second visit");
    }

    @Test
    void readAllNotes_missingFileReturnsEmpty() throws Exception {
        personId = "PersonTest-" + UUID.randomUUID().toString().substring(0, 8);
        Person p = new Person("Nobody", "Friend");
        p.setId(personId);

        MeetingRecord reader = new MeetingRecord(p, "");
        assertThat(reader.readAllNotes()).isEmpty();
    }
}
