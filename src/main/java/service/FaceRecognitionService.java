package service;

import java.io.File;
import java.io.PrintStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import people.Person;
import util.FileHandler;
import util.ImageHandler;
import util.ImageUtils;

public class FaceRecognitionService {
    private LBPHFaceRecognizer recognizer;
    private List<Person> trainedPersons;
    private boolean isTrained = false;
    private static final double CONFIDENCE_THRESHOLD = (double)100.0F;
    private static final String MODEL_FILE_NAME = "LBPH_model.yml";
    private static final String MANIFEST_FILE_NAME = "LBPH_model.manifest";

    public FaceRecognitionService() {
        System.out.println("FaceRecognitionService created");
        this.recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, (double)100.0F);
        this.trainedPersons = new ArrayList();
    }

    /**
     * PLEASE DO NOT REMOVE MY COMMENTSSSSSS in any of the methods here.
     * Naay nag remove. Mag add lng nya ko balik.
     * And do not remove the console outputs.
     */
    public void train(List<Person> persons) {
        System.out.println("\n===== TRAINING START =====");
        System.out.println("Received " + persons.size() + " person(s) to train");

        if (persons.isEmpty()) {
            System.out.println("No persons to train on - marking as untrained");
            this.isTrained = false;
            deletePersistedModel();
            return;
        }

        MatVector faceImages = new MatVector();
        List<Integer> labelList = new ArrayList(); // list of labels
        this.trainedPersons.clear();
        int label = 0; // label is welp, a label for the person, like ID.

        for(Person person : persons) {
            System.out.println("\nProcessing person: " + person.getName());

            boolean personAdded = false;
            for (String filePath : listFaceFiles(directoryPath(), person.getId())) {
                try {
                    Mat faceMat = ImageHandler.loadMatFromFile(filePath);

                    if (faceMat == null || faceMat.empty()) {
                        System.out.println("ERROR: Could not load image from path, skipping file: " + filePath);
                        continue;
                    }

                    Mat resizedFace = ImageUtils.preprocessFace(faceMat);
                    System.out.println("Preprocessed to 100x100 + equalized: " + filePath);

                    faceImages.push_back(resizedFace);
                    labelList.add(label);

                    System.out.println("Added to training set with label " + label);
                    personAdded = true;

                } catch (RuntimeException e) {
                    // OpenCV/JavaCV often throws RuntimeExceptions for native errors
                    System.out.println("OPENCV ERROR processing face:");
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("GENERAL ERROR processing face:");
                    e.printStackTrace();
                }
            }

            if (personAdded) {
                // Only persons with a usable image get a label, so the
                // trainedPersons index always matches the recognizer label.
                this.trainedPersons.add(person);
                System.out.println("Person assigned label: " + label);
                ++label;
            } else {
                System.out.println("WARNING: No usable images for " + person.getName() + ", skipped (no label assigned)");
            }
        }

        /*
          Note: faceImages list assumes our initial implementation plan to have multiple images of the same person for better accuracy.
          Since we currently abolished that, we are left with this.
          This still works for one image of the person, so let us keep this implementation.
          Should we decide to change this, we have faceImages ready.
          Please avoid modifying :>
          */

        if (faceImages.size() == 0) {
            System.out.println("\nNo valid face images to train on");
            this.isTrained = false;
        } else {
            /*
            recognizer.train() needs MatVector the faceImages, and a Mat, the labelList, that we made into a Mat below.
             Also, we needed to create a Buffer for labels in order to actually put the labelList items in it.
             C++ cannot work with java arraylist objects so yeah. We create Mat labels to put inside the recognizer.train()
             Also, CV_32SC1 (train() method expected this) is a 32-bit signed with 1 value(just the person label id thingy).
             32 bit is the size of java int
             */

            Mat labels = new Mat(labelList.size(), 1, opencv_core.CV_32SC1);
            IntBuffer labelBuffer = labels.createBuffer();

            for(int i = 0; i < labelList.size(); ++i) {
                labelBuffer.put(i, labelList.get(i));
            }

            System.out.println("\nTraining recognizer...");
            System.out.println("  Total images: " + faceImages.size());
            System.out.println("  Total persons: " + this.trainedPersons.size());

            try {
                // This is where we actually train. Note nga lahi ang recognizer.train and this train method.
                this.recognizer.train(faceImages, labels);
                this.isTrained = true;
                System.out.println("Training successful!");
                persistModel(persons);
            } catch (RuntimeException e) {
                System.out.println("Training failed (Native Error):");
                e.printStackTrace();
                this.isTrained = false;
            } catch (Exception e) {
                System.out.println("Training failed (General Error):");
                e.printStackTrace();
                this.isTrained = false;
            }

            System.out.println("\n===== TRAINING COMPLETE =====");
            System.out.println("Label mapping:");
            for(int i = 0; i < this.trainedPersons.size(); ++i) {
                System.out.println("  Label " + i + " → " + this.trainedPersons.get(i).getName());
            }
            System.out.println("==============================\n");
        }
    }

    public RecognitionResult recognize(Mat faceImage) {
        System.out.println("\n===== RECOGNITION START =====");
        if (!this.isTrained) {
            System.out.println("Cannot recognize - not trained!");
            return new RecognitionResult((Person)null, (double)-1.0F, "Not Trained");
        } else {
            System.out.println("Input face: " + faceImage.cols() + "x" + faceImage.rows() + ", channels=" + faceImage.channels());

            try {
                Mat resizedFace = ImageUtils.preprocessFace(faceImage);
                System.out.println("Preprocessed to 100x100 + equalized");

                int[] predictedLabel = new int[1];
                double[] confidence = new double[1];

                System.out.println("Calling recognizer.predict()...");
                // This is the method of face recognizer to recog and predict whoever.
                this.recognizer.predict(resizedFace, predictedLabel, confidence);
                System.out.println("Prediction complete");

                System.out.println("\n--- PREDICTION RESULT ---");
                System.out.println("Predicted label: " + predictedLabel[0]);
                System.out.println("Confidence: " + confidence[0]);
                System.out.println("Threshold: 100.0");
                System.out.println("-------------------------");

                String confidenceLevel = this.getConfidenceLevel(confidence[0]);

                if (confidence[0] > (double)CONFIDENCE_THRESHOLD) {
                    System.out.println("Confidence " + confidence[0] + " > 100.0");
                    System.out.println("Match not good enough → UNKNOWN");
                    System.out.println("===== RECOGNITION END (UNKNOWN) =====\n");
                    return new RecognitionResult((Person)null, confidence[0], confidenceLevel);
                } else if (predictedLabel[0] >= 0 && predictedLabel[0] < this.trainedPersons.size()) {
                    Person matched = (Person)this.trainedPersons.get(predictedLabel[0]);
                    System.out.println("Confidence acceptable!");
                    System.out.println("*** MATCH: " + matched.getName() + " ***");
                    System.out.println("===== RECOGNITION END (MATCHED) =====\n");
                    return new RecognitionResult(matched, confidence[0], confidenceLevel);
                } else {
                    System.out.println("Label " + predictedLabel[0] + " out of range");
                    System.out.println("===== RECOGNITION END (UNKNOWN) =====\n");
                    return new RecognitionResult((Person)null, confidence[0], confidenceLevel);
                }
            } catch (Exception e) {
                System.out.println("✗ ERROR during recognition:");
                e.printStackTrace();
                System.out.println("===== RECOGNITION END (ERROR) =====\n");
                return new RecognitionResult((Person)null, (double)-1.0F, "Error");
            }
        }
    }

    private String getConfidenceLevel(double confidence) {
        if (confidence < (double)0.0F) {
            return "Error";
        } else if (confidence < (double)40.0F) {
            return "Excellent Match";
        } else if (confidence < (double)60.0F) {
            return "Very Good Match";
        } else if (confidence < (double)80.0F) {
            return "Good Match";
        } else if (confidence < (double)100.0F) {
            return "Fair Match";
        } else {
            return confidence < (double)120.0F ? "Poor Match" : "Very Poor Match";
        }
    }

    public boolean isTrained() {
        return this.isTrained;
    }

    /**
     * Tries to load a previously persisted model instead of retraining.
     * Succeeds only when the model file, its manifest, and the current
     * persons+image files all match; otherwise returns false (caller trains).
     * Restores trainedPersons in the exact training order so labels align.
     */
    public boolean tryLoadModel(List<Person> persons) {
        if (persons == null || persons.isEmpty()) {
            return false;
        }
        try {
            String dataFolder = new ImageHandler().getDataFolder();
            File modelFile = new File(dataFolder, MODEL_FILE_NAME);
            File manifestFile = new File(dataFolder, MANIFEST_FILE_NAME);
            if (!modelFile.isFile() || !manifestFile.isFile()) {
                return false;
            }
            String expected = buildManifest(persons);
            String saved = Files.readString(manifestFile.toPath());
            if (!expected.equals(saved)) {
                System.out.println("Model manifest changed, retraining instead of loading.");
                return false;
            }
            LBPHFaceRecognizer loaded = LBPHFaceRecognizer.create(1, 8, 8, 8, (double)100.0F);
            loaded.read(modelFile.getAbsolutePath());

            List<Person> labeled = new ArrayList<>();
            String dir = directoryPath();
            for (Person p : persons) {
                if (p != null && !listFaceFiles(dir, p.getId()).isEmpty()) {
                    labeled.add(p);
                }
            }
            this.recognizer = loaded;
            this.trainedPersons = labeled;
            this.isTrained = true;
            System.out.println("Loaded persisted model for " + labeled.size() + " person(s), no retraining needed.");
            return true;
        } catch (Exception e) {
            System.out.println("Could not load persisted model (" + e.getMessage() + "), retraining.");
            return false;
        }
    }

    /** Faces folder shared by training, manifest and persistence. */
    private static String directoryPath() {
        return Paths.get(new ImageHandler().getDataFolder(), "saved_faces").toString();
    }

    /**
     * All training images for one person, in stable order: legacy
     * {@code <id>.png} first, then multi-sample {@code <id>_*.png} files.
     */
    static List<String> listFaceFiles(String facesDir, String personId) {
        List<String> files = new ArrayList<>();
        if (personId == null) {
            return files;
        }
        File legacy = new File(facesDir, personId + ".png");
        if (legacy.isFile()) {
            files.add(legacy.getAbsolutePath());
        }
        File dir = new File(facesDir);
        File[] multi = dir.listFiles((d, name) -> name.startsWith(personId + "_") && name.endsWith(".png"));
        if (multi != null) {
            Arrays.sort(multi);
            for (File f : multi) {
                files.add(f.getAbsolutePath());
            }
        }
        return files;
    }

    /**
     * Fingerprint of the training set in training order: one line per image
     * file (person, name, size, mtime). Any added/removed/changed image, or
     * any reordering, changes the manifest and forces a retrain on load.
     */
    private static String buildManifest(List<Person> persons) {
        StringBuilder sb = new StringBuilder();
        String dir = directoryPath();
        for (Person p : persons) {
            if (p == null) {
                continue;
            }
            for (String path : listFaceFiles(dir, p.getId())) {
                File f = new File(path);
                sb.append(p.getId()).append('|').append(f.getName())
                        .append('|').append(f.length()).append('|').append(f.lastModified())
                        .append('\n');
            }
        }
        return sb.toString();
    }

    /** Writes the trained model plus its manifest next to the app data. */
    private void persistModel(List<Person> persons) {
        try {
            String dataFolder = new ImageHandler().getDataFolder();
            String modelPath = Paths.get(dataFolder, MODEL_FILE_NAME).toString();
            this.recognizer.write(modelPath);
            Files.writeString(Paths.get(dataFolder, MANIFEST_FILE_NAME), buildManifest(persons));
            System.out.println("Model saved to " + modelPath);
        } catch (Exception e) {
            System.out.println("WARNING: Could not persist model: " + e.getMessage());
        }
    }

    /** Removes stale model files (e.g. when the last contact is deleted). */
    private static void deletePersistedModel() {
        try {
            String dataFolder = new ImageHandler().getDataFolder();
            new File(dataFolder, MODEL_FILE_NAME).delete();
            new File(dataFolder, MANIFEST_FILE_NAME).delete();
        } catch (Exception e) {
            System.out.println("WARNING: Could not delete stale model: " + e.getMessage());
        }
    }

    public static class RecognitionResult {
        private Person person;
        private double confidence;
        private String confidenceLevel;

        public RecognitionResult(Person person, double confidence, String confidenceLevel) {
            this.person = person;
            this.confidence = confidence;
            this.confidenceLevel = confidenceLevel;
        }

        public Person getPerson() {
            return this.person;
        }

        public double getConfidence() {
            return this.confidence;
        }

        public String getConfidenceLevel() {
            return this.confidenceLevel;
        }

        public boolean isRecognized() {
            return this.person != null;
        }
    }
}