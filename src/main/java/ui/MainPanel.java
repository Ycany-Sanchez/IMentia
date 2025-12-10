package ui;

import util.NoCamException;
import people.MeetingRecord;
import people.Person;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import service.FaceRecognitionService;
import service.PersonRecognitionManager;
import util.FileHandler;
import util.ImageHandler;
import util.ImageUtils;
import util.PersonDataManager;

import org.bytedeco.opencv.opencv_core.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class MainPanel extends AbstractMainPanel {

    // --- SERVICES ---
    // The Manager Facade replaces direct access to FileHandler, RecognitionService, and raw Person lists
    private PersonRecognitionManager personManager;
    private VideoProcessor videoProcessor;

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
    private JButton SAVEEDITButton; // Kept for UI binding compatibility
    private JButton CANCELEDITButton; // Kept for UI binding compatibility

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
    private JTextField PersonNameEdit;
    private JTextField PersonRelEdit;

    //SCROLLPANE
    private JScrollPane ContactsScrollPane;
    private JScrollPane MeetingNotesScrollPane;
    private JScrollPane MeetingNotesTextAreaScrollPane;

    //TEXTAREA
    private JTextArea MeetingNotesTextArea;

    //FONTS
    private Font buttonFont = new Font("", Font.BOLD, 24);
    private Font HLabelFont = new Font("", Font.BOLD, 20);
    private Font PLabelFont = new Font("", Font.PLAIN, 20);


    //OTHERS
    private List<JPanel> contactListPanels = new ArrayList<>();
    private List<JTextArea> meetingNoteAreas = new ArrayList<>(); // List to track displayed notes
    private Person currentDisplayedPerson;
    private CardLayout cardLayout = new CardLayout();
    private boolean isEditing = false;
    private JFrame tempFrame = new JFrame();
    private boolean hasSaved = false;
    private Mat faceImage; // Stored temporarily when capturing a new face

    boolean isEditingMeetingNotes = false;

    public MainPanel() {
        // Initialize the Facade Manager
        // This handles loading data, training the model, and file management
        this.personManager = new PersonRecognitionManager();

        setupTutorialPanel();

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePanelSizes();
            }
        });

        setUpUI();

        try {
            videoProcessor.startCamera();
        } catch (NoCamException e) {
            JOptionPane.showMessageDialog(mainPanel,
                    e.getMessage(),
                    "Camera Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // UI Setup and Display
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
        DisplayPanel.add(PersonDetailsForm, "5");

        MeetingNotesTextArea.setVisible(false);
        MeetingNotesTextArea.setFont(PLabelFont);
        MeetingNotesTextArea.setText("Add meeting notes here...");

        tempFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        System.out.println(tempFrame.getWidth() + " " + tempFrame.getHeight());

        setButtonFont(mainPanel);
        setPLabelFont(mainPanel);

        setScrollbarsIncrement(6);

        cardLayout.show(DisplayPanel, "1");
        EditContactButton.setFont(new Font("", Font.BOLD, 24));
        MeetingNotesTextAreaScrollPane.setVisible(false);

        // --- BUTTON LISTENERS ---

        ViewContactsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Facade ensures data is fresh and model is trained
                personManager.refreshDataAndTrain();
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

        CapturePhotoButton.addActionListener(e -> {
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

        // *** FACADE USAGE: Register New Person ***
        SavePersonInfoButton.addActionListener(e -> {
            String htmlMessage =
                    "<html><body style='width: 300px'>" +
                            "Do you want to save this person with these information?<br><br>" +
                            "<b>Name:</b> " + PersonNameField.getText() + "<br>" +
                            "<b>Relationship:</b> " + PersonRelationshipField.getText() +
                            "</body></html>";

            JLabel messageLabel = new JLabel(htmlMessage);
            messageLabel.setFont(PLabelFont);

            messageLabel.setFont(PLabelFont);
            int op = JOptionPane.showConfirmDialog(mainPanel, messageLabel,
                    "Confirm Person Information", JOptionPane.YES_NO_OPTION);

            if (op == JOptionPane.YES_OPTION) {
                String pName = PersonNameField.getText().trim();
                String pRel = PersonRelationshipField.getText().trim();

                // DELEGATE TO FACADE
                Person savedPerson = personManager.registerNewPerson(pName, pRel, faceImage);


                if (savedPerson != null) {
                    setupPersonDetailsForm(savedPerson);
                    cardLayout.show(DisplayPanel, "5");
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "Error saving person.", "Error", JOptionPane.ERROR_MESSAGE);
                    // Reset UI logic
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

        ADDMEETINGNOTESButton.addActionListener(e -> {
            isEditingMeetingNotes = !isEditingMeetingNotes;

            if(isEditingMeetingNotes){
                // --- SWITCH TO EDIT/ADD MODE ---
                ADDMEETINGNOTESButton.setText("SAVE MEETING NOTES");

                // 1. Show New Note Text Area
                MeetingNotesTextAreaScrollPane.setVisible(true);
                MeetingNotesTextArea.setVisible(true);
                // DO NOT reset text here, rely on FocusListener unless it's a fresh panel load
                // MeetingNotesTextArea.setText("Add meeting notes here...");
                MeetingNotesTextArea.setForeground(Color.GRAY);

                // 2. Enable Editing/Deleting on Existing Notes
                for(JTextArea noteArea : meetingNoteAreas){
                    noteArea.setEditable(true);
                    noteArea.setFocusable(true); // Enable focusing/editing

                    // Use standard, less jarring color scheme for editing
                    noteArea.setBackground(Color.WHITE);
                    noteArea.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY, 2), // Simple gray border
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }

            } else {
                // --- SAVE EDITS & ADD NEW NOTE MODE ---

                boolean newNoteAdded = false;

                // 1. Handle Edits to Existing Notes first (Must run if meetingNoteAreas is not empty)
                if (!meetingNoteAreas.isEmpty()) {
                    // This method now handles saving the edits and displaying the resulting message
                    saveEditedNotesToFile(newNoteAdded);
                }

                // 2. Handle New Note (The input box)
                String noteText = MeetingNotesTextArea.getText().trim();

                // Check if the user entered *any* non-placeholder text
                if (currentDisplayedPerson != null && !noteText.isEmpty() && !noteText.equals("Add meeting notes here...")) {
                    try {
                        // Create new conversation/meeting record for the person
                        MeetingRecord record = currentDisplayedPerson.newConversation(noteText);
                        record.createFile();
                        personManager.updatePersonDetails(currentDisplayedPerson);
                        newNoteAdded = true;

                        // If edits were saved above, the message was already shown.
                        // If no edits were saved (meetingNoteAreas was empty), show new note success message.
                        if (meetingNoteAreas.isEmpty()) {
                            JLabel successLabel = new JLabel("New Meeting note saved successfully!");
                            successLabel.setFont(PLabelFont); // <-- APPLY FONT HERE

                            JOptionPane.showMessageDialog(mainPanel,
                                    successLabel,
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }


                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JLabel errorLabel = new JLabel("Error saving new meeting note: " + ex.getMessage());
                        errorLabel.setFont(PLabelFont); // <-- APPLY FONT HERE

                        JOptionPane.showMessageDialog(mainPanel,
                                errorLabel,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }

                // --- FIX: Explicitly reset the New Note Text Area after save ---
                MeetingNotesTextArea.setText("Add meeting notes here...");
                MeetingNotesTextArea.setForeground(Color.GRAY);
                MeetingNotesTextArea.setVisible(false);
                MeetingNotesTextAreaScrollPane.setVisible(false);
                // -------------------------------------------------------------


                // 3. Cleanup and Reset UI
                ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");

                // 4. Refresh display to show saved state (including new/edited notes)
                setupPersonDetailsForm(currentDisplayedPerson);
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

        EDITCONTACTButton.addActionListener(e -> {
            // Check if a person is currently displayed before attempting to edit
            if (currentDisplayedPerson != null) {
                showEditDetailsDialog(currentDisplayedPerson);
            } else {
                JOptionPane.showMessageDialog(mainPanel, "No person selected for editing.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    protected void setupPersonDetailsForm(Person p){
        // Store reference to current person being displayed
        currentDisplayedPerson = p;
        personManager.ensureImageLoaded(p);

        if (p.getPersonImage() != null) {
            Image scaledImage = p.getPersonImage().getScaledInstance(200, 200, Image.SCALE_FAST);
            PersonDetailsImageLabel.setIcon(new ImageIcon(scaledImage));
        } else {
            // Updated print statement for clarity
            PersonDetailsImageLabel.setIcon(null);
            PersonDetailsImageLabel.setText("No Image");
            System.out.println("Person image is null.");
        }

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
        MeetingNotesTextAreaScrollPane.setVisible(false);
        isEditingMeetingNotes = false;
        ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");

        // Display existing meeting notes from file
        displayMeetingNotes(p);
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

    protected void displayMeetingNotes(Person person) {
        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
        notesPanel.setBackground(Color.WHITE);

        meetingNoteAreas.clear(); // Clear old references

        try {
            // Use the MeetingRecord helper to read notes (better data encapsulation)
            MeetingRecord reader = new MeetingRecord(person, "");
            List<String> allNotesBlocks = reader.readAllNotes(); // Reads the full blocks with START/END

            if (allNotesBlocks.isEmpty()) {
                JLabel noNotesLabel = new JLabel("No meeting notes yet.");
                noNotesLabel.setFont(PLabelFont);
                noNotesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                notesPanel.add(noNotesLabel);
            } else {
                for (String fullBlock : allNotesBlocks) {
                    // Extract only the displayable text (Date, Time, Content)
                    String displayableContent = extractContentFromNoteBlock(fullBlock);
                    addNoteToPanel(notesPanel, displayableContent);
                }
                notesPanel.add(Box.createVerticalGlue());
            }

        } catch (IOException e) {
            System.out.println("Error reading meeting notes: " + e.getMessage());
            e.printStackTrace();

            JLabel errorLabel = new JLabel("Error loading meeting notes.");
            errorLabel.setFont(PLabelFont);
            notesPanel.add(errorLabel);
        }

        MeetingNotesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        MeetingNotesScrollPane.setViewportView(notesPanel);
        MeetingNotesScrollPane.revalidate();
        MeetingNotesScrollPane.repaint();
    }

    private void showEditDetailsDialog(Person person) {
        if (person == null) {
            JOptionPane.showMessageDialog(mainPanel, "Error: No contact data available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField nameField = new JTextField(person.getName(), 15);
        JTextField relationshipField = new JTextField(person.getRelationship(), 15);

        Font dialogLabelFont = new Font("", Font.BOLD, 14);
        nameField.setFont(PLabelFont);
        relationshipField.setFont(PLabelFont);

        JPanel editPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel("New Name:");
        nameLabel.setFont(dialogLabelFont);
        editPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        editPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel relLabel = new JLabel("New Relationship:");
        relLabel.setFont(dialogLabelFont);
        editPanel.add(relLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        editPanel.add(relationshipField, gbc);

        int result = JOptionPane.showConfirmDialog(
                mainPanel,
                editPanel,
                "Edit Contact Details",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newRel = relationshipField.getText().trim();

            if (newName.isEmpty() || newRel.isEmpty()) {
                JLabel errorLabel = new JLabel("Name and Relationship cannot be empty.");
                errorLabel.setFont(PLabelFont); // <-- APPLY FONT HERE
                JOptionPane.showMessageDialog(mainPanel, errorLabel, "Error", JOptionPane.ERROR_MESSAGE);
                showEditDetailsDialog(person);
                return;
            }

            // *** FACADE USAGE: Update Person ***
            person.setName(FileHandler.capitalizeLabel(newName));
            person.setRelationship(newRel);

            personManager.updatePersonDetails(person);

            JOptionPane.showMessageDialog(mainPanel, "Contact details updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            setupPersonDetailsForm(person);
        }
    }


    // UI Helpers
    private void setScrollbarsIncrement(int num){
        ContactsScrollPane.getVerticalScrollBar().setUnitIncrement(num);
        MeetingNotesScrollPane.getVerticalScrollBar().setUnitIncrement(num);
        MeetingNotesTextAreaScrollPane.getVerticalScrollBar().setUnitIncrement(num);
    }

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

    public JPanel getPanel(){
        return mainPanel;
    }

    protected void toggleDeleteButton(){
        for(JPanel panel : contactListPanels){
            for(Component c : panel.getComponents()){
                if(c instanceof JButton b){
                    if(!b.isVisible()) b.setVisible(true);
                    else b.setVisible(false);
                }
            }
        }
    }

    // UI Logic
    protected boolean captureFace() {
        System.out.println("=== captureFace() called ===");
        Rect currentFaceRect = videoProcessor.getCurrentFaceRect();
        Mat currentFrame = videoProcessor.getCurrentFrame();

        if (currentFrame == null || currentFaceRect == null) {
            System.out.println("No face detected in current frame");
            JLabel errorLabel = new JLabel("No face detected! Please look at the camera.");
            errorLabel.setFont(PLabelFont);

            JOptionPane.showMessageDialog(mainPanel, errorLabel, "No Face", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        System.out.println("Extracting face from frame...");

        faceImage = new Mat(currentFrame, currentFaceRect);

        // *** FACADE USAGE: Recognize ***
        FaceRecognitionService.RecognitionResult result = personManager.recognizeFace(faceImage);

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
        JLabel questionLabel = new JLabel("Person not recognized. Would you like to add them?");
        questionLabel.setFont(PLabelFont);
        int choice = JOptionPane.showConfirmDialog(mainPanel,
                questionLabel,
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
            // *** FACADE USAGE: Delete ***
            personManager.deletePerson(personToDelete);

            refreshContactsPanel();

            JLabel successLabel = new JLabel("Contact Deleted.");
            successLabel.setFont(PLabelFont); // <-- APPLY FONT HERE
            JOptionPane.showMessageDialog(mainPanel, successLabel);

            EditContactButton.setText("EDIT LIST");
            isEditing = !isEditing;
        }
    }



    private void addNoteToPanel(JPanel parent, String noteText) {
        MeetingNotesScrollPane.setVisible(true);
        MeetingNotesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JTextArea noteArea = new JTextArea(noteText);
        noteArea.setBackground(Color.WHITE);
        noteArea.setFocusable(false); // Default state: Not focusable/editable
        noteArea.setFont(PLabelFont);
        noteArea.setEditable(false); // Default state
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        Border lineBorder = BorderFactory.createMatteBorder(0, 0, 2, 0, Color.LIGHT_GRAY);
        Border marginBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        noteArea.setBorder(BorderFactory.createCompoundBorder(lineBorder, marginBorder));

        noteArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Calculate height based on content
        // Set a temporary size to allow JTextArea to calculate its preferred height
        noteArea.setSize(new Dimension(parent.getWidth(), 9999));
        Dimension preferredSize = noteArea.getPreferredSize();

        noteArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));

        parent.add(noteArea);

        meetingNoteAreas.add(noteArea); // <-- ADD TO LIST
    }

    private void saveEditedNotesToFile(boolean newNoteAdded) {
        if (currentDisplayedPerson == null) return;

        String filePath = Paths.get("imentia_data", "Meeting_Notes", currentDisplayedPerson.getId() + ".txt").toString();
        File notesFile = new File(filePath);

        List<String> finalNotes = new ArrayList<>();
        int editedCount = 0;
        int deletedCount = 0;

        for (JTextArea noteArea : meetingNoteAreas) {
            String editedText = noteArea.getText().trim();

            if (editedText.isEmpty()) {
                deletedCount++;
                continue; // Note was deleted by clearing text
            }

            // Reconstruct the note block based on the expected format (Date, Time, Content)
            // Note: The displayed text contains Date\nTime\n\nContent
            String[] lines = editedText.split("\n", 4);

            StringBuilder noteBlock = new StringBuilder();
            noteBlock.append("----- NOTE START -----\n");

            // Append header lines (Date and Time, assuming they are the first two lines)
            // Use lines[0] and lines[1] if they exist
            if (lines.length > 0) noteBlock.append(lines[0].trim()).append("\n");
            if (lines.length > 1) noteBlock.append(lines[1].trim()).append("\n");
            noteBlock.append("\n"); // Blank line after time

            // Append the remaining content (lines[3] onwards)
            if (lines.length > 2) {
                // Reconstruct the content from the 3rd line onwards (lines[2] is the blank line)
                // The actual content starts from lines[3] if it exists, but the split(..., 4) groups the rest into lines[3]
                // Let's just use the entirety of the text area content and rely on the initial split structure

                // Content is lines[3] if present, or lines[2] if the text area didn't contain the blank line
                StringBuilder contentBody = new StringBuilder();
                for (int i = 2; i < lines.length; i++) {
                    contentBody.append(lines[i]).append("\n");
                }
                noteBlock.append(contentBody.toString().trim()).append("\n");

            } else if (lines.length == 1) {
                // Only one line exists, assume it is the content
                noteBlock.append(editedText).append("\n");
            }


            noteBlock.append("----- NOTE END -----\n");
            noteBlock.append("\n");

            finalNotes.add(noteBlock.toString());
            editedCount++;
        }

        // Rewrite the entire file (false = overwrite)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(notesFile, false))) {
            for (String note : finalNotes) {
                bw.write(note);
            }

            String messageText = "Notes successfully saved. " + editedCount + " notes remaining. " + deletedCount + " notes deleted.";

            JLabel messageLabel = new JLabel(messageText);
            messageLabel.setFont(PLabelFont);
            if (!newNoteAdded) {
                JOptionPane.showMessageDialog(mainPanel, messageLabel, "Edits Saved", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException e) {
            JLabel errorLabel = new JLabel("Error saving note edits: " + e.getMessage());
            errorLabel.setFont(PLabelFont);
            JOptionPane.showMessageDialog(mainPanel, errorLabel, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extractContentFromNoteBlock(String fullBlock) {
        String content = fullBlock.trim();

        // Remove "START" and "END" tags
        if (content.startsWith("----- NOTE START -----")) {
            content = content.substring("----- NOTE START -----".length()).trim();
        }
        if (content.endsWith("----- NOTE END -----")) {
            // Find the last occurrence of the END tag and cut before it
            content = content.substring(0, content.lastIndexOf("----- NOTE END -----")).trim();
        }
        return content;
    }

    private JPanel createPersonEntryPanel(Person person) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        final int FIXED_HEIGHT = 200;
        final int MIN_WIDTH = (mainPanel.getWidth()-60) / 2;
        Dimension fixedSize = new Dimension(MIN_WIDTH, FIXED_HEIGHT);
        panel.setMinimumSize(fixedSize);
        panel.setPreferredSize(fixedSize);
        panel.setMaximumSize(fixedSize);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel imageLabel = new JLabel("img", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(160, 160));
        imageLabel.setMinimumSize(new Dimension(160, 160));

        try {
            // Using standard path resolution
            String directoryPath = Paths.get("imentia_data", "saved_faces").toString();
            String filePath = Paths.get(directoryPath, person.getId() + ".png").toString();
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                Mat faceMat = ImageHandler.loadMatFromFile(filePath);
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

    protected void refreshContactsPanel() {
        PersonPanel.removeAll();
        // *** FACADE USAGE: Get Data ***
        List<Person> persons = personManager.getAllPersons();
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

}