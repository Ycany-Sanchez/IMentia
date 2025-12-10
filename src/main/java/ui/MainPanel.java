// >>> FILE: src/main/java/ui/MainPanel.java
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
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;

public class MainPanel {
    private JPanel mainPanel;
    private JButton CapturePhotoButton;
    private JButton ViewContactsButton;
    private JButton TutorialButton;
    private JPanel CameraPanel;
    private JPanel ContactsPanel;
    private JPanel TutorialPanel;
    private JPanel PersonFormPanel;
    private JPanel PersonDetailsForm;
    private JButton EditContactButton;
    private JButton DeleteButton;
    private JPanel DisplayPanel;
    private JPanel ButtonPanel;
    private JPanel PersonPanel;
    private JButton BackToCameraButton;
    private JLabel PersonImageLabel;
    private JTextField PersonNameField;
    private JButton SavePersonInfoButton;
    private JLabel PersonNameLabel;
    private JPanel NamePanel;
    private JPanel RelationshipPanel;
    private JLabel PersonRelationshipLabel;
    private JTextField PersonRelationshipField;
    private JScrollPane ContactsScrollPane;
    private JLabel PersonDetailsImageLabel;
    private JPanel PersonDetailsTopSection;
    private JPanel PersonDetailsBottomSection;
    private JButton ADDMEETINGNOTESButton;
    private JButton EDITCONTACTButton;
    private JScrollPane MeetingNotesScrollPane;
    private JLabel MeetingNotesLabel;
    private JLabel PersonDetailPersonName;
    private JLabel PersonDetailPersonRel;
    private JLabel PersonDetailNameLabel;
    private JLabel PersonDetailRelLabel;
    private JTextArea MeetingNotesTextArea;

    private List<JPanel> contactListPanels = new ArrayList<>();

    private JLabel personInfoLabel = new JLabel();

    // --- NEW VARIABLES FOR DETAILS PANEL ---
    private JPanel PersonDetailsPanel;
    private JLabel detailsImageLabel;
    private JLabel detailsNameLabel;
    private JLabel detailsRelationLabel;
    private JPanel notesListPanel;
    private Person currentDisplayedPerson;
    // ---------------------------------------

    private CardLayout cardLayout = new CardLayout();
    private boolean isEditing = false;
    private JFrame tempFrame = new JFrame();

    private Font buttonFont = new Font("", Font.BOLD, 24);
    private Font HLabelFont = new Font("", Font.BOLD, 20);
    private Font PLabelFont = new Font("", Font.PLAIN, 20);

    private boolean hasSaved = false;

    private VideoCapture camera;
    private Mat faceImage;
    private CascadeClassifier faceDetector;
    private FaceRecognitionService recognitionService;
    private FileHandler fileHandler;
    private List<Person> persons;
    private String PersonName;
    private String PersonRelationship;

    private VideoPanel videoPanel;
    private Mat currentFrame;
    private Rect currentFaceRect;
    private boolean running = true;

    boolean isEditingMeetingNotes = false;



    public MainPanel(){
        this.fileHandler = new FileHandler();
        persons = fileHandler.loadPersonFile();
        this.faceDetector = loadFaceDetector();

        this.recognitionService = new FaceRecognitionService();
        this.recognitionService.train(persons);

        setupTutorialPanel();

        // Initialize the new Details UI
//        setupPersonDetailsPanel();

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePanelSizes();
            }
        });

        setUpUI();
        startCamera();
    }

    private void setUpUI(){
        videoPanel = new VideoPanel();
        CameraPanel.add(videoPanel, BorderLayout.CENTER);

        // Ensure Contacts Scroll Logic is correct
        PersonPanel.setLayout(new BoxLayout(PersonPanel, BoxLayout.Y_AXIS));
        if (ContactsScrollPane != null) {
            ContactsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            ContactsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

        DisplayPanel.setLayout(cardLayout);
        DisplayPanel.add(CameraPanel, "1");
        DisplayPanel.add(ContactsPanel, "2");
        DisplayPanel.add(PersonFormPanel, "3");
        DisplayPanel.add(TutorialPanel, "4");
        // Add the new Details Panel as card "5"
      //  DisplayPanel.add(PersonDetailsPanel, "5");
        DisplayPanel.add(PersonDetailsForm, "5");

        MeetingNotesTextArea.setVisible(false);
        MeetingNotesTextArea.setText("Add meeting notes here...");


        tempFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        System.out.println(tempFrame.getWidth() + " " + tempFrame.getHeight());

        setButtonFont(mainPanel);
        setPLabelFont(mainPanel);

        cardLayout.show(DisplayPanel, "1");
        EditContactButton.setFont(new Font("", Font.BOLD, 24));

        ViewContactsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                persons = fileHandler.loadPersonFile();
                refreshContactsPanel();

                cardLayout.show(DisplayPanel, "2");
                BackToCameraButton.setVisible(true);
                CapturePhotoButton.setVisible(false);
                TutorialButton.setVisible(false);
                ViewContactsButton.setVisible(false);
            }
        });

        TutorialButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "4");

                CapturePhotoButton.setVisible(false);
                ViewContactsButton.setVisible(false);
                TutorialButton.setVisible(false);

                BackToCameraButton.setVisible(true);
                BackToCameraButton.setText("BACK TO CAMERA");
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
                    isEditing = false;
                    EditContactButton.setText("EDIT LIST");
                    toggleDeleteButton();

                }
                hasSaved = false;
                cardLayout.show(DisplayPanel, "1");

                // Reset buttons for Camera view
                BackToCameraButton.setVisible(false);
                CapturePhotoButton.setVisible(true);
                TutorialButton.setVisible(true);
                ViewContactsButton.setVisible(true);
            }
        });

        CapturePhotoButton.addActionListener(e ->{
            if(captureFace()){
                cardLayout.show(DisplayPanel, "3");
                BufferedImage bufferedImage = ImageUtils.matToBufferedImage(faceImage);
                Image scaledImage = bufferedImage.getScaledInstance(200, 200, Image.SCALE_FAST);
                ImageIcon imageIcon = new ImageIcon(scaledImage);


                PersonImageLabel.setIcon(imageIcon);
                BackToCameraButton.setVisible(true);
                CapturePhotoButton.setVisible(false);
                TutorialButton.setVisible(false);
                ViewContactsButton.setVisible(false);
            }
        });

        SavePersonInfoButton.addActionListener(e->{
            String htmlMessage =
                    "<html><body style='width: 300px'>" +
                            "Do you want to save this person with these information?<br><br>" +
                            "<b>Name:</b> " + PersonNameField.getText() + "<br>" +
                            "<b>Relationship:</b> " + PersonRelationshipField.getText() +
                            "</body></html>";

            JLabel messageLabel = new JLabel(htmlMessage);

            messageLabel.setFont(PLabelFont);
            int op = JOptionPane.showConfirmDialog(mainPanel, messageLabel,
                    "Confirm Person Information", JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) {
                PersonName = PersonNameField.getText().trim();
                PersonRelationship = PersonRelationshipField.getText().trim();
                String PersonCapitalizedName = FileHandler.capitalizeLabel(PersonName);

                System.out.println("Cpaitalized name: " + PersonCapitalizedName);

                Person person = new Person(PersonCapitalizedName, PersonRelationship);

                person.setId(fileHandler.generateId(persons));
                person.setPersonImage(ImageUtils.matToBufferedImage(faceImage));
                persons.add(person);
                String curID = person.getId();
                System.out.println("ID: " + curID);


                if(fileHandler.savePersons(persons)){
                    saveFaceImage(curID, faceImage);
                    recognitionService.train(persons);
                    //showPersonDetails(person, faceImage);
                    setupPersonDetailsForm(person);
                    cardLayout.show(DisplayPanel, "5");
                } else {
                    BackToCameraButton.setVisible(false);
                    CapturePhotoButton.setVisible(true);
                    TutorialButton.setVisible(true);
                    ViewContactsButton.setVisible(true);
                    cardLayout.show(DisplayPanel, "1");
                }
                PersonNameField.setText("");
                PersonRelationshipField.setText("");
            }
        });


        ADDMEETINGNOTESButton.addActionListener(e->{

            isEditingMeetingNotes = !isEditingMeetingNotes;

            if(isEditingMeetingNotes){
                MeetingNotesTextArea.setVisible(true);
                ADDMEETINGNOTESButton.setText("SAVE MEETING NOTES");

            } else {
                MeetingNotesTextArea.setVisible(false);
                ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");
            }

        });

        MeetingNotesTextArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (MeetingNotesTextArea.getText().equals("Add meeting notes here...")) {
                    MeetingNotesTextArea.setText("");
                    MeetingNotesTextArea.setForeground(Color.BLACK); // Change back to standard text color
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (MeetingNotesTextArea.getText().isEmpty()) {
                    MeetingNotesTextArea.setForeground(Color.GRAY); // Set back to placeholder color
                    MeetingNotesTextArea.setText("Add meeting notes here...");
                }
            }


        });
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Request focus when the user clicks the background panel
                mainPanel.requestFocusInWindow();
            }
        });


    }



    private void setupPersonDetailsForm(Person p){
        Image scaledImage = p.getPersonImage().getScaledInstance(200, 200, Image.SCALE_FAST);
        PersonDetailsImageLabel.setIcon(new ImageIcon(scaledImage));

        PersonDetailPersonName.setText(p.getName());
        PersonDetailPersonRel.setText(p.getRelationship());
        PersonDetailNameLabel.setFont(HLabelFont);
        PersonDetailRelLabel.setFont(HLabelFont);
        MeetingNotesLabel.setFont(HLabelFont);

        ViewContactsButton.setVisible(true);
    }

//    private void setupPersonDetailsPanel() {
//        PersonDetailsPanel = new JPanel(new BorderLayout());
//        PersonDetailsPanel.setBackground(new Color(230, 230, 230));
//
//        // 1. Top Section (Image + Info) -> Goes to NORTH
//        JPanel topSection = new JPanel(new BorderLayout(20, 0));
//        topSection.setOpaque(false);
//        topSection.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));
//
//        // Image Label - Fixed Size to prevent stretching
//        detailsImageLabel = new JLabel();
//        Dimension imageSize = new Dimension(200, 200);
//        detailsImageLabel.setPreferredSize(imageSize);
//        detailsImageLabel.setMinimumSize(imageSize);
//        detailsImageLabel.setMaximumSize(imageSize);
//        detailsImageLabel.setSize(imageSize);
//        detailsImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
//        detailsImageLabel.setOpaque(true);
//        detailsImageLabel.setBackground(Color.LIGHT_GRAY);
//        detailsImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
//        detailsImageLabel.setVerticalAlignment(SwingConstants.CENTER);
//
//        // Text Info Panel
//        JPanel infoTextPanel = new JPanel();
//        infoTextPanel.setLayout(new BoxLayout(infoTextPanel, BoxLayout.Y_AXIS));
//        infoTextPanel.setOpaque(false);
//
//        detailsNameLabel = new JLabel("Name Placeholder");
//        detailsNameLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
//
//        detailsRelationLabel = new JLabel("Relation Placeholder");
//        detailsRelationLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
//        detailsRelationLabel.setForeground(Color.DARK_GRAY);
//
//        infoTextPanel.add(Box.createVerticalStrut(40));
//        infoTextPanel.add(detailsNameLabel);
//        infoTextPanel.add(Box.createVerticalStrut(15));
//        infoTextPanel.add(detailsRelationLabel);
//
//        // Edit Button (Top Right)
//        JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        editButtonPanel.setOpaque(false);
//        JButton editDetailsButton = new JButton("EDIT CONTACT");
//        editDetailsButton.setFont(buttonFont);
//        editButtonPanel.add(editDetailsButton);
//
//        topSection.add(detailsImageLabel, BorderLayout.WEST);
//        topSection.add(infoTextPanel, BorderLayout.CENTER);
//        topSection.add(editButtonPanel, BorderLayout.EAST);
//
//        // 2. Middle Section (Notes) -> Goes to CENTER (Fills remaining space)
//        JPanel notesSection = new JPanel(new BorderLayout(0, 5));
//        notesSection.setOpaque(false);
//        notesSection.setBorder(BorderFactory.createEmptyBorder(10, 40, 40, 40));
//
//        // Header
//        JPanel notesHeader = new JPanel(new BorderLayout());
//        notesHeader.setOpaque(false);
//
//        JLabel notesTitle = new JLabel("Notes From Your Meetings");
//        notesTitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
//
//        JButton addNoteButton = new JButton("ADD MEETING NOTES");
//        addNoteButton.setFont(buttonFont);
//
//        notesHeader.add(notesTitle, BorderLayout.WEST);
//        notesHeader.add(addNoteButton, BorderLayout.EAST);
//
//        // Notes List
//        notesListPanel = new JPanel();
//        notesListPanel.setLayout(new BoxLayout(notesListPanel, BoxLayout.Y_AXIS));
//        notesListPanel.setBackground(Color.WHITE);
//
//        // Wrapper
//        JPanel notesWrapper = new JPanel(new BorderLayout());
//        notesWrapper.setBackground(Color.WHITE);
//        notesWrapper.add(notesListPanel, BorderLayout.NORTH);
//
//        JScrollPane notesScroll = new JScrollPane(notesWrapper);
//        notesScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
//
//        notesSection.add(notesHeader, BorderLayout.NORTH);
//        notesSection.add(notesScroll, BorderLayout.CENTER);
//
//        // 3. Assemble
//        PersonDetailsPanel.add(topSection, BorderLayout.NORTH);
//        PersonDetailsPanel.add(notesSection, BorderLayout.CENTER);
//
//        // --- BUTTON ACTIONS ---
//        addNoteButton.addActionListener(e -> {
//            if(currentDisplayedPerson != null) {
//                JOptionPane.showMessageDialog(mainPanel, "Add note feature coming soon for " + currentDisplayedPerson.getName());
//            }
//        });
//
//        editDetailsButton.addActionListener(e -> {
//            if(currentDisplayedPerson != null) {
//                JOptionPane.showMessageDialog(mainPanel, "Edit feature coming soon.");
//            }
//        });
//    }
//
//    // --- UPDATED METHOD: Accepts an optional directImage for immediate display ---
//    private void showPersonDetails(Person person, Mat directImage) {
//        this.currentDisplayedPerson = person;
//
//        // Populate Text
//        detailsNameLabel.setText("<html>Person's Name: &nbsp;&nbsp; " + person.getName() + "</html>");
//        detailsRelationLabel.setText("<html>Your Relation: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; " + person.getRelationship() + "</html>");
//
//        BufferedImage buff = null;
//
//        try {
//            // 1. Priority: Use the image passed directly (from Save action)
//            if (directImage != null && !directImage.empty()) {
//                buff = ImageUtils.matToBufferedImage(directImage);
//            }
//            // 2. Fallback: Load from disk (from Contact List action)
//            else {
//                String filePath = "saved_faces/" + person.getId() + ".png";
//                File imgFile = new File(filePath);
//                if (imgFile.exists()) {
//                    Mat faceMat = ImageUtils.loadMatFromFile(filePath);
//                    if (faceMat != null && !faceMat.empty()) {
//                        buff = ImageUtils.matToBufferedImage(faceMat);
//                    }
//                }
//            }
//
//            // 3. Render the image if found
//            if (buff != null) {
//                // Logic to Fit Image within 200x200 maintaining aspect ratio
//                int originalW = buff.getWidth();
//                int originalH = buff.getHeight();
//                int targetW = 200;
//                int targetH = 200;
//
//                int newW = targetW;
//                int newH = (originalH * targetW) / originalW;
//
//                if (newH > targetH) {
//                    newH = targetH;
//                    newW = (originalW * targetH) / originalH;
//                }
//
//                Image scaled = buff.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
//                detailsImageLabel.setIcon(new ImageIcon(scaled));
//                detailsImageLabel.setText("");
//            } else {
//                detailsImageLabel.setIcon(null);
//                detailsImageLabel.setText("No Image");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            detailsImageLabel.setIcon(null);
//            detailsImageLabel.setText("Error");
//        }
//
//        // Clear Notes
//        notesListPanel.removeAll();
//        notesListPanel.revalidate();
//        notesListPanel.repaint();
//
//        // Switch View to Details
//        cardLayout.show(DisplayPanel, "5");
//
//        // Manage Footer
//        CapturePhotoButton.setVisible(false);
//        TutorialButton.setVisible(false);
//
//        BackToCameraButton.setVisible(true);
//        ViewContactsButton.setVisible(true);
//    }


    private void saveFaceImage(String personID, Mat imageToSave) {
        String directoryPath = "saved_faces/";
        String filePath = directoryPath + personID + ".png";

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        boolean isSaved = imwrite(filePath, imageToSave);

        if (isSaved) {
            System.out.println("Image successfully saved to: " + filePath);
        } else {
            System.out.println("Failed to save image.");
            // JOptionPane.showMessageDialog(mainPanel, "Error saving image to disk.");
        }
    }


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
        //refreshContactsPanel();
        for(JPanel panel : contactListPanels){
            for(Component c : panel.getComponents()){
                if(c instanceof JButton b){
                    if(!b.isVisible()) b.setVisible(true);
                    else b.setVisible(false);
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

    private void refreshContactsPanel() {
        PersonPanel.removeAll();
        int numPersons = persons.size();

        for (int i = 0; i < numPersons; i += 2) {

            Box rowBox = Box.createHorizontalBox();
            rowBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel card1 = createPersonEntryPanel(persons.get(i));
            rowBox.add(card1);

            rowBox.add(Box.createHorizontalStrut(20));

            if (i + 1 < numPersons) {
                JPanel card2 = createPersonEntryPanel(persons.get(i + 1));
                rowBox.add(card2);
                rowBox.add(Box.createHorizontalGlue());
            } else {
                rowBox.add(Box.createHorizontalGlue());
            }
            PersonPanel.add(rowBox);
        }
    }

    private JPanel createPersonEntryPanel(Person person) {
        // 1. Setup Panel with GridBagLayout (The most flexible layout)
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        final int FIXED_HEIGHT = 200;
        final int MIN_WIDTH = (mainPanel.getWidth()-60) / 2;
        Dimension fixedSize = new Dimension(MIN_WIDTH, FIXED_HEIGHT);
        panel.setMinimumSize(fixedSize);
        panel.setPreferredSize(fixedSize);
        panel.setMaximumSize(fixedSize);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));


        // -- Image Label --
        JLabel imageLabel = new JLabel("img", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(160, 160));
        imageLabel.setMinimumSize(new Dimension(160, 160));

        try {
            String filePath = "saved_faces/" + person.getId() + ".png";
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                Mat faceMat = ImageUtils.loadMatFromFile(filePath);
                if (faceMat != null && !faceMat.empty()) {
                    BufferedImage bufferedImage = ImageUtils.matToBufferedImage(faceMat);
                    Image scaledImage = bufferedImage.getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                    imageLabel.setText("");
                } else { imageLabel.setText("Load Fail"); }
            } else { imageLabel.setText("No Image"); }
        } catch (Exception e) { imageLabel.setText("Error"); }

        // -- Info Panel --
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(person.getName());
        nameLabel.setFont(HLabelFont);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel relationshipLabel = new JLabel("Relationship: " + person.getRelationship());
        relationshipLabel.setFont(PLabelFont);
        relationshipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(Box.createVerticalStrut(10)); // Top spacing
        infoPanel.add(nameLabel);
        infoPanel.add(relationshipLabel);

        JButton deleteButton = new JButton("DELETE");
        deleteButton.setVisible(false);
        deleteButton.setForeground(Color.RED);
        deleteButton.setFont(HLabelFont);

        deleteButton.addActionListener(e -> {
            // delete logic
            deleteContact(person);
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Pass NULL so it loads from disk (since we don't have the memory image here)
                setupPersonDetailsForm(person);
                cardLayout.show(DisplayPanel, "5");
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Row 0
        gbc.weightx = 0; // Do not stretch width
        gbc.fill = GridBagConstraints.VERTICAL; // Fill height if needed
        gbc.insets = new Insets(0, 0, 0, 10); // Right padding
        panel.add(imageLabel, gbc);

        // B. Add Info Panel (Middle Column)
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE; // Fill both width and height
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 0); // Reset padding
        panel.add(infoPanel, gbc);

        // C. Add Delete Button (Right Column)
        gbc.gridx = 2; // Column 2
        gbc.weightx = 0; // Do not stretch width
        gbc.fill = GridBagConstraints.NONE; // Do not resize the button
        gbc.anchor = GridBagConstraints.EAST; // PIN TO TOP-RIGHT
        panel.add(deleteButton, gbc);

        contactListPanels.add(panel);
        return panel;
    }

    private void deleteContact(Person personToDelete){
        String htmlMessage =
                "<html><body style='width: 300px'>" +
                        "Are you sure you want to delete this person from your contact list?" +
                        "</body></html>";

        JLabel messageLabel = new JLabel(htmlMessage);
        messageLabel.setFont(PLabelFont);

        int choice = JOptionPane.showConfirmDialog(
                mainPanel,
                messageLabel,
                "Confirm Delete Person",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            persons.remove(personToDelete);
            fileHandler.updatePersonFile(persons);

            try {
                File imageFile = new File("saved_faces", personToDelete.getId() + ".png");
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            } catch (Exception e) {
                System.out.println("Could not delete image file.");
            }
            refreshContactsPanel();

            JOptionPane.showMessageDialog(mainPanel, "Contact Deleted.");
        }
    }

    private static CascadeClassifier loadFaceDetector() {
        try {
            System.out.println("Loading face detector from resources...");
            InputStream is = MainPanel.class.getResourceAsStream("/haarcascade_frontalface_default.xml");

            if (is == null) {
                System.out.println("Could not find haarcascade file in resources!");
                return null;
            } else {
                File tempFile = File.createTempFile("haarcascade", ".xml");
                tempFile.deleteOnExit();
                FileOutputStream os = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];

                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
                System.out.println("Temp file created at: " + tempFile.getAbsolutePath());

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

    private void startCamera() {
        System.out.println("Starting camera...");
        camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("ERROR: Cannot open camera!");
            JOptionPane.showMessageDialog(this.getPanel(), "Cannot open camera!");
            return;
        }

        System.out.println("Camera opened successfully");

        Thread cameraThread = new Thread(() -> {
            System.out.println("Camera thread started");
            Mat frame = new Mat();
            Mat grayFrame = new Mat();

            int frameCount = 0;

            int missedDetectionCount = 0;
            int maxMissedDetections = 60;

            while (running) {
                if (!camera.read(frame)) {
                    continue;
                }

                currentFrame = frame.clone();

                if(frameCount % 4 == 0){
                    cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

                    RectVector detections = new RectVector();
                    faceDetector.detectMultiScale(grayFrame, detections);

                    int numFaces = (int)detections.size();

                    if (numFaces > 0) {
                        Rect[] faces = new Rect[(int)numFaces];
                        for (int i = 0; i < numFaces; i++) {
                            Rect temp = detections.get(i);
                            faces[i] = new Rect(temp.x(), temp.y(), temp.height(), temp.width());
                        }
                        currentFaceRect = getBiggestFace(faces);
                        missedDetectionCount = 0;
                    } else {
                        missedDetectionCount++;
                        if (missedDetectionCount > maxMissedDetections) {
                            currentFaceRect = null;
                        }
                    }
                }
                frameCount++;

                if (currentFaceRect != null) {
                    rectangle(
                            frame,
                            new Point(currentFaceRect.x(), currentFaceRect.y()),
                            new Point(currentFaceRect.x() + currentFaceRect.width(),
                                    currentFaceRect.y() + currentFaceRect.height()),
                            new Scalar(0, 255, 0, 0),
                            3, LINE_8, 0
                    );
                }

                BufferedImage bufferedImage = ImageUtils.matToBufferedImage(frame);

                SwingUtilities.invokeLater(() -> {
                    if (videoPanel != null) {
                        videoPanel.updateImage(bufferedImage);
                    }
                });

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

    private boolean captureFace() {
        System.out.println("=== captureFace() called ===");

        if (currentFrame == null || currentFaceRect == null) {
            System.out.println("No face detected in current frame");
            JOptionPane.showMessageDialog(mainPanel, "No face detected! Please look at the camera.", "No Face", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        System.out.println("Extracting face from frame...");

        faceImage = new Mat(currentFrame, currentFaceRect);

        FaceRecognitionService.RecognitionResult result = recognitionService.recognize(faceImage);
        if(result.isRecognized()){
            System.out.println("Person recognized: " + result.getPerson().getId());
            setupPersonDetailsForm(result.getPerson());
            CapturePhotoButton.setVisible(false);
            BackToCameraButton.setVisible(true);
            TutorialButton.setVisible(false);
            cardLayout.show(DisplayPanel, "5");
            return false;
        }

        System.out.println("*** PERSON NOT RECOGNIZED ***");
        int choice = JOptionPane.showConfirmDialog(mainPanel,
                "Person not recognized. Would you like to add them?",
                "Unknown Person",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            System.out.println("User chose to add new person");
            return true;
        } else {
            System.out.println("User chose not to add person");
            return false;
        }
    }
//
//    private void showRecognizedPerson(Person person, double confidence) {
//        System.out.println("Showing recognized person dialog for: " + person.getName());
//
//        String confidenceLevel = getConfidenceDescription(confidence);
//        personInfoLabel.setText("Recognized: " + person.getName() + " - " + confidenceLevel);
//
//        String message = String.format(
//                "<html><center>" +
//                        "<h1 style='font-size: 32px; margin: 10px;'>%s</h1>" +
//                        "<h2 style='font-size: 24px; color: #666; margin: 10px;'>%s</h2>" +
//                        "<div style='margin-top: 20px; padding: 10px; background-color: %s; border-radius: 5px;'>" +
//                        "<p style='font-size: 18px; margin: 5px;'><b>Match Quality:</b> %s</p>" +
//                        "<p style='font-size: 14px; color: #666; margin: 5px;'>Confidence Score: %.1f</p>" +
//                        "</div>" +
//                        "</center></html>",
//                person.getName(),
//                person.getRelationship(),
//                getConfidenceColor(confidence),
//                confidenceLevel,
//                confidence
//        );
//
//        JOptionPane.showMessageDialog(mainPanel, message,"Person Recognized!", JOptionPane.INFORMATION_MESSAGE);
//    }

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

    private void setupTutorialPanel() {
        TutorialPanel = new JPanel();
        TutorialPanel.setLayout(new BorderLayout());
        TutorialPanel.setBackground(Color.WHITE);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));

        JLabel titleLabel = new JLabel("Welcome to IMentia");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        headerPanel.add(titleLabel);

        TutorialPanel.add(headerPanel, BorderLayout.NORTH);

        String htmlContent = "<html><body style='width: 100%; font-family: sans-serif;'>" +
                "<div style='padding: 60px 60px 60px 60px;'>" +

                "<p style='font-size: 18px; color: #444; line-height: 1.5; margin-top: 0;'>" +
                "<b>IMentia</b> is a supportive memory assistant designed to help you recognize loved ones and daily companions.<br/>" +
                "By storing photos and details of important people, the application provides gentle, real-time reminders <br/>" +
                "of who someone is and how they are connected to you. With the help of caregivers to manage these memories,<br/> " +
                "<b>IMentia</b> aims to reduce confusion and strengthen your emotional connections with the people around you.<br/>" +
                "</p>" +

                "<hr style='margin-top: 30px; margin-bottom: 30px;'>" +

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

        textLabel.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scrollPane = new JScrollPane(textLabel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        TutorialPanel.add(scrollPane, BorderLayout.CENTER);
    }
}