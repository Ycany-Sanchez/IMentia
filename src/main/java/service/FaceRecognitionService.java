// >>> FILE: src/main/java/service/FaceRecognitionService.java
package service;

import java.io.PrintStream;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import people.FaceData;
import people.Person;
import util.FileHandler;
import util.ImageHandler; // <--- NEW IMPORT
import util.ImageUtils;

/**
 * Manages the training and recognition process using the Local Binary Patterns Histograms (LBPH)
 * algorithm from OpenCV, wrapped by Bytedeco.
 */
public class FaceRecognitionService {
    private LBPHFaceRecognizer recognizer;
    private List<Person> trainedPersons;
    private boolean isTrained = false;
    // The confidence threshold determines the maximum "distance" for a match to be considered a known person.
    private static final double CONFIDENCE_THRESHOLD = 100.0;

    public FaceRecognitionService() {
        System.out.println("FaceRecognitionService created");
        // Create the LBPH recognizer instance.
        // Parameters: radius=1, neighbors=8, grid_x=8, grid_y=8, threshold=100.0
        this.recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, 100.0);
        this.trainedPersons = new ArrayList<>();
    }

    /**
     * Trains the LBPH recognizer using face images loaded directly from files
     * for each Person object in the provided list.
     */
    public void train(List<Person> persons) {
        System.out.println("\n===== TRAINING START =====");
        System.out.println("Received " + persons.size() + " person(s) to train");

        if (persons.isEmpty()) {
            System.out.println("No persons to train on - marking as untrained");
            this.isTrained = false;
            return;
        }

        MatVector faceImages = new MatVector();
        List<Integer> labelList = new ArrayList<>();
        this.trainedPersons.clear();
        int label = 0;

        // Use the Concrete ImageHandler instead of the abstract FileHandler
        ImageHandler imageHandler = new ImageHandler();

        for(Person person : persons) {
            System.out.println("\nProcessing person: " + person.getName());

            try {
                // 1. LOAD IMAGE
                // We use imageHandler.getDataFolder() inherited from FileHandler
                String directoryPath = Paths.get(imageHandler.getDataFolder(), "saved_faces").toString();
                String filePath = Paths.get(directoryPath, person.getId() + ".png").toString();

                Mat faceMat = ImageUtils.loadMatFromFile(filePath);

                if (faceMat == null || faceMat.empty()) {
                    System.out.println("  ✗ ERROR: Could not load image from path: " + filePath);
                    continue;
                }

                // 2. CONVERT TO GRAYSCALE
                Mat grayFace = new Mat();
                if (faceMat.channels() > 1) {
                    opencv_imgproc.cvtColor(faceMat, grayFace, opencv_imgproc.COLOR_BGR2GRAY);
                    System.out.println("    ✓ Converted to grayscale");
                } else {
                    grayFace = faceMat.clone();
                    System.out.println("    Already grayscale");
                }

                // 3. RESIZE
                Mat resizedFace = new Mat();
                opencv_imgproc.resize(grayFace, resizedFace, new Size(100, 100));
                System.out.println("    ✓ Resized to 100x100");

                // 4. ADD TO DATASET
                faceImages.push_back(resizedFace);
                labelList.add(label);

                System.out.println("    ✓ Added to training set with label " + label);

            } catch (Exception e) {
                System.out.println("    ✗ CRITICAL ERROR processing face:");
                e.printStackTrace();
            }

            this.trainedPersons.add(person);
            System.out.println("  ✓ Person assigned label: " + label);
            ++label;
        }

        if (faceImages.size() == 0) {
            System.out.println("\n✗ No valid face images to train on");
            this.isTrained = false;
        } else {
            Mat labels = new Mat(labelList.size(), 1, opencv_core.CV_32SC1);
            IntBuffer labelBuffer = labels.createBuffer();

            for(int i = 0; i < labelList.size(); ++i) {
                labelBuffer.put(i, labelList.get(i));
            }

            System.out.println("\nTraining recognizer...");
            System.out.println("  Total images: " + faceImages.size());
            System.out.println("  Total persons: " + this.trainedPersons.size());

            try {
                this.recognizer.train(faceImages, labels);
                this.isTrained = true;
                System.out.println("  ✓ Training successful!");
            } catch (Exception e) {
                System.out.println("  ✗ Training failed:");
                e.printStackTrace();
                this.isTrained = false;
            }

            System.out.println("\n===== TRAINING COMPLETE =====");
        }
    }

    /**
     * Attempts to recognize a face image against the trained model.
     */
    public RecognitionResult recognize(Mat faceImage) {
        System.out.println("\n===== RECOGNITION START =====");
        if (!this.isTrained) {
            System.out.println("✗ Cannot recognize - not trained!");
            return new RecognitionResult(null, -1.0, "Not Trained");
        } else {
            System.out.println("Input face: " + faceImage.cols() + "x" + faceImage.rows());

            try {
                // --- RECOGNITION PRE-PROCESSING ---
                Mat grayFace = new Mat();
                if (faceImage.channels() > 1) {
                    opencv_imgproc.cvtColor(faceImage, grayFace, opencv_imgproc.COLOR_BGR2GRAY);
                } else {
                    grayFace = faceImage.clone();
                }

                Mat resizedFace = new Mat();
                opencv_imgproc.resize(grayFace, resizedFace, new Size(100, 100));

                // --- PREDICTION ---
                int[] predictedLabel = new int[1];
                double[] confidence = new double[1];

                this.recognizer.predict(resizedFace, predictedLabel, confidence);

                System.out.println("Predicted label: " + predictedLabel[0]);
                System.out.println("Confidence: " + confidence[0]);

                String confidenceLevel = this.getConfidenceLevel(confidence[0]);

                if (confidence[0] > CONFIDENCE_THRESHOLD) {
                    System.out.println("Match not good enough → UNKNOWN");
                    return new RecognitionResult(null, confidence[0], confidenceLevel);
                } else if (predictedLabel[0] >= 0 && predictedLabel[0] < this.trainedPersons.size()) {
                    Person matched = this.trainedPersons.get(predictedLabel[0]);
                    System.out.println("*** MATCH: " + matched.getName() + " ***");
                    return new RecognitionResult(matched, confidence[0], confidenceLevel);
                } else {
                    return new RecognitionResult(null, confidence[0], confidenceLevel);
                }
            } catch (Exception e) {
                System.out.println("✗ ERROR during recognition:");
                e.printStackTrace();
                return new RecognitionResult(null, -1.0, "Error");
            }
        }
    }

    private String getConfidenceLevel(double confidence) {
        if (confidence < 0.0) return "Error";
        else if (confidence < 40.0) return "Excellent Match";
        else if (confidence < 60.0) return "Very Good Match";
        else if (confidence < 80.0) return "Good Match";
        else if (confidence < 100.0) return "Fair Match";
        else return confidence < 120.0 ? "Poor Match" : "Very Poor Match";
    }

    public boolean isTrained() {
        return this.isTrained;
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

        public Person getPerson() { return this.person; }
        public double getConfidence() { return this.confidence; }
        public String getConfidenceLevel() { return this.confidenceLevel; }
        public boolean isRecognized() { return this.person != null; }
    }
}