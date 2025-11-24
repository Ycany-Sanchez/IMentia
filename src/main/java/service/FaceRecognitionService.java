package service;

import java.io.PrintStream;
import java.nio.IntBuffer;
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

    public void train(List<Person> persons) {
        System.out.println("\n===== TRAINING START =====");
        System.out.println("Received " + persons.size() + " person(s) to train");
        if (persons.isEmpty()) {
            System.out.println("No persons to train on - marking as untrained");
            this.isTrained = false;
        } else {
            MatVector faceImages = new MatVector();
            List<Integer> labelList = new ArrayList();
            this.trainedPersons.clear();
            int label = 0;

            for(Person person : persons) {
                System.out.println("\nProcessing person: " + person.getName());
                System.out.println("  Relationship: " + person.getRelationship());
                System.out.println("  Face count: " + person.getFaces().size());
                if (person.getFaces().isEmpty()) {
                    System.out.println("  ⚠ Skipping - no faces");
                } else {
                    int faceIndex = 0;

                    for(FaceData faceData : person.getFaces()) {
                        ++faceIndex;
                        System.out.println("  Face " + faceIndex + ":");
                        PrintStream var10000 = System.out;
                        int var10001 = faceData.getImageWidth();
                        var10000.println("    Stored size: " + var10001 + "x" + faceData.getImageHeight());

                        try {
                            Mat faceMat = ImageUtils.faceDataToMat(faceData);
                            var10000 = System.out;
                            var10001 = faceMat.cols();
                            var10000.println("    Converted Mat: " + var10001 + "x" + faceMat.rows() + ", channels=" + faceMat.channels());
                            Mat grayFace = new Mat();
                            if (faceMat.channels() > 1) {
                                opencv_imgproc.cvtColor(faceMat, grayFace, 6);
                                System.out.println("    ✓ Converted to grayscale");
                            } else {
                                grayFace = faceMat.clone();
                                System.out.println("    Already grayscale");
                            }

                            Mat resizedFace = new Mat();
                            opencv_imgproc.resize(grayFace, resizedFace, new Size(100, 100));
                            System.out.println("    ✓ Resized to 100x100");
                            faceImages.push_back(resizedFace);
                            labelList.add(label);
                            System.out.println("    ✓ Added to training set with label " + label);
                        } catch (Exception e) {
                            System.out.println("    ✗ ERROR processing face:");
                            e.printStackTrace();
                        }
                    }

                    this.trainedPersons.add(person);
                    System.out.println("  ✓ Person assigned label: " + label);
                    ++label;
                }
            }

            if (faceImages.size() == 0L) {
                System.out.println("\n✗ No valid face images to train on");
                this.isTrained = false;
            } else {
                Mat labels = new Mat(labelList.size(), 1, opencv_core.CV_32SC1);
                IntBuffer labelBuffer = (IntBuffer)labels.createBuffer();

                for(int i = 0; i < labelList.size(); ++i) {
                    labelBuffer.put(i, (Integer)labelList.get(i));
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
                System.out.println("Label mapping:");

                for(int i = 0; i < this.trainedPersons.size(); ++i) {
                    System.out.println("  Label " + i + " → " + ((Person)this.trainedPersons.get(i)).getName());
                }

                System.out.println("==============================\n");
            }
        }
    }

    public RecognitionResult recognize(Mat faceImage) {
        System.out.println("\n===== RECOGNITION START =====");
        if (!this.isTrained) {
            System.out.println("✗ Cannot recognize - not trained!");
            return new RecognitionResult((Person)null, (double)-1.0F, "Not Trained");
        } else {
            PrintStream var10000 = System.out;
            int var10001 = faceImage.cols();
            var10000.println("Input face: " + var10001 + "x" + faceImage.rows() + ", channels=" + faceImage.channels());

            try {
                Mat grayFace = new Mat();
                if (faceImage.channels() > 1) {
                    opencv_imgproc.cvtColor(faceImage, grayFace, 6);
                    System.out.println("✓ Converted to grayscale");
                } else {
                    grayFace = faceImage.clone();
                    System.out.println("Already grayscale");
                }

                Mat resizedFace = new Mat();
                opencv_imgproc.resize(grayFace, resizedFace, new Size(100, 100));
                System.out.println("✓ Resized to 100x100");
                int[] predictedLabel = new int[1];
                double[] confidence = new double[1];
                System.out.println("Calling recognizer.predict()...");
                this.recognizer.predict(resizedFace, predictedLabel, confidence);
                System.out.println("✓ Prediction complete");
                System.out.println("\n--- PREDICTION RESULT ---");
                System.out.println("Predicted label: " + predictedLabel[0]);
                System.out.println("Confidence: " + confidence[0]);
                System.out.println("Threshold: 100.0");
                System.out.println("-------------------------");
                String confidenceLevel = this.getConfidenceLevel(confidence[0]);
                if (confidence[0] > (double)100.0F) {
                    System.out.println("✗ Confidence " + confidence[0] + " > 100.0");
                    System.out.println("Match not good enough → UNKNOWN");
                    System.out.println("===== RECOGNITION END (UNKNOWN) =====\n");
                    return new RecognitionResult((Person)null, confidence[0], confidenceLevel);
                } else if (predictedLabel[0] >= 0 && predictedLabel[0] < this.trainedPersons.size()) {
                    Person matched = (Person)this.trainedPersons.get(predictedLabel[0]);
                    System.out.println("✓ Confidence acceptable!");
                    System.out.println("*** MATCH: " + matched.getName() + " ***");
                    System.out.println("===== RECOGNITION END (MATCHED) =====\n");
                    return new RecognitionResult(matched, confidence[0], confidenceLevel);
                } else {
                    System.out.println("✗ Label " + predictedLabel[0] + " out of range");
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
