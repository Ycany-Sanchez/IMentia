package ui;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Point;
import people.Person;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import service.FaceRecognitionService;
import util.FileHandler;
import util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;

public class MainPanel {
    //UI Components
    private JPanel mainPanel;
    private JButton CapturePhotoButton;
    private JButton ViewContactsButton;
    private JButton TutorialButton;
    private JPanel CameraPanel;
    private JPanel ContactsPanel;
    private JPanel TutorialPanel;
    private JPanel PersonFormPanel;
    private JPanel RecognizedForm;
    private JButton EditContactButton;
    private JButton DeleteButton;
    private JPanel DisplayPanel;
    private JPanel ButtonPanel;
    private JPanel PersonPanel;
    private JButton BackToCameraButton;
    private JLabel CameraLabel;
    private JLabel PersonImageLabel;
    private JTextField PersonNameField;
    private JButton SavePersonInfoButton;
    private JLabel PersonNameLabel;
    private JPanel NamePanel;
    private JPanel RelationshipPanel;
    private JLabel PersonRelationshipLabel;
    private JTextField PersonRelationshipField;

    private JLabel personInfoLabel;

    private CardLayout cardLayout = new CardLayout();
    private boolean isEditing = false;
    private JFrame tempFrame = new JFrame();

    private Font buttonFont = new Font("", Font.BOLD, 24);
    private Font HLabelFont = new Font("", Font.BOLD, 20);
    private Font PLabelFont = new Font("", Font.PLAIN, 20);

    //Face Components
    private VideoCapture camera;
    private Mat faceImage;
    private CascadeClassifier faceDetector;
    private FaceRecognitionService recognitionService;
    private FileHandler fileHandler;
    private List<Person> persons;   //careful with conflicts on java.util and java.awt for List keyword
    private String PersonName;
    private String PersonRelationship;

    // Camera
    private Mat currentFrame; // Holds the last captured raw frame
    private Rect currentFaceRect; // Holds the bounding box of the biggest detected face
    private boolean running = true; // Control flag for the camera thread


    public MainPanel(){
        //this.fileHandler = new FileHandler();
       // persons = fileHandler.loadPersonFile("Person_File.csv");
        this.faceDetector = loadFaceDetector();

        // took me 3 hours to size the CameraLabel and scale the image properly. Please avoid modifying if possible.
        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePanelSizes();
            }
        });

        persons = new ArrayList<>();

        setUpUI();
        startCamera();
    }


    private void setUpUI(){     // this method sets up swing components
        DisplayPanel.setLayout(cardLayout);

        DisplayPanel.add(CameraPanel, "1");
        DisplayPanel.add(ContactsPanel, "2");
        DisplayPanel.add(PersonFormPanel, "3");


        tempFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        System.out.println(tempFrame.getWidth() + " " + tempFrame.getHeight());

        setButtonFont(mainPanel);
        setPLabelFont(mainPanel);

        cardLayout.show(DisplayPanel, "1");
        EditContactButton.setFont(new Font("", Font.BOLD, 24));

        ViewContactsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "2");
                BackToCameraButton.setVisible(true);
                CapturePhotoButton.setVisible(false);
                TutorialButton.setVisible(false);
                ViewContactsButton.setVisible(false);
            }
        });

        // NEW CODE: Tutorial Button Logic
        TutorialButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "4"); // Switch to Tutorial Card

                // Hide main menu buttons
                CapturePhotoButton.setVisible(false);
                ViewContactsButton.setVisible(false);
                TutorialButton.setVisible(false);

                // Show the Back button
                BackToCameraButton.setVisible(true);
                BackToCameraButton.setText("BACK TO CAMERA"); // Optional: Make text clear
            }
        });


        EditContactButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isEditing = !isEditing;

                if (isEditing) {
                    EditContactButton.setText("DONE EDIT");
                } else {
                    EditContactButton.setText("EDIT LIST");
                    EditContactButton.setFont(new Font("", Font.BOLD, 24));
                }

                toggleDeleteButton();
            }
        });
        BackToCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(isEditing){
                    //int editChoice = JOptionPane.showConfirmDialog(null, "You are currently editing your contacts. Are you done editing?")\
                    isEditing = false;
                    EditContactButton.setText("EDIT LIST");
                    toggleDeleteButton();

                }
                cardLayout.show(DisplayPanel, "1"); // Go back to Camera

                // Restore buttons
                BackToCameraButton.setVisible(false);
                CapturePhotoButton.setVisible(true);
                TutorialButton.setVisible(true);
                ViewContactsButton.setVisible(true);
            }
        });

        CapturePhotoButton.addActionListener(e ->{
            captureFace();
            cardLayout.show(DisplayPanel, "3");
            BufferedImage bufferedImage = ImageUtils.matToBufferedImage(faceImage);
            Image scaledImage = bufferedImage.getScaledInstance(200, 200, Image.SCALE_FAST);
            ImageIcon imageIcon = new ImageIcon(scaledImage);


            PersonImageLabel.setIcon(imageIcon);
            BackToCameraButton.setVisible(true);
            CapturePhotoButton.setVisible(false);
            TutorialButton.setVisible(false);
            ViewContactsButton.setVisible(false);
        });

        SavePersonInfoButton.addActionListener(e->{
            PersonName = PersonNameField.getText();
            PersonRelationship = PersonRelationshipField.getText();
            Person person = new Person(PersonName, PersonRelationship);

            person.setId(FileHandler.generateId(persons));
            persons.add(person);
            String curID = person.getId();
            System.out.println("ID: " + curID);
            saveFaceImage(curID, faceImage);
            fileHandler.savePersons(persons);

        });

    }

    private void saveFaceImage(String personID, Mat imageToSave) {
        // 1. Clean the name to make it filename-safe (remove spaces, etc.)
       // String cleanName = personName.replaceAll("\\s+", "_");

        // 2. Define the directory and filename
        String directoryPath = "saved_faces/";
        // You can change ".png" to ".jpg" if you prefer
        String filePath = directoryPath + personID + ".png";

        // 3. Create the directory if it doesn't exist
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 4. Save the image using OpenCV's imwrite
        // imwrite returns true if successful, false otherwise
        boolean isSaved = imwrite(filePath, imageToSave);

        if (isSaved) {
            System.out.println("Image successfully saved to: " + filePath);
        } else {
            System.out.println("Failed to save image.");
            JOptionPane.showMessageDialog(mainPanel, "Error saving image to disk.");
        }
    }

    //showAddPersonDialog(faceImage);

    // ui helpers
    private void updatePanelSizes() {
        int totalHeight = mainPanel.getHeight();
        if (totalHeight > 0) {
            int displayHeight = (int)(totalHeight * 0.87);
            int buttonHeight = (int)(totalHeight * 0.13);

            DisplayPanel.setPreferredSize(new Dimension(mainPanel.getWidth(), displayHeight));
            DisplayPanel.setMinimumSize(new Dimension(mainPanel.getWidth(), displayHeight));
            DisplayPanel.setMaximumSize(new Dimension(mainPanel.getWidth(), displayHeight));

            ButtonPanel.setPreferredSize(new Dimension(mainPanel.getWidth(), buttonHeight));
            ButtonPanel.setMinimumSize(new Dimension(mainPanel.getWidth(), buttonHeight));
            ButtonPanel.setMaximumSize(new Dimension(mainPanel.getWidth(), buttonHeight));

            mainPanel.revalidate();
        }
    }

    private void toggleDeleteButton(){
        for (Component c : PersonPanel.getComponents()) {
            if (c instanceof JPanel itemPanel) {
                for (Component inner : itemPanel.getComponents()) {
                    if (inner instanceof JButton DeleteButton) {
                        DeleteButton.setVisible(isEditing);
                    }
                }
            }
        }
    }

    public JPanel getPanel(){
        return mainPanel;
    }

    void setButtonFont(Container container){
        for(Component c1 : container.getComponents()){
            if(c1 instanceof JButton b){
                b.setFont(buttonFont);
            }

            if(c1 instanceof Container c2){
                setButtonFont(c2);
            }
        }
    }

    void setPLabelFont(Container container){
        for(Component c1 : container.getComponents()){
            if(c1 instanceof JLabel l){
                l.setFont(PLabelFont);
            }

            if(c1 instanceof JTextField t){
                t.setFont(PLabelFont);
            }

            if(c1 instanceof Container c2){
                setPLabelFont(c2);
            }
        }
    }


    // face helpers

    // this method is for loading the haarcascade file. Transferred it here from main
    private static CascadeClassifier loadFaceDetector() {
        try {
            System.out.println("Loading face detector from resources...");
            // STEP 1: Get the resource as an InputStream (from JAR/classpath)
            InputStream is = MainPanel.class.getResourceAsStream("/haarcascade_frontalface_default.xml");

            if (is == null) {
                System.out.println("Could not find haarcascade file in resources!");
                return null;
            } else {
                // STEP 2: Create a temporary file on the filesystem
                File tempFile = File.createTempFile("haarcascade", ".xml");
                tempFile.deleteOnExit(); // Ensure the temp file is deleted on exit
                FileOutputStream os = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];

                // STEP 3: Copy data from resource stream to the temporary file
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
                System.out.println("Temp file created at: " + tempFile.getAbsolutePath());

                // STEP 4: Initialize the CascadeClassifier using the temp file's path
                CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());

                if (classifier.empty()) {
                    System.out.println("Classifier is empty! XML loading failed.");
                    return null;
                } else {
                    return classifier;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while loading face detector:");
            e.printStackTrace();
            return null;
        }
    }

    // this method is for the camera feed
    private void startCamera() {
        System.out.println("Starting camera...");
        camera = new VideoCapture(0); // 0 is typically the default webcam

        if (!camera.isOpened()) {
            // Handle camera open failure
            System.out.println("ERROR: Cannot open camera!");
            JOptionPane.showMessageDialog(this.getPanel(), "Cannot open camera!");
            return;
        }

        System.out.println("Camera opened successfully");

        Thread cameraThread = new Thread(() -> {
            System.out.println("Camera thread started");
            Mat frame = new Mat();
            Mat grayFrame = new Mat();

            // --- MAIN VIDEO PROCESSING LOOP ---
            while (running) {
                if (!camera.read(frame)) { // Read next frame from camera
                    continue;
                }

                currentFrame = frame.clone(); // Store a copy of the original frame for potential capture

                // 1. Pre-process: Convert frame to grayscale for faster detection
                cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

                // 2. Face Detection
                RectVector detections = new RectVector();
                // detectMultiScale applies the Haar Cascade to find faces
                faceDetector.detectMultiScale(grayFrame, detections);

                int numFaces = (int)detections.size(); //cast to int because for some reason, this method returns a long
                if (numFaces > 0) {
                    // Find the largest face to focus on
                    Rect[] faces = new Rect[(int)numFaces];
                    for (int i = 0; i < numFaces; i++) {
                        faces[i] = detections.get(i);
                    }
                    currentFaceRect = getBiggestFace(faces);

                    // 3. Draw green rectangle on the color frame
                    rectangle(
                            frame,
                            new Point(currentFaceRect.x(), currentFaceRect.y()),
                            new Point(currentFaceRect.x() + currentFaceRect.width(),
                                    currentFaceRect.y() + currentFaceRect.height()),
                            new Scalar(0, 255, 0, 0), // BGR color: Green
                            3, LINE_8, 0
                    );
                } else {
                    currentFaceRect = null; // No face detected
                }

                // 4. Display: Convert OpenCV Mat to Swing Icon and update the JLabel cameraLabel
                BufferedImage bufferedImage = ImageUtils.matToBufferedImage(frame);

                SwingUtilities.invokeLater(() -> {  //scale the image
                    int panelWidth = CameraPanel.getWidth();
                    int panelHeight = CameraPanel.getHeight();

                    if (panelWidth > 0 && panelHeight > 0) {
                        // Scale image to panel dimensions
                        Image scaledImage = bufferedImage.getScaledInstance(panelWidth, panelHeight, Image.SCALE_FAST);
                        ImageIcon icon = new ImageIcon(bufferedImage);
                        CameraLabel.setIcon(new ImageIcon(scaledImage));
                    } else {
                        CameraLabel.setIcon(new ImageIcon(bufferedImage));
                    }
                });


                // Important code below to control CPU usage
                try {
                    Thread.sleep(30); // Control frame rate (~60 FPS) ----16 for 60fps, 30 for 30fps
                } catch (InterruptedException e) {
                    break;
                }
            }

            camera.release(); // Release camera resources on exit
            System.out.println("Camera thread stopped");
        });

        cameraThread.setDaemon(true); // Daemon threads allow application to exit even if this thread is running
        cameraThread.start();
    }

    // helper to find the detection rectangle with the largest area
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
            JOptionPane.showMessageDialog(mainPanel, "No face detected! Please look at the camera.", "No Face", JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.println("Extracting face from frame...");
        System.out.println("Face rect: x=" + currentFaceRect.x() +
                ", y=" + currentFaceRect.y() +
                ", w=" + currentFaceRect.width() +
                ", h=" + currentFaceRect.height());

        // 1. Extract the detected face region from the current frame
        faceImage = new Mat(currentFrame, currentFaceRect);
        System.out.println("Face image extracted: " +
                faceImage.cols() + "x" + faceImage.rows() +
                ", channels=" + faceImage.channels());

        // 2. Call the recognition service
//        System.out.println("Calling recognitionService.recognize()...");
//        FaceRecognitionService.RecognitionResult result = recognitionService.recognize(faceImage);
//        System.out.println("Recognition completed");
//        System.out.println("Result - isRecognized: " + result.isRecognized() +
//                ", confidence: " + result.getConfidence());
//
//        if (result.isRecognized()) {
//            // 3. Recognized: Show information about the known person
//            System.out.println("*** PERSON RECOGNIZED: " + result.getPerson().getName() + " ***");
//            showRecognizedPerson(result.getPerson(), result.getConfidence());
//        }
        //else {
            // 4. Unknown: Prompt user to add the new person
            System.out.println("*** PERSON NOT RECOGNIZED ***");
            int choice = JOptionPane.showConfirmDialog(mainPanel,
                    "Person not recognized. Would you like to add them?",
                    "Unknown Person",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                System.out.println("User chose to add new person");
                showAddPersonDialog(faceImage);
            } else {
                System.out.println("User chose not to add person");
            }
        //}
    }

    private void showRecognizedPerson(Person person, double confidence) {
        System.out.println("Showing recognized person dialog for: " + person.getName());

        String confidenceLevel = getConfidenceDescription(confidence);
        personInfoLabel.setText("Recognized: " + person.getName() + " - " + confidenceLevel);

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

        JOptionPane.showMessageDialog(mainPanel, message,"Person Recognized!", JOptionPane.INFORMATION_MESSAGE);
    }

    private String getConfidenceDescription(double confidence) {
        // Mirrors the logic in FaceRecognitionService
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


//    private void showAddPersonDialog(Mat faceImage) {
//        System.out.println("Opening add person dialog...");
//        PersonFormDialog dialog = new PersonFormDialog(this, faceImage);
//        dialog.setVisible(true);
//
//        if (dialog.isConfirmed()) {
//            Person newPerson = dialog.getPerson();
//            System.out.println("New person confirmed: " + newPerson.getName());
//
//            // 1. Add the new Person to the in-memory list
//            persons.add(newPerson);
//
//            // 2. Persist the updated list of persons to disk
//            System.out.println("Saving persons to file...");
//            fileHandler.savePersons(persons);
//
//            // 3. The face model must be **retrained** with the new person's face data
//            System.out.println("Retraining recognizer...");
//            recognitionService.train(persons);
//
//            infoLabel.setText("Saved: " + newPerson.getName() + " (" + newPerson.getRelationship() + ")");
//        } else {
//            System.out.println("Add person dialog cancelled");
//        }
//    }

    private void showAddPersonDialog(Mat faceImage){
        System.out.println("Opening add person dialog...");
//        PersonFormDialog dialog = new PersonFormDialog(this, faceImage);
//        dialog.setVisible(true);


    }

    private void setupTutorialPanel() {
        TutorialPanel = new JPanel();
        TutorialPanel.setLayout(new BorderLayout());
        TutorialPanel.setBackground(Color.WHITE);

        // --- 1. Header Section (Title) ---
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        // We remove the bottom border so the text padding below controls the spacing
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));

        JLabel titleLabel = new JLabel("Welcome to IMentia");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        headerPanel.add(titleLabel);

        TutorialPanel.add(headerPanel, BorderLayout.NORTH);

        // --- 2. Main Content ---
        // padding: TOP RIGHT BOTTOM LEFT
        // We set Top (60px) and Left/Right (60px) to be the same size.
        String htmlContent = "<html><body style='width: 100%; font-family: sans-serif;'>" +
                "<div style='padding: 60px 60px 60px 60px;'>" +

                // Description
                "<p style='font-size: 18px; color: #444; line-height: 1.5; margin-top: 0;'>" +
                "<b>IMentia</b> is a supportive memory assistant designed to help you recognize loved ones and daily companions.<br/>" +
                "By storing photos and details of important people, the application provides gentle, real-time reminders <br/>" +
                "of who someone is and how they are connected to you. With the help of caregivers to manage these memories,<br/> " +
                "<b>IMentia</b> aims to reduce confusion and strengthen your emotional connections with the people around you.<br/>" +
                "</p>" +

                // Separator
                "<hr style='margin-top: 30px; margin-bottom: 30px;'>" +

                // Instructions
                "<h3>How to use:</h3>" +
                "<p><b>1. Position yourself:</b><br/>" +
                "Sit comfortably in front of the camera so the face is clearly visible.</p><br/>" +
                "<p><b>2. Automatic Recognition:</b><br/>" +
                "Just look at the screen. If the system knows the person, their name will appear.</p><br/>" +
                "<p><b>3. Saving a New Person:</b><br/>" +
                "If the system doesn't know the person, press the <b>'Capture Photo'</b> button to save them.</p><br/>" +
                "<p><b>4. View List:</b><br/>" +
                "Press <b>'View Contacts'</b> to see all your saved family and friends.</p>" +
                "</div></body></html>";

        JLabel textLabel = new JLabel(htmlContent);

        // CRITICAL: This ensures the text starts at the TOP, not centered vertically
        textLabel.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scrollPane = new JScrollPane(textLabel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        TutorialPanel.add(scrollPane, BorderLayout.CENTER);
    }


}
