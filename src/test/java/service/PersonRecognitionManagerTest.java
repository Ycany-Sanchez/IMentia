package service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import people.Person;
import util.exceptions.PersonAlreadyExistsException;
import util.exceptions.PersonSaveException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-sample enrollment through the Facade. Backs up imentia_data
 * (csv, faces, model) while running.
 */
class PersonRecognitionManagerTest {

    private Path dataDir;
    private Path dataBackup;

    @BeforeEach
    void backupData() throws Exception {
        dataDir = Paths.get("imentia_data");
        dataBackup = Paths.get("imentia_data.phaseMbak");
        Files.createDirectories(dataDir);
        if (Files.exists(dataBackup)) {
            deleteRecursively(dataBackup);
        }
        Files.move(dataDir, dataBackup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.createDirectories(dataDir);
    }

    @AfterEach
    void restoreData() throws Exception {
        deleteRecursively(dataDir);
        Files.move(dataBackup, dataDir, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            for (Path p : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static Mat sample(int brightness) {
        return new Mat(new Size(120, 120), opencv_core.CV_8UC3,
                new Scalar(brightness, brightness, brightness, 0));
    }

    @Test
    void registerNewPerson_savesMultipleSamplesAndRecognizes() throws Exception {
        PersonRecognitionManager manager = new PersonRecognitionManager();

        List<Mat> burst = new ArrayList<>();
        burst.add(sample(90));
        burst.add(sample(100));
        burst.add(sample(110));

        Person saved = manager.registerNewPerson("Burst Bella", "Sister", burst);

        assertThat(saved.getId()).isEqualTo("Person1");
        assertThat(Files.isRegularFile(dataDir.resolve("saved_faces/Person1.png"))).isTrue();
        assertThat(Files.isRegularFile(dataDir.resolve("saved_faces/Person1_1.png"))).isTrue();
        assertThat(Files.isRegularFile(dataDir.resolve("saved_faces/Person1_2.png"))).isTrue();
        assertThat(manager.getAllPersons()).hasSize(1);

        FaceRecognitionService.RecognitionResult hit =
                manager.recognizeFace(sample(100));
        assertThat(hit.isRecognized()).isTrue();
        assertThat(hit.getPerson().getName()).isEqualTo("Burst Bella");

        assertThatThrownBy(() -> manager.registerNewPerson("burst bella", "Cousin", burst))
                .isInstanceOf(PersonAlreadyExistsException.class);
        assertThatThrownBy(() -> manager.registerNewPerson("  ", "Cousin", burst))
                .isInstanceOf(PersonSaveException.class);

        manager.deletePerson(saved);
        assertThat(manager.getAllPersons()).isEmpty();
        try (var faces = Files.list(dataDir.resolve("saved_faces"))) {
            assertThat(faces.findAny()).isEmpty();
        }
    }
}
