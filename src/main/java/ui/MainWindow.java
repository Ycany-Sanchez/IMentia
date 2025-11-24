package ui;

import people.Person;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import service.FaceRecognitionService;
import util.FileHandler;
import util.ImageUtils;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

public class MainWindow extends JFrame {
    private JLabel cameraLabel;
    private JButton captureButton;
    private JLabel infoLabel;

    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private FaceRecognitionService recognitionService;
    private FileHandler fileHandler;
    private List<Person> persons;

    private Mat currentFrame;
    private Rect currentFaceRect;
    private boolean running = true;

    public MainWindow(CascadeClassifier faceDetector) {
        System.out.println("=== MainWindow Constructor START ===");

        this.faceDetector = faceDetector;
        this.fileHandler = new FileHandler();
        this.recognitionService = new FaceRecognitionService();

        System.out.println("Loading persons from file...");
        this.persons = fileHandler.loadPersons();
        System.out.println("Loaded " + persons.size() + " persons");

        for (Person p : persons) {
            System.out.println("  - Person: " + p.getName() +
                    " (" + p.getRelationship() + ") " +
                    "with " + p.getFaces().size() + " face(s)");
        }

        System.out.println("Training face recognizer...");
        recognitionService.train(persons);
        System.out.println("Training complete. Trained: " + recognitionService.isTrained());

        setupUI();
        startCamera();

        System.out.println("=== MainWindow Constructor END ===");
    }

    private void setupUI() {
        System.out.println("Setting up UI...");

        setTitle("IMentia - Dementia Assistance");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout(10, 10));

        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(JLabel.CENTER);
        cameraLabel.setPreferredSize(new Dimension(640, 480));
        add(cameraLabel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        captureButton = new JButton("CAPTURE FACE");
        captureButton.setFont(new Font("Arial", Font.BOLD, 24));
        captureButton.setPreferredSize(new Dimension(250, 60));
        captureButton.addActionListener(e -> {
            System.out.println("\n*** CAPTURE BUTTON CLICKED ***");
            captureFace();
        });

        infoLabel = new JLabel("Loaded " + persons.size() + " saved person(s). Point camera at a face.");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        infoLabel.setHorizontalAlignment(JLabel.CENTER);

        bottomPanel.add(captureButton, BorderLayout.WEST);
        bottomPanel.add(infoLabel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        System.out.println("UI setup complete");
    }

    private void startCamera() {
        System.out.println("Starting camera...");
        camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("ERROR: Cannot open camera!");
            JOptionPane.showMessageDialog(this, "Cannot open camera!");
            return;
        }

        System.out.println("Camera opened successfully");

        Thread cameraThread = new Thread(() -> {
            System.out.println("Camera thread started");
            Mat frame = new Mat();
            Mat grayFrame = new Mat();

            while (running) {
                if (!camera.read(frame)) {
                    continue;
                }

                currentFrame = frame.clone();

                cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

                RectVector detections = new RectVector();
                faceDetector.detectMultiScale(grayFrame, detections);

                long numFaces = detections.size();
                if (numFaces > 0) {
                    Rect[] faces = new Rect[(int)numFaces];
                    for (int i = 0; i < numFaces; i++) {
                        faces[i] = detections.get(i);
                    }

                    currentFaceRect = getBiggestFace(faces);

                    rectangle(
                            frame,
                            new Point(currentFaceRect.x(), currentFaceRect.y()),
                            new Point(currentFaceRect.x() + currentFaceRect.width(),
                                    currentFaceRect.y() + currentFaceRect.height()),
                            new Scalar(0, 255, 0, 0),
                            3, LINE_8, 0
                    );
                } else {
                    currentFaceRect = null;
                }

                ImageIcon icon = new ImageIcon(ImageUtils.matToBufferedImage(frame));
                SwingUtilities.invokeLater(() -> cameraLabel.setIcon(icon));

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    break;
                }
            }

            camera.release();
            System.out.println("Camera thread stopped");
        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private Rect getBiggestFace(Rect[] faces) {
        Rect biggest = faces[0];
        int maxArea = biggest.width() * biggest.height();

        for (int i = 1; i < faces.length; i++) {
            int area = faces[i].width() * faces[i].height();
            if (area > maxArea) {
                maxArea = area;
                biggest = faces[i];
            }
        }

        return biggest;
    }

    private void captureFace() {
        System.out.println("=== captureFace() called ===");

        if (currentFrame == null || currentFaceRect == null) {
            System.out.println("No face detected in current frame");
            JOptionPane.showMessageDialog(this,
                    "No face detected! Please look at the camera.",
                    "No Face",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.println("Extracting face from frame...");
        System.out.println("Face rect: x=" + currentFaceRect.x() +
                ", y=" + currentFaceRect.y() +
                ", w=" + currentFaceRect.width() +
                ", h=" + currentFaceRect.height());

        Mat faceImage = new Mat(currentFrame, currentFaceRect);
        System.out.println("Face image extracted: " +
                faceImage.cols() + "x" + faceImage.rows() +
                ", channels=" + faceImage.channels());

        System.out.println("Calling recognitionService.recognize()...");
        FaceRecognitionService.RecognitionResult result = recognitionService.recognize(faceImage);
        System.out.println("Recognition completed");
        System.out.println("Result - isRecognized: " + result.isRecognized() +
                ", confidence: " + result.getConfidence());

        if (result.isRecognized()) {
            System.out.println("*** PERSON RECOGNIZED: " + result.getPerson().getName() + " ***");
            showRecognizedPerson(result.getPerson(), result.getConfidence());
        } else {
            System.out.println("*** PERSON NOT RECOGNIZED ***");
            int choice = JOptionPane.showConfirmDialog(this,
                    "Person not recognized. Would you like to add them?",
                    "Unknown Person",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                System.out.println("User chose to add new person");
                showAddPersonDialog(faceImage);
            } else {
                System.out.println("User chose not to add person");
            }
        }
    }

    private void showRecognizedPerson(Person person, double confidence) {
        System.out.println("Showing recognized person dialog for: " + person.getName());

        String confidenceLevel = getConfidenceDescription(confidence);

        String message = String.format(
                "<html><center>" +
                        "<h1 style='font-size: 32px; margin: 10px;'>%s</h1>" +
                        "<h2 style='font-size: 24px; color: #666; margin: 10px;'>%s</h2>" +
                        "<div style='margin-top: 20px; padding: 10px; background-color: %s; border-radius: 5px;'>" +
                        "<p style='font-size: 18px; margin: 5px;'><b>Match Quality:</b> %s</p>" +
                        "<p style='font-size: 14px; color: #666; margin: 5px;'>Confidence Score: %.1f</p>" +
                        "</div>" +
                        "</center></html>",
                person.getName(),
                person.getRelationship(),
                getConfidenceColor(confidence),
                confidenceLevel,
                confidence
        );

        infoLabel.setText("Recognized: " + person.getName() + " - " + confidenceLevel);

        JOptionPane.showMessageDialog(this,
                message,
                "Person Recognized!",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String getConfidenceDescription(double confidence) {
        if (confidence < 40) return "Excellent Match";
        else if (confidence < 60) return "Very Good Match";
        else if (confidence < 80) return "Good Match";
        else if (confidence < 100) return "Fair Match";
        else if (confidence < 120) return "Poor Match";
        else return "Very Poor Match";
    }

    private String getConfidenceColor(double confidence) {
        if (confidence < 40) return "#d4edda";
        else if (confidence < 60) return "#d1ecf1";
        else if (confidence < 80) return "#fff3cd";
        else if (confidence < 100) return "#f8d7da";
        else return "#f8d7da";
    }

    private void showAddPersonDialog(Mat faceImage) {
        System.out.println("Opening add person dialog...");
        PersonFormDialog dialog = new PersonFormDialog(this, faceImage);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Person newPerson = dialog.getPerson();
            System.out.println("New person confirmed: " + newPerson.getName());
            persons.add(newPerson);

            System.out.println("Saving persons to file...");
            fileHandler.savePersons(persons);

            System.out.println("Retraining recognizer...");
            recognitionService.train(persons);

            infoLabel.setText("Saved: " + newPerson.getName() + " (" + newPerson.getRelationship() + ")");
        } else {
            System.out.println("Add person dialog cancelled");
        }
    }

    @Override
    public void dispose() {
        System.out.println("Disposing window...");
        running = false;
        super.dispose();
    }
}