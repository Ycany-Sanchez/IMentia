package util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import people.Person;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 0 baseline: locks current CSV round-trip behaviour.
 * Uses a backup/restore of the real Person_File.csv because
 * DATA_FOLDER is currently a hardcoded relative path (see Phase 2).
 */
class PersonDataManagerTest {

    private Path csvPath;
    private Path backupPath;
    private boolean backupExisted;

    @BeforeEach
    void backupCsv() throws Exception {
        csvPath = Paths.get("imentia_data", "Person_File.csv");
        backupPath = Paths.get("imentia_data", "Person_File.csv.phase0bak");
        if (Files.exists(csvPath)) {
            Files.createDirectories(csvPath.getParent());
            Files.copy(csvPath, backupPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            backupExisted = true;
            Files.delete(csvPath);
        } else {
            backupExisted = false;
            if (csvPath.getParent() != null) {
                Files.createDirectories(csvPath.getParent());
            }
        }
    }

    @AfterEach
    void restoreCsv() throws Exception {
        Files.deleteIfExists(csvPath);
        if (backupExisted) {
            Files.copy(backupPath, csvPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(backupPath);
        } else {
            // Remove dir only if we created it and it is empty.
            File dir = new File("imentia_data");
            if (dir.exists() && dir.isDirectory()
                    && dir.list() != null && dir.list().length == 0) {
                dir.delete();
            }
        }
    }

    @Test
    void saveThenLoad_roundTripsNameAndRelationship() {
        PersonDataManager manager = new PersonDataManager();

        List<Person> toSave = new ArrayList<>();
        Person ada = new Person("Ada Lovelace", "Mother");
        ada.setId("Person1");
        Person grace = new Person("Grace Hopper", "Caregiver");
        grace.setId("Person2");
        toSave.add(ada);
        toSave.add(grace);

        // Fresh manager has empty in-memory dedup map after backup removal.
        PersonDataManager fresh = new PersonDataManager();
        assertThat(fresh.savePersons(toSave)).isTrue();

        List<Person> loaded = fresh.loadPersons();
        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).getId()).isEqualTo("Person1");
        assertThat(loaded.get(0).getName()).isEqualTo("Ada Lovelace");
        assertThat(loaded.get(0).getRelationship()).isEqualTo("Mother");
        assertThat(loaded.get(1).getId()).isEqualTo("Person2");
    }

    @Test
    @Disabled("Documents Phase 1 bug: names containing commas break split(\",\"). "
            + "Enable after CSV quoting is added.")
    void saveThenLoad_nameWithComma() {
        PersonDataManager manager = new PersonDataManager();
        List<Person> toSave = new ArrayList<>();
        Person p = new Person("Dela Cruz, Jr.", "Son");
        p.setId("Person1");
        toSave.add(p);

        manager.savePersons(toSave);
        List<Person> loaded = manager.loadPersons();
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getName()).isEqualTo("Dela Cruz, Jr.");
    }
}
