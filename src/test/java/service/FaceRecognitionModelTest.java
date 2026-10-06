package service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import people.Person;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * Train -> persist -> load-from-disk round trip using synthetic face images.
 * Backs up the real saved_faces dir and model files while running.
 */
class FaceRecognitionModelTest {

    private Path facesDir;
    private Path facesBackup;
    private Path modelFile;
    private Path modelBackup;
    private Path manifestFile;
    private Path manifestBackup;

    @BeforeEach
    void backupData() throws Exception {
        facesDir = Paths.get("imentia_data", "saved_faces");
        facesBackup = Paths.get("imentia_data", "saved_faces.phase3bak");
        modelFile = Paths.get("imentia_data", "LBPH_model.yml");
        modelBackup = Paths.get("imentia_data", "LBPH_model.yml.phase3bak");
        manifestFile = Paths.get("imentia_data", "LBPH_model.manifest");
        manifestBackup = Paths.get("imentia_data", "LBPH_model.manifest.phase3bak");

        Files.createDirectories(facesDir.getParent());
        if (Files.exists(facesDir)) {
            Files.move(facesDir, facesBackup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(facesDir);
        moveAside(modelFile, modelBackup);
        moveAside(manifestFile, manifestBackup);
    }

    @AfterEach
    void restoreData() throws Exception {
        deleteRecursively(facesDir);
        if (Files.exists(facesBackup)) {
            Files.move(facesBackup, facesDir, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(modelFile);
        Files.deleteIfExists(manifestFile);
        moveBack(modelBackup, modelFile);
        moveBack(manifestBackup, manifestFile);
    }

    private static void moveAside(Path src, Path bak) throws Exception {
        if (Files.exists(src)) {
            Files.move(src, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void moveBack(Path bak, Path src) throws Exception {
        if (Files.exists(bak)) {
            Files.move(bak, src, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            for (Path p : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    /** Vertical split: left white, right black. */
    private static Mat verticalSplit() {
        Mat m = new Mat(new Size(100, 100), opencv_core.CV_8UC1, new Scalar(0, 0, 0, 0));
        opencv_imgproc.rectangle(m, new Point(0, 0), new Point(49, 99),
                new Scalar(255, 0, 0, 0), -1, opencv_imgproc.LINE_8, 0);
        return m;
    }

    /** Horizontal split: top white, bottom black. */
    private static Mat horizontalSplit() {
        Mat m = new Mat(new Size(100, 100), opencv_core.CV_8UC1, new Scalar(0, 0, 0, 0));
        opencv_imgproc.rectangle(m, new Point(0, 0), new Point(99, 49),
                new Scalar(255, 0, 0, 0), -1, opencv_imgproc.LINE_8, 0);
        return m;
    }

    private static Person personWithId(String id, String name) {
        Person p = new Person(name, "Friend");
        p.setId(id);
        return p;
    }

    @Test
    void trainPersistsModelAndReloadsWithoutRetraining() {
        assertThat(imwrite(facesDir.resolve("Person1.png").toString(), verticalSplit())).isTrue();
        assertThat(imwrite(facesDir.resolve("Person2.png").toString(), horizontalSplit())).isTrue();

        List<Person> persons = new ArrayList<>();
        persons.add(personWithId("Person1", "Vertical Vera"));
        persons.add(personWithId("Person2", "Horizontal Horacio"));

        FaceRecognitionService trained = new FaceRecognitionService();
        trained.train(persons);
        assertThat(trained.isTrained()).isTrue();
        assertThat(new File(modelFile.toString()).isFile()).isTrue();
        assertThat(new File(manifestFile.toString()).isFile()).isTrue();

        // Fresh service loads from disk instead of retraining.
        FaceRecognitionService loaded = new FaceRecognitionService();
        assertThat(loaded.tryLoadModel(persons)).isTrue();
        assertThat(loaded.isTrained()).isTrue();

        // And the loaded model actually recognizes.
        FaceRecognitionService.RecognitionResult hit = loaded.recognize(verticalSplit());
        assertThat(hit.isRecognized()).isTrue();
        assertThat(hit.getPerson().getName()).isEqualTo("Vertical Vera");

        // A changed dataset invalidates the persisted model.
        assertThat(imwrite(facesDir.resolve("Person3.png").toString(), verticalSplit())).isTrue();
        List<Person> grown = new ArrayList<>(persons);
        grown.add(personWithId("Person3", "Newbie Nora"));
        assertThat(new FaceRecognitionService().tryLoadModel(grown)).isFalse();
    }

    @Test
    void tryLoadModel_returnsFalseWithoutModelFiles() {
        List<Person> persons = new ArrayList<>();
        persons.add(personWithId("Person1", "Nobody"));
        assertThat(new FaceRecognitionService().tryLoadModel(persons)).isFalse();
        assertThat(new FaceRecognitionService().tryLoadModel(new ArrayList<>())).isFalse();
    }
}
