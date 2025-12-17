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
import people.Person;
import util.FileHandler;
import util.ImageHandler;
import util.ImageUtils;

public class FaceRecognitionService {
    private LBPHFaceRecognizer recognizer;
    private List<Person> trainedPersons;
    private boolean isTrained = false;
    private static final double CONFIDENCE_THRESHOLD = (double)100.0F;

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
            return;
        }

        MatVector faceImages = new MatVector();
        List<Integer> labelList = new ArrayList(); // list of labels
        this.trainedPersons.clear();
        int label = 0; // label is welp, a label for the person, like ID.

        for(Person person : persons) {
            System.out.println("\nProcessing person: " + person.getName());

            try {
                FileHandler fileHandler = new ImageHandler();
                String directoryPath = Paths.get(fileHandler.getDataFolder(), "saved_faces").toString();
                String filePath = Paths.get(directoryPath, person.getId() + ".png").toString();
                Mat faceMat = ImageHandler.loadMatFromFile(filePath);

                if (faceMat.empty()) {
                    System.out.println("ERROR: Could not load image from path");
                    continue;
                }

                Mat grayFace = new Mat();
                if (faceMat.channels() > 1) {
                    opencv_imgproc.cvtColor(faceMat, grayFace, 6);
                    System.out.println("Converted to grayscale");
                } else {
                    grayFace = faceMat.clone();
                    System.out.println("Already grayscale");
                }

                Mat resizedFace = new Mat();
                opencv_imgproc.resize(grayFace, resizedFace, new Size(100, 100));
                System.out.println("Resized to 100x100");

                faceImages.push_back(resizedFace);
                labelList.add(label);

                System.out.println("Added to training set with label " + label);

            } catch (RuntimeException e) {
                // OpenCV/JavaCV often throws RuntimeExceptions for native errors
                System.out.println("OPENCV ERROR processing face:");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("GENERAL ERROR processing face:");
                e.printStackTrace();
            }

            this.trainedPersons.add(person);
            System.out.println("Person assigned label: " + label);
            ++label;
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
                Mat grayFace = new Mat();
                if (faceImage.channels() > 1) {
                    opencv_imgproc.cvtColor(faceImage, grayFace, 6);
                    System.out.println("Converted to grayscale");
                } else {
                    grayFace = faceImage.clone();
                    System.out.println("Already grayscale");
                }

                Mat resizedFace = new Mat();
                opencv_imgproc.resize(grayFace, resizedFace, new Size(100, 100));
                System.out.println("Resized to 100x100");

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