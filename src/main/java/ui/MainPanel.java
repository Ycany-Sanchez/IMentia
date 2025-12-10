// >>> FILE: src/main/java/ui/MainPanel.java
package ui;
import people.MeetingRecord;
import people.Person;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import service.FaceRecognitionService;
import util.FileHandler;
import util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class MainPanel extends AbstractMainPanel {

    //PANELS
    private JPanel mainPanel;
    private JPanel CameraPanel;
    private JPanel ContactsPanel;
    private JPanel TutorialPanel;
    private JPanel PersonFormPanel;
    private JPanel PersonDetailsForm;
    private JPanel DisplayPanel;
    private JPanel ButtonPanel;
    private JPanel PersonPanel;
    private JPanel NamePanel;
    private JPanel RelationshipPanel;
    private JPanel PersonDetailsTopSection;
    private JPanel PersonDetailsBottomSection;
    //BUTTONS
    private JButton CapturePhotoButton;
    private JButton ViewContactsButton;
    private JButton TutorialButton;
    private JButton EditContactButton;
    private JButton BackToCameraButton;
    private JButton SavePersonInfoButton;
    private JButton ADDMEETINGNOTESButton;
    private JButton EDITCONTACTButton;

    //LABEL
    private JLabel PersonNameLabel;
    private JLabel PersonImageLabel;
    private JLabel PersonRelationshipLabel;
    private JLabel MeetingNotesLabel;
    private JLabel PersonDetailPersonName;
    private JLabel PersonDetailPersonRel;
    private JLabel PersonDetailNameLabel;
    private JLabel PersonDetailRelLabel;
    private JLabel PersonDetailsImageLabel;

    //TEXTFIELD
    private JTextField PersonNameField;
    private JTextField PersonRelationshipField;

    //SCROLLPANE
    private JScrollPane ContactsScrollPane;
    private JScrollPane MeetingNotesScrollPane;

    //TEXTAREA
    private JTextArea MeetingNotesTextArea;
    private JEditorPane editorPane1;
    private JButton SAVEEDITButton;
    private JButton CANCELEDITButton;
    private JTextField PersonNameEdit;
    private JTextField PersonRelEdit;


    //FONTS
    private Font buttonFont = new Font("", Font.BOLD, 24);
    private Font HLabelFont = new Font("", Font.BOLD, 20);
    private Font PLabelFont = new Font("", Font.PLAIN, 20);


    //OTHERS
    private List<JPanel> contactListPanels = new ArrayList<>();
    private Person currentDisplayedPerson;
    private CardLayout cardLayout = new CardLayout();
    private boolean isEditing = false;
    private JFrame tempFrame = new JFrame();
    private boolean hasSaved = false;

    //    private VideoCapture camera;
    private VideoProcessor videoProcessor;
    private Mat faceImage;
    private CascadeClassifier faceDetector;
    private FaceRecognitionService recognitionService;
    private FileHandler fileHandler;
    private List<Person> persons;
    private String PersonName;
    private String PersonRelationship;
    private Mat currentFrame;
    boolean isEditingMeetingNotes = false;
    boolean isEditingPersonDetails = false;

    public MainPanel(){
        this.fileHandler = new FileHandler();
        persons = fileHandler.loadPersonFile();

        this.recognitionService = new FaceRecognitionService();
        this.recognitionService.train(persons);

        setupTutorialPanel();

        // Initialize the new Details UI

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePanelSizes();
            }
        });

        setUpUI();
        videoProcessor.startCamera();
    }

    @Override
    protected void setUpUI(){
        videoProcessor = new VideoProcessor();
        CameraPanel.add(videoProcessor, BorderLayout.CENTER);

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


        // Update the ADDMEETINGNOTESButton ActionListener to save meeting notes
        // Update the ADDMEETINGNOTESButton ActionListener to save meeting notes
        ADDMEETINGNOTESButton.addActionListener(e->{
            isEditingMeetingNotes = !isEditingMeetingNotes;

            if(isEditingMeetingNotes){
                MeetingNotesTextArea.setVisible(true);
                ADDMEETINGNOTESButton.setText("SAVE MEETING NOTES");
            } else {
                // Save the meeting notes when user clicks save
                String noteText = MeetingNotesTextArea.getText().trim();

                // Don't save if it's empty or still has placeholder text
                if (!noteText.isEmpty() && !noteText.equals("Add meeting notes here...")) {
                    // Find the current person being displayed and add the note
                    if (currentDisplayedPerson != null) {
                        try {
                            // Find the actual person object in the persons list
                            Person personToUpdate = null;
                            for (Person p : persons) {
                                if (p.getId().equals(currentDisplayedPerson.getId())) {
                                    personToUpdate = p;
                                    break;
                                }
                            }

                            if (personToUpdate != null) {
                                // Create new conversation/meeting record for the person
                                MeetingRecord record = personToUpdate.newConversation(noteText);

                                // Save the meeting record to file
                                record.createFile();

                                // Save persons list to persist the lastestConv reference
                                fileHandler.savePersons(persons);

                                // Update the currentDisplayedPerson reference
                                currentDisplayedPerson = personToUpdate;

                                // Show confirmation
                                JOptionPane.showMessageDialog(mainPanel,
                                        "Meeting note saved successfully!",
                                        "Success",
                                        JOptionPane.INFORMATION_MESSAGE);

                                // Refresh the display to show the new note
                                setupPersonDetailsForm(currentDisplayedPerson);
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(mainPanel,
                                    "Error saving meeting note: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Please enter meeting notes before saving.",
                            "Empty Note",
                            JOptionPane.WARNING_MESSAGE);
                }

                // Clear the text area and reset
                MeetingNotesTextArea.setText("Add meeting notes here...");
                MeetingNotesTextArea.setForeground(Color.GRAY);
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

        EDITCONTACTButton.addActionListener(e->{
            isEditingPersonDetails = !isEditingPersonDetails;
            if(isEditingPersonDetails){
                EDITCONTACTButton.setVisible(false);
                SAVEEDITButton.setVisible(true);
                CANCELEDITButton.setVisible(true);

                PersonNameEdit.setVisible(true);
                PersonRelEdit.setVisible(true);
                PersonNameEdit.setText(currentDisplayedPerson.getName());
                PersonRelEdit.setText(currentDisplayedPerson.getRelationship());

                PersonDetailPersonName.setVisible(false);
                PersonDetailPersonRel.setVisible(false);

            }
        });


    }


    @Override
    protected void setupPersonDetailsForm(Person p){
        // Store reference to current person being displayed
        currentDisplayedPerson = p;

        Image scaledImage = p.getPersonImage().getScaledInstance(200, 200, Image.SCALE_FAST);
        PersonDetailsImageLabel.setIcon(new ImageIcon(scaledImage));

        PersonDetailPersonName.setText(p.getName());
        PersonDetailPersonRel.setText(p.getRelationship());
        PersonDetailNameLabel.setFont(HLabelFont);
        PersonDetailRelLabel.setFont(HLabelFont);
        MeetingNotesLabel.setFont(HLabelFont);

        ViewContactsButton.setVisible(true);

        // Reset the meeting notes input area
        MeetingNotesTextArea.setText("Add meeting notes here...");
        MeetingNotesTextArea.setForeground(Color.GRAY);
        MeetingNotesTextArea.setVisible(false);
        isEditingMeetingNotes = false;
        ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");

        // Display existing meeting notes from file
        displayMeetingNotes(p);
    }

<<<<<<< HEAD
    @Override
    protected void displayMeetingNotes(Person person) {
        // Create a panel to hold all meeting notes
=======

    private void displayMeetingNotes(Person person) {
>>>>>>> a5a857b4f902987cf676e1840879ee677f4cd740
        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
        notesPanel.setBackground(Color.WHITE);
        String FolderName = "Meeting_Notes";

        try {
            String directoryPath = Paths.get(fileHandler.getDataFolder(), FolderName).toString();
            String filePath = Paths.get(directoryPath, person.getId() + ".txt").toString();

            File notesFile = new File(filePath);

            System.out.println("Trying to load notes from: " + notesFile.getAbsolutePath());
            System.out.println("File exists: " + notesFile.exists());

            if (notesFile.exists()) {

                List<String> allNotes = new ArrayList<>();

                try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    StringBuilder currentNote = null;

                    while ((line = br.readLine()) != null) {

                        if (line.equals("----- NOTE START -----")) {
                            currentNote = new StringBuilder();
                        }
                        else if (line.equals("----- NOTE END -----")) {
                            if (currentNote != null) {
                                allNotes.add(currentNote.toString());  // store entire note
                            }
                            currentNote = null;
                        }
                        else if (currentNote != null) {
                            currentNote.append(line).append("\n");   // preserve ALL newlines
                        }
                    }
                }

                if (allNotes.isEmpty()) {
                    JLabel noNotesLabel = new JLabel("No meeting notes yet.");
                    noNotesLabel.setFont(PLabelFont);
                    noNotesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    notesPanel.add(noNotesLabel);
                } else {
                    for (String note : allNotes) {
                        addNoteToPanel(notesPanel, note); // your existing rendering method
                    }
                }

            } else {
                JLabel noNotesLabel = new JLabel("No meeting notes yet.");
                noNotesLabel.setFont(PLabelFont);
                noNotesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                notesPanel.add(noNotesLabel);
            }

        } catch (IOException e) {
            System.out.println("Error reading meeting notes: " + e.getMessage());
            e.printStackTrace();

            JLabel errorLabel = new JLabel("Error loading meeting notes.");
            errorLabel.setFont(PLabelFont);
            notesPanel.add(errorLabel);
        }

        MeetingNotesScrollPane.setViewportView(notesPanel);
        MeetingNotesScrollPane.revalidate();
        MeetingNotesScrollPane.repaint();
    }

    // Helper method to add a note entry to the panel
    private void addNoteToPanel(JPanel parent, String noteText) {

        JTextArea noteArea = new JTextArea(noteText);
        noteArea.setFont(PLabelFont);
        noteArea.setEditable(false);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setBackground(new Color(245, 245, 245));
        noteArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(noteArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));  // You can adjust height
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(Color.WHITE);
        wrapper.add(scrollPane);
        wrapper.add(Box.createVerticalStrut(15));

        parent.add(wrapper);
    }


<<<<<<< HEAD
    protected void saveFaceImage(String personID, Mat imageToSave) {
=======

    private void saveFaceImage(String personID, Mat imageToSave) {
>>>>>>> a5a857b4f902987cf676e1840879ee677f4cd740
        String directoryPath = "imentia_data/saved_faces/";
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

    @Override
    protected void updatePanelSizes() {
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

    @Override
    protected void toggleDeleteButton(){
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

    @Override
    protected void refreshContactsPanel() {
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
            String directoryPath = Paths.get(fileHandler.getDataFolder(), "imentia_data/saved_faces/").toString();
            String filePath = Paths.get(directoryPath, person.getId() + ".png").toString();
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

    @Override
    protected void deleteContact(Person personToDelete){
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
            recognitionService.train(persons);


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

    @Override
    protected boolean captureFace() {
        System.out.println("=== captureFace() called ===");
        Rect currentFaceRect = videoProcessor.getCurrentFaceRect();
        Mat currentFrame = videoProcessor.getCurrentFrame();

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