// >>> FILE: src/main/java/ui/MainPanel.java
package ui;

import people.MeetingRecord;
import people.Person;

import org.bytedeco.opencv.opencv_core.*;
import service.FaceRecognitionService;
import service.PersonRecognitionManager;
import util.FileHandler;
import util.ImageHandler;
import util.ImageUtils;
import util.exceptions.PersonAlreadyExistsException;
import util.exceptions.PersonSaveException;
import util.exceptions.NoCamException;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainPanel extends AbstractMainPanel {

    // --- SERVICES ---
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

    // NEW: Start Screen Panel
    private JPanel StartScreenPanel;

    //BUTTONS
    private JButton CapturePhotoButton;
    private JButton ViewContactsButton;
    private JButton HomeButton;
    private JButton EditContactButton;
    private JButton BackToCameraButton;
    private JButton SavePersonInfoButton;
    private JButton ADDMEETINGNOTESButton;
    private JButton EDITCONTACTButton;
    private JButton SAVEEDITButton;
    private JButton CANCELEDITButton;

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
    private JLabel msgLabel;




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
    private List<JTextArea> meetingNoteAreas = new ArrayList<>();
    private Person currentDisplayedPerson;
    private CardLayout cardLayout = new CardLayout();
    private boolean isEditing = false;
    private boolean hasSaved = false;
    private List<Mat> faceImages = new ArrayList<>();
    private CameraLifecycleManager cameraManager;

    boolean isEditingMeetingNotes = false;

    public MainPanel() {
        // Builds all UI components in code (replaces IntelliJ GUI Designer
        // .form injection, so the app compiles and runs with any build tool).
        buildFormUI();
        // Initialize the Facade Manager
        this.personManager = new PersonRecognitionManager();

        setupTutorialPanel();
        setupStartScreen(); // Initialize the start screen

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePanelSizes();
            }
        });

        setUpUI();


        cameraManager = new CameraLifecycleManager(videoProcessor, mainPanel);
    }

    // UI Setup and Display
    @Override
    protected void setUpUI() {
        videoProcessor = new VideoProcessor();
        CameraPanel.add(videoProcessor, BorderLayout.CENTER);

        // Ensure Contacts Scroll Logic is correct
        PersonPanel.setLayout(new BoxLayout(PersonPanel, BoxLayout.Y_AXIS));
        if (ContactsScrollPane != null) {
            ContactsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            ContactsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

        DisplayPanel.setLayout(cardLayout);

        // Add Panels
        DisplayPanel.add(StartScreenPanel, "START");
        DisplayPanel.add(CameraPanel, "1");
        DisplayPanel.add(ContactsPanel, "2");
        DisplayPanel.add(PersonFormPanel, "3");
        DisplayPanel.add(TutorialPanel, "4");
        DisplayPanel.add(PersonDetailsForm, "5");

        MeetingNotesTextArea.setVisible(false);
        MeetingNotesTextArea.setFont(PLabelFont);
        MeetingNotesTextArea.setText("Add meeting notes here...");

        setButtonFont(mainPanel);
        setPLabelFont(mainPanel);
        setScrollbarsIncrement(6);

        msgLabel = new JLabel();
        msgLabel.setFont(PLabelFont);


        // Initial State: Show Start Screen, Hide Buttons
        cardLayout.show(DisplayPanel, "START");
        ButtonPanel.setVisible(false);

        // Update the Home Button Text (if it was labeled Tutorial before)
        if(HomeButton != null) HomeButton.setText("HOME SCREEN");

        EditContactButton.setFont(new Font("", Font.BOLD, 24));
        MeetingNotesTextAreaScrollPane.setVisible(false);

        // --- BUTTON LISTENERS ---

        ViewContactsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraManager.stopCamera();
                personManager.refreshDataAndTrain();
                refreshContactsPanel();

                cardLayout.show(DisplayPanel, "2");

                BackToCameraButton.setVisible(true);
                BackToCameraButton.setText("BACK TO CAMERA"); // <--- ADD THIS LINE TO BE SAFE

                CapturePhotoButton.setVisible(false);
                if(HomeButton != null) HomeButton.setVisible(false);
                ViewContactsButton.setVisible(false);
            }
        });

        // --- CHANGED: Tutorial Button is now Home Button ---
        HomeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Go back to Start Screen
                cameraManager.stopCamera();
                cardLayout.show(DisplayPanel, "START");
                // Hide the main button controls since Start Screen has its own "Start" button
                ButtonPanel.setVisible(false);
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
                // --- NEW LOGIC: Check if we are in Tutorial Mode ---
                if (BackToCameraButton.getText().equals("BACK TO HOME SCREEN")) {
                    cameraManager.stopCamera();
                    cardLayout.show(DisplayPanel, "START");
                    ButtonPanel.setVisible(false); // Hide buttons on Start Screen
                    return; // Stop here, don't execute the rest
                }
                // ---------------------------------------------------

                // Existing "Back to Camera" Logic
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
                if(HomeButton != null) HomeButton.setVisible(true);
                ViewContactsButton.setVisible(true);

                cameraManager.startCamera();
                ButtonPanel.setVisible(true);
            }
        });

        CapturePhotoButton.addActionListener(e -> {
            if(captureFace()){
                cameraManager.stopCamera();
                cardLayout.show(DisplayPanel, "3");
                BufferedImage bufferedImage = ImageUtils.matToBufferedImage(faceImages.get(0));
                Image scaledImage = bufferedImage.getScaledInstance(200, 200, Image.SCALE_FAST);
                ImageIcon imageIcon = new ImageIcon(scaledImage);

                PersonImageLabel.setIcon(imageIcon);

                BackToCameraButton.setVisible(true);
                BackToCameraButton.setText("BACK TO CAMERA"); // <--- ADD THIS LINE TO BE SAFE

                CapturePhotoButton.setVisible(false);
                if(HomeButton != null) HomeButton.setVisible(false);
                ViewContactsButton.setVisible(false);
            }
        });

        SavePersonInfoButton.addActionListener(e -> {
            String htmlMessage = "<html><body style='width: 300px'>Do you want to save this person with these information?<br><br><b>Name:</b> " + PersonNameField.getText() + "<br><b>Relationship:</b> " + PersonRelationshipField.getText() + "</body></html>";
            JLabel messageLabel = new JLabel(htmlMessage);
            messageLabel.setFont(PLabelFont);
            int op = JOptionPane.showConfirmDialog(mainPanel, messageLabel, "Confirm Person Information", JOptionPane.YES_NO_OPTION);

            if (op == JOptionPane.YES_OPTION) {
                String pName = PersonNameField.getText().trim();
                String pRel = PersonRelationshipField.getText().trim();
                Person savedPerson = null;

                try {
                    savedPerson = personManager.registerNewPerson(pName, pRel, faceImages);
                } catch (PersonAlreadyExistsException ex) {
                    JOptionPane.showMessageDialog(
                            mainPanel,
                            ex.getMessage(),
                            "Duplicate Contact",
                            JOptionPane.ERROR_MESSAGE
                    );

                    //OPENS THE RECOGNIZED NAME PANEL
                    Person existing = personManager.getAllPersons()
                            .stream()
                            .filter(p -> p.getName().equalsIgnoreCase(ex.getPersonName()))
                            .findFirst()
                            .orElse(null);


                    if (existing != null) {
                        setupPersonDetailsForm(existing);
                        cardLayout.show(DisplayPanel, "5");
                    }

                    PersonNameField.setText("");
                    PersonRelationshipField.setText("");
                    return;

                } catch (PersonSaveException ex) {

                    JOptionPane.showMessageDialog(
                            mainPanel,
                            ex.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }


                if (savedPerson != null) {
                    setupPersonDetailsForm(savedPerson);
                    cardLayout.show(DisplayPanel, "5");
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "Error saving person.", "Error", JOptionPane.ERROR_MESSAGE);
                    BackToCameraButton.setVisible(false);
                    CapturePhotoButton.setVisible(true);
                    HomeButton.setVisible(true);
                    ViewContactsButton.setVisible(true);
                    cardLayout.show(DisplayPanel, "1");
                }
                PersonNameField.setText("");
                PersonRelationshipField.setText("");
            }
        });

        ADDMEETINGNOTESButton.addActionListener(e -> {
            // ... (Keep your existing Meeting Notes logic) ...
            isEditingMeetingNotes = !isEditingMeetingNotes;
            if(isEditingMeetingNotes){
                ADDMEETINGNOTESButton.setText("SAVE MEETING NOTES");
                MeetingNotesTextAreaScrollPane.setVisible(true);
                MeetingNotesTextArea.setVisible(true);
                MeetingNotesTextArea.setForeground(Color.GRAY);
                for(JTextArea noteArea : meetingNoteAreas){
                    noteArea.setEditable(true);
                    noteArea.setFocusable(true);
                    noteArea.setBackground(Color.WHITE);
                    noteArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                }
            } else {
                // Save logic
                boolean newNoteAdded = false;
                if (!meetingNoteAreas.isEmpty()) { saveEditedNotesToFile(newNoteAdded); }
                String noteText = MeetingNotesTextArea.getText().trim();
                if (currentDisplayedPerson != null && !noteText.isEmpty() && !noteText.equals("Add meeting notes here...")) {
                    try {
                        MeetingRecord record = currentDisplayedPerson.newConversation(noteText);
                        record.createFile();
                        personManager.updatePersonDetails(currentDisplayedPerson);
                        if (meetingNoteAreas.isEmpty()) {
                            String msg = "<html><body>New meeting note saved successfully!</body></html>";
                            msgLabel.setText(msg);

                            JOptionPane.showMessageDialog(mainPanel, msgLabel, "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        String msg = "<html><body>" + "Error: " + ex.getMessage() + "</body></html>";
                        msgLabel.setText(msg);

                        JOptionPane.showMessageDialog(mainPanel, msgLabel, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                MeetingNotesTextArea.setText("Add meeting notes here...");
                MeetingNotesTextArea.setForeground(Color.GRAY);
                MeetingNotesTextArea.setVisible(false);
                MeetingNotesTextAreaScrollPane.setVisible(false);
                ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");
                setupPersonDetailsForm(currentDisplayedPerson);
            }
        });

        MeetingNotesTextArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (MeetingNotesTextArea.getText().equals("Add meeting notes here...")) {
                        MeetingNotesTextArea.setText(""); MeetingNotesTextArea.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (MeetingNotesTextArea.getText().isEmpty()) {
                    MeetingNotesTextArea.setForeground(Color.GRAY); MeetingNotesTextArea.setText("Add meeting notes here...");
                }
            }
        });

        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainPanel.requestFocusInWindow();
            }
        });

        EDITCONTACTButton.addActionListener(e -> {
            if (currentDisplayedPerson != null) {
                showEditDetailsDialog(currentDisplayedPerson);
            } else {
                String msg = "<html><body>No person selected for editing.</body></html>";
                msgLabel.setText(msg);
                JOptionPane.showMessageDialog(mainPanel, "No person selected for editing.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void setupStartScreen() {
        // 1. Initialize Panel
        if (StartScreenPanel == null) {
            StartScreenPanel = new AnimatedBackgroundPanel();
            StartScreenPanel.setLayout(new GridBagLayout());
        }
        StartScreenPanel.removeAll();
        StartScreenPanel.setLayout(new GridBagLayout()); // GridBag is best for centering
        StartScreenPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Each item gets its own row
        gbc.anchor = GridBagConstraints.CENTER;       // FORCE CENTER ALIGNMENT
        gbc.fill = GridBagConstraints.NONE;           // Do not stretch to fill screen width

        // 2. Title: "IMentia"
        // HTML is used to force the size, 'text-align: center' ensures it doesn't align left
        JLabel titleLabel = new JLabel("<html><div style='text-align: center;'><span style='font-size:80px; font-weight:normal;'>IMentia</span></div></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.insets = new Insets(34, 0, 15, 0); // Gap below title
        StartScreenPanel.add(titleLabel, gbc);

        // 3. Subtitle
        JLabel subtitleLabel = new JLabel("Every. Familiar. Face. Matters.");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // *** SPACER: Increased to 250 to push the button MUCH lower ***
        gbc.insets = new Insets(0, 0, 230, 0);
        StartScreenPanel.add(subtitleLabel, gbc);

        // 4. Start Button
        JButton startButton = new JButton("START");
        startButton.setFont(buttonFont);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(180, 40));

        startButton.addActionListener(e -> {
            // Navigate to Camera
            cardLayout.show(DisplayPanel, "1");
            ButtonPanel.setVisible(true);

            // --- FIX: RESET BUTTON STATES FOR CAMERA MODE ---
            // We must explicitly show the camera buttons and hide the tutorial buttons
            if(HomeButton != null) HomeButton.setVisible(true);
            CapturePhotoButton.setVisible(true);
            ViewContactsButton.setVisible(true);
            cameraManager.startCamera();

            // Hide the "Back" button and reset its text
            BackToCameraButton.setVisible(false);
            BackToCameraButton.setText("BACK TO CAMERA");
            // ------------------------------------------------
        });

        // Gap between button and tutorial link
        gbc.insets = new Insets(0, 0, 30, 0);
        StartScreenPanel.add(startButton, gbc);

        // 5. Tutorial Link
        JLabel helpLabel = new JLabel("Don't know how to use IMentia?");
        helpLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        helpLabel.setForeground(Color.blue);
        helpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        helpLabel.setHorizontalAlignment(SwingConstants.CENTER);

        helpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Navigate to Tutorial
                cardLayout.show(DisplayPanel, "4");
                ButtonPanel.setVisible(true); // Show the button bar

                // Hide unnecessary buttons
                CapturePhotoButton.setVisible(false);
                ViewContactsButton.setVisible(false);
                if(HomeButton != null) HomeButton.setVisible(false);

                // Configure the Back Button for "Home" navigation
                BackToCameraButton.setVisible(true);
                BackToCameraButton.setText("BACK TO HOME SCREEN"); // <--- CHANGED TEXT
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                helpLabel.setText("<html><u>Don't know how to use IMentia?</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                helpLabel.setText("Don't know how to use IMentia?");
            }
        });


        // Reset insets
        gbc.insets = new Insets(0, 0, 0, 0);
        StartScreenPanel.add(helpLabel, gbc);

        // Force UI update
        StartScreenPanel.revalidate();
        StartScreenPanel.repaint();
    }

    protected void setupPersonDetailsForm(Person p){
        // Store reference to current person being displayed
        currentDisplayedPerson = p;
        personManager.ensureImageLoaded(p);

        if (p.getPersonImage() != null) {
            Image scaledImage = p.getPersonImage().getScaledInstance(200, 200, Image.SCALE_FAST);
            PersonDetailsImageLabel.setIcon(new ImageIcon(scaledImage));
        } else {
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
            MeetingRecord reader = new MeetingRecord(person, "");
            List<String> allNotesBlocks = reader.readAllNotes();

            if (allNotesBlocks.isEmpty()) {
                JLabel noNotesLabel = new JLabel("No meeting notes yet.");
                noNotesLabel.setFont(PLabelFont);
                noNotesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                notesPanel.add(noNotesLabel);
            } else {
                for (String fullBlock : allNotesBlocks) {
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
                errorLabel.setFont(PLabelFont);
                JOptionPane.showMessageDialog(mainPanel, errorLabel, "Error", JOptionPane.ERROR_MESSAGE);
                showEditDetailsDialog(person);
                return;
            }

            // *** FACADE USAGE: Update Person ***
            person.setName(FileHandler.capitalizeLabel(newName));
            person.setRelationship(newRel);

            personManager.updatePersonDetails(person);

            String msg = "<html><body>Contact details  updated successfully!</body><html>";
            msgLabel.setText(msg);
            JOptionPane.showMessageDialog(mainPanel, msgLabel, "Success", JOptionPane.INFORMATION_MESSAGE);
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

    /**
     * Constructs the UI component tree in code. Generated from MainPanel.form
     * (same components, hierarchy, texts and constraints) using only standard
     * Swing layouts, so any compiler (IntelliJ or plain javac/Maven) works.
     * Runtime setup in setUpUI() then adjusts layouts, fonts and listeners.
     */
    private void buildFormUI() {
        mainPanel = new JPanel(new GridBagLayout());
        DisplayPanel = new JPanel(new CardLayout(0, 0));
        DisplayPanel.setBackground(new Color(-15728877));
        mainPanel.add(DisplayPanel, new GridBagConstraints(0, 0, 3, 1, 0.5, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        CameraPanel = new JPanel(new BorderLayout(0, 0));
        CameraPanel.setBackground(new Color(-1118482));
        DisplayPanel.add(CameraPanel);
        ContactsPanel = new JPanel(new GridBagLayout());
        ContactsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ContactsPanel.setBackground(new Color(-1642241));
        DisplayPanel.add(ContactsPanel);
        JLabel listOfContactsLabel = new JLabel();
        listOfContactsLabel.setText("List of Contacts");
        ContactsPanel.add(listOfContactsLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        EditContactButton = new JButton();
        EditContactButton.setText("EDIT LIST");
        ContactsPanel.add(EditContactButton, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        ContactsScrollPane = new JScrollPane();
        ContactsPanel.add(ContactsScrollPane, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        PersonPanel = new JPanel(new GridBagLayout());
        PersonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ContactsScrollPane.setViewportView(PersonPanel);
        TutorialPanel = new JPanel(new GridBagLayout());
        DisplayPanel.add(TutorialPanel);
        PersonFormPanel = new JPanel(new GridBagLayout());
        PersonFormPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        PersonFormPanel.setBackground(new Color(-1118482));
        PersonFormPanel.setForeground(new Color(-1118482));
        DisplayPanel.add(PersonFormPanel);
        PersonImageLabel = new JLabel();
        PersonImageLabel.setText("img");
        PersonImageLabel.setMinimumSize(new Dimension(200, 200));
        PersonImageLabel.setPreferredSize(new Dimension(200, 200));
        PersonImageLabel.setMaximumSize(new Dimension(200, 200));
        PersonFormPanel.add(PersonImageLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        SavePersonInfoButton = new JButton();
        SavePersonInfoButton.setText("SAVE CONTACT");
        PersonFormPanel.add(SavePersonInfoButton, new GridBagConstraints(0, 3, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        NamePanel = new JPanel(new GridBagLayout());
        PersonFormPanel.add(NamePanel, new GridBagConstraints(0, 1, 2, 1, 0.5, 0.5, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        PersonNameLabel = new JLabel();
        PersonNameLabel.setText("Enter Person's Name:");
        PersonNameLabel.setMinimumSize(new Dimension(250, PersonNameLabel.getMinimumSize().height));
        PersonNameLabel.setPreferredSize(new Dimension(250, PersonNameLabel.getMinimumSize().height));
        PersonNameLabel.setMaximumSize(new Dimension(250, PersonNameLabel.getMinimumSize().height));
        NamePanel.add(PersonNameLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        PersonNameField = new JTextField();
        PersonNameField.setHorizontalAlignment(10);
        PersonNameField.setMinimumSize(new Dimension(300, PersonNameField.getMinimumSize().height));
        PersonNameField.setPreferredSize(new Dimension(300, PersonNameField.getMinimumSize().height));
        PersonNameField.setMaximumSize(new Dimension(300, PersonNameField.getMinimumSize().height));
        NamePanel.add(PersonNameField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        RelationshipPanel = new JPanel(new GridBagLayout());
        PersonFormPanel.add(RelationshipPanel, new GridBagConstraints(0, 2, 2, 1, 0.5, 0.5, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        PersonRelationshipLabel = new JLabel();
        PersonRelationshipLabel.setText("Enter Relationship:");
        PersonRelationshipLabel.setMinimumSize(new Dimension(250, PersonRelationshipLabel.getMinimumSize().height));
        PersonRelationshipLabel.setPreferredSize(new Dimension(250, PersonRelationshipLabel.getMinimumSize().height));
        PersonRelationshipLabel.setMaximumSize(new Dimension(250, PersonRelationshipLabel.getMinimumSize().height));
        RelationshipPanel.add(PersonRelationshipLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        PersonRelationshipField = new JTextField();
        PersonRelationshipField.setHorizontalAlignment(10);
        PersonRelationshipField.setMinimumSize(new Dimension(300, PersonRelationshipField.getMinimumSize().height));
        PersonRelationshipField.setPreferredSize(new Dimension(300, PersonRelationshipField.getMinimumSize().height));
        PersonRelationshipField.setMaximumSize(new Dimension(300, PersonRelationshipField.getMinimumSize().height));
        RelationshipPanel.add(PersonRelationshipField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        PersonDetailsForm = new JPanel(new GridBagLayout());
        DisplayPanel.add(PersonDetailsForm);
        PersonDetailsTopSection = new JPanel(new GridBagLayout());
        PersonDetailsForm.add(PersonDetailsTopSection, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
        PersonDetailsImageLabel = new JLabel();
        PersonDetailsImageLabel.setBackground(new Color(-1642448));
        PersonDetailsImageLabel.setForeground(new Color(-8133279));
        PersonDetailsImageLabel.setMaximumSize(new Dimension(200, 200));
        PersonDetailsImageLabel.setMinimumSize(new Dimension(200, 200));
        PersonDetailsImageLabel.setPreferredSize(new Dimension(200, 200));
        PersonDetailsImageLabel.setText("img");
        PersonDetailsTopSection.add(PersonDetailsImageLabel, new GridBagConstraints(0, 0, 1, 5, 0.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(20, 20, 0, 0), 0, 0));
        PersonDetailNameLabel = new JLabel();
        PersonDetailNameLabel.setText("Person Name:");
        PersonDetailsTopSection.add(PersonDetailNameLabel, new GridBagConstraints(1, 0, 1, 4, 0.01, 0.5, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 20, 5, 0), 0, 0));
        PersonDetailRelLabel = new JLabel();
        PersonDetailRelLabel.setText("Your Relation:");
        PersonDetailsTopSection.add(PersonDetailRelLabel, new GridBagConstraints(1, 4, 2, 1, 0.01, 0.5, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        PersonDetailPersonName = new JLabel();
        PersonDetailPersonName.setText("Label");
        PersonDetailsTopSection.add(PersonDetailPersonName, new GridBagConstraints(2, 3, 2, 1, 0.3, 0.5, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
        PersonDetailPersonRel = new JLabel();
        PersonDetailPersonRel.setText("Label");
        PersonDetailsTopSection.add(PersonDetailPersonRel, new GridBagConstraints(3, 4, 2, 1, 0.3, 0.5, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        EDITCONTACTButton = new JButton();
        EDITCONTACTButton.setText("EDIT CONTACT");
        PersonDetailsTopSection.add(EDITCONTACTButton, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        SAVEEDITButton = new JButton();
        SAVEEDITButton.setText("SAVE EDIT");
        SAVEEDITButton.setVisible(false);
        PersonDetailsTopSection.add(SAVEEDITButton, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        CANCELEDITButton = new JButton();
        CANCELEDITButton.setText("CANCEL EDIT");
        CANCELEDITButton.setVisible(false);
        PersonDetailsTopSection.add(CANCELEDITButton, new GridBagConstraints(6, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        PersonNameEdit = new JTextField();
        PersonNameEdit.setVisible(false);
        PersonNameEdit.setPreferredSize(new Dimension(150, PersonNameEdit.getMinimumSize().height));
        PersonDetailsTopSection.add(PersonNameEdit, new GridBagConstraints(4, 3, 2, 1, 0.3, 0.5, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0));
        PersonRelEdit = new JTextField();
        PersonRelEdit.setVisible(false);
        PersonRelEdit.setPreferredSize(new Dimension(150, PersonRelEdit.getMinimumSize().height));
        PersonDetailsTopSection.add(PersonRelEdit, new GridBagConstraints(5, 4, 1, 1, 0.3, 0.5, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        PersonDetailsBottomSection = new JPanel(new GridBagLayout());
        PersonDetailsForm.add(PersonDetailsBottomSection, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.8, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 10, 10), 0, 0));
        ADDMEETINGNOTESButton = new JButton();
        ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");
        PersonDetailsBottomSection.add(ADDMEETINGNOTESButton, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.1, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
        MeetingNotesLabel = new JLabel();
        MeetingNotesLabel.setText("Meeting Notes");
        PersonDetailsBottomSection.add(MeetingNotesLabel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.1, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
        MeetingNotesScrollPane = new JScrollPane();
        MeetingNotesScrollPane.setAutoscrolls(true);
        PersonDetailsBottomSection.add(MeetingNotesScrollPane, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.4, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 10), 0, 0));
        MeetingNotesTextAreaScrollPane = new JScrollPane();
        MeetingNotesTextAreaScrollPane.setVisible(true);
        PersonDetailsBottomSection.add(MeetingNotesTextAreaScrollPane, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0));
        MeetingNotesTextArea = new JTextArea();
        MeetingNotesTextArea.setForeground(new Color(-1776412));
        MeetingNotesTextArea.setLineWrap(true);
        MeetingNotesTextArea.setMargin(new Insets(10, 10, 10, 10));
        MeetingNotesTextArea.setText("");
        MeetingNotesTextAreaScrollPane.setViewportView(MeetingNotesTextArea);
        ButtonPanel = new JPanel(new GridBagLayout());
        ButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        ButtonPanel.setBackground(new Color(-2565928));
        mainPanel.add(ButtonPanel, new GridBagConstraints(0, 1, 3, 1, 0.5, 0.5, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        CapturePhotoButton = new JButton();
        CapturePhotoButton.setText("CAPTURE PHOTO");
        ButtonPanel.add(CapturePhotoButton, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        HomeButton = new JButton();
        HomeButton.setText("HOME SCREEN");
        ButtonPanel.add(HomeButton, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        ViewContactsButton = new JButton();
        ViewContactsButton.setText("VIEW CONTACTS");
        ButtonPanel.add(ViewContactsButton, new GridBagConstraints(3, 0, 1, 1, 0.5, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        BackToCameraButton = new JButton();
        BackToCameraButton.setText("BACK TO CAMERA");
        BackToCameraButton.setVisible(false);
        ButtonPanel.add(BackToCameraButton, new GridBagConstraints(2, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }

    public JPanel getPanel(){
        return mainPanel;
    }

    protected void toggleDeleteButton(){
        for(JPanel panel : contactListPanels){
            for(Component c : panel.getComponents()){
                if(c instanceof JButton b){
                    b.setVisible(!b.isVisible());
                }
            }
        }
    }

    /** Face samples captured per new person for multi-sample training. */
    private static final int BURST_SAMPLES = 5;
    /** Delay between burst samples so expressions and angles vary slightly. */
    private static final int BURST_DELAY_MS = 250;

    /**
     * Captures up to BURST_SAMPLES face crops about BURST_DELAY_MS apart
     * while a modal progress dialog keeps the UI responsive. Returns
     * whatever was captured (possibly fewer than requested, never null).
     */
    private List<Mat> captureBurst() {
        JDialog progress = new JDialog(SwingUtilities.getWindowAncestor(mainPanel),
                "Capturing...", Dialog.ModalityType.APPLICATION_MODAL);
        JProgressBar bar = new JProgressBar(0, BURST_SAMPLES);
        bar.setStringPainted(true);
        bar.setString("Stay still... 0/" + BURST_SAMPLES);
        progress.add(bar);
        progress.setSize(320, 80);
        progress.setLocationRelativeTo(mainPanel);

        SwingWorker<List<Mat>, Integer> worker = new SwingWorker<>() {
            @Override
            protected List<Mat> doInBackground() {
                List<Mat> samples = new ArrayList<>();
                for (int i = 0; i < BURST_SAMPLES && !isCancelled(); i++) {
                    Rect r = videoProcessor.getClampedFaceRect();
                    Mat f = videoProcessor.getCurrentFrame();
                    if (r != null && f != null && !f.empty()) {
                        samples.add(new Mat(f, r));
                    }
                    publish(samples.size());
                    try {
                        Thread.sleep(BURST_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return samples;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int n = chunks.get(chunks.size() - 1);
                bar.setValue(n);
                bar.setString("Stay still... " + n + "/" + BURST_SAMPLES);
            }

            @Override
            protected void done() {
                progress.dispose();
            }
        };
        worker.execute();
        progress.setVisible(true); // blocks until the worker disposes the dialog
        try {
            return worker.get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // UI Logic
    protected boolean captureFace() {
        System.out.println("=== captureFace() called ===");
        List<Mat> burst = captureBurst();

        if (burst.isEmpty()) {
            System.out.println("No face detected in current frame");
            JLabel errorLabel = new JLabel("No face detected! Please look at the camera.");
            errorLabel.setFont(PLabelFont);

            JOptionPane.showMessageDialog(mainPanel, errorLabel, "No Face", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        faceImages = burst;
        System.out.println("Captured " + burst.size() + " face sample(s), recognizing first...");

        // *** FACADE USAGE: Recognize ***
        FaceRecognitionService.RecognitionResult result = personManager.recognizeFace(faceImages.get(0));

        if(result.isRecognized()){
            // ...
            System.out.println("Person recognized: " + result.getPerson().getId());
            setupPersonDetailsForm(result.getPerson());
            cameraManager.stopCamera();
            CapturePhotoButton.setVisible(false);
            BackToCameraButton.setVisible(true);
            HomeButton.setVisible(false); // Hide Home
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
            successLabel.setFont(PLabelFont);
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
        noteArea.setSize(new Dimension(parent.getWidth(), 9999));
        Dimension preferredSize = noteArea.getPreferredSize();

        noteArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));

        parent.add(noteArea);

        meetingNoteAreas.add(noteArea);
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
            String[] lines = editedText.split("\n", 4);

            StringBuilder noteBlock = new StringBuilder();
            noteBlock.append("----- NOTE START -----\n");

            // Append header lines (Date and Time, assuming they are the first two lines)
            if (lines.length > 0) noteBlock.append(lines[0].trim()).append("\n");
            if (lines.length > 1) noteBlock.append(lines[1].trim()).append("\n");
            noteBlock.append("\n"); // Blank line after time

            if (lines.length > 2) {
                StringBuilder contentBody = new StringBuilder();
                for (int i = 2; i < lines.length; i++) {
                    contentBody.append(lines[i]).append("\n");
                }
                noteBlock.append(contentBody.toString().trim()).append("\n");

            } else if (lines.length == 1) {
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
            } else {
                imageLabel.setText("No Image");
            }
        } catch (Exception e) {
            imageLabel.setText("Error");
        }

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
            deleteContact(person);
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE; // Fill both width and height
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 0); // Reset padding
        panel.add(infoPanel, gbc);

        gbc.gridx = 2; // Column 2
        gbc.weightx = 0; // Do not stretch width
        gbc.fill = GridBagConstraints.NONE; // Do not resize the button
        gbc.anchor = GridBagConstraints.EAST; // PIN TO TOP-RIGHT
        panel.add(deleteButton, gbc);

        contactListPanels.add(panel);
        return panel;
    }

    public void refreshContactsPanel() {
        PersonPanel.removeAll();
        contactListPanels.clear();

        // *** FACADE USAGE: Get Data ***
        List<Person> persons = personManager.getAllPersons();

        // Comparator for sorting. Person details bug fixed already, please do not modify.
        persons.sort(new Comparator<Person>(){
            public int compare(Person p1, Person p2){
                return p1.getName().compareToIgnoreCase(p2.getName());
            }
        });

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

        PersonPanel.revalidate();
        PersonPanel.repaint();

        if (isEditing) {
            toggleDeleteButton();
        }
    }

}