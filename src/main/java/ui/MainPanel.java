package ui;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import people.Person;
import service.FaceRecognitionService;
import ui.listeners.ContactSelectionListener;
import ui.panels.ContactListView;
import ui.panels.PersonDetailsView;
import ui.panels.TutorialView;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;

// FIX: Removed 'extends JPanel'. This class is now a controller for the form.
public class MainPanel implements ContactSelectionListener {

    // =================================================================
    // START: FIELDS TO SATISFY .FORM BINDINGS
    // =================================================================

    private JPanel mainPanel; // The root panel bound by the form
    private JPanel CameraPanel;
    private JPanel ContactsPanel;
    private JPanel TutorialPanel;
    private JPanel PersonFormPanel;
    private JPanel PersonDetailsForm;
    private JPanel DisplayPanel;
    private JPanel ButtonPanel;
    // Unused panel references kept for binding safety
    private JPanel NamePanel;
    private JPanel RelationshipPanel;
    private JPanel PersonDetailsTopSection;
    private JPanel PersonDetailsBottomSection;

    private JButton CapturePhotoButton;
    private JButton ViewContactsButton;
    private JButton TutorialButton;
    private JButton EditContactButton;
    private JButton BackToCameraButton;
    private JButton SavePersonInfoButton;
    private JButton ADDMEETINGNOTESButton;
    private JButton EDITCONTACTButton;
    private JButton SAVEEDITButton;
    private JButton CANCELEDITButton;

    private JLabel PersonNameLabel;
    private JLabel PersonImageLabel;
    private JLabel PersonRelationshipLabel;
    private JLabel MeetingNotesLabel;
    private JLabel PersonDetailPersonName;
    private JLabel PersonDetailPersonRel;
    private JLabel PersonDetailNameLabel;
    private JLabel PersonDetailRelLabel;
    private JLabel PersonDetailsImageLabel;
    private JTextField PersonNameField;
    private JTextField PersonRelationshipField;
    private JTextField PersonNameEdit;
    private JTextField PersonRelEdit;
    private JScrollPane ContactsScrollPane;
    private JScrollPane MeetingNotesScrollPane;
    private JScrollPane MeetingNotesTextAreaScrollPane;
    private JTextArea MeetingNotesTextArea;
    private JPanel PersonPanel;

    // =================================================================
    // END: FIELDS TO SATISFY .FORM BINDINGS
    // =================================================================

    // --- NEW MODULAR VIEWS & SERVICES ---
    private ContactListView contactListView;
    private PersonDetailsView personDetailsView;
    private VideoProcessor videoProcessor;

    private PersonDataManager personManager;
    private ImageHandler imageHandler;
    private MeetingNotesHandler notesHandler;
    private FaceRecognitionService recognitionService;
    private List<Person> persons;

    // --- STATE & UTILS ---
    private CardLayout cardLayout = new CardLayout();
    private Person currentDisplayedPerson;
    private Mat currentFaceImage;
    private boolean isEditing = false;

    private final Font buttonFont = AppTheme.BUTTON_FONT;
    private final Font HLabelFont = AppTheme.H_LABEL_FONT;
    private final Font PLabelFont = AppTheme.P_LABEL_FONT;

    public MainPanel() {
        // 1. Initialize Managers & Load Data
        personManager = new PersonDataManager();
        imageHandler = new ImageHandler();
        notesHandler = new MeetingNotesHandler();

        persons = personManager.loadPersonFile();
        recognitionService = new FaceRecognitionService();
        recognitionService.train(persons);

        // 2. Initialize Modular Views
        setupModularViews();

        // 3. Configure UI
        setUpUI();

        // 4. Validate Root Panel
        if (mainPanel == null) {
            System.err.println("ERROR: Form binding failed. 'mainPanel' is null.");
            // If this prints, ensure IntelliJ Settings > Editor > GUI Designer
            // is set to 'Generate GUI into: Java source code' OR try 'Binary class files'.
            // Also try Build > Rebuild Project.
        } else {
            // Force layout update
            mainPanel.revalidate();
            mainPanel.repaint();
        }

        // 5. Add Component Listener to control camera lifecycle
        if (CameraPanel != null) {
            CameraPanel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    if (videoProcessor != null && !videoProcessor.isRunning()) {
                        videoProcessor.startCamera();
                    }
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    if (videoProcessor != null) {
                        videoProcessor.stopCamera();
                    }
                }
            });
        }
    }

    private void setupModularViews() {
        contactListView = new ContactListView(imageHandler, this);

        personDetailsView = new PersonDetailsView(imageHandler, notesHandler, () -> {
            personManager.savePersons(persons);
            recognitionService.train(persons);
            if (currentDisplayedPerson != null) {
                // Width check: use mainPanel width if available, else generic 800
                int width = (mainPanel != null) ? mainPanel.getWidth() : 800;
                contactListView.displayContacts(persons, width);
                personDetailsView.setPerson(currentDisplayedPerson);
            }
        });

        videoProcessor = new VideoProcessor();
    }

    private void setUpUI() {
        if (mainPanel == null) return; // Guard against binding failure

        // Setup Modular Views inside the form containers
        CameraPanel.setLayout(new BorderLayout());
        CameraPanel.add(videoProcessor, BorderLayout.CENTER);

        PersonPanel.setLayout(new BoxLayout(PersonPanel, BoxLayout.Y_AXIS));

        PersonDetailsForm.setLayout(new BorderLayout());
        PersonDetailsForm.removeAll();
        PersonDetailsForm.add(personDetailsView, BorderLayout.CENTER);

        TutorialPanel.setLayout(new BorderLayout());
        TutorialPanel.removeAll();
        TutorialPanel.add(new TutorialView(), BorderLayout.CENTER);

        setButtonFont(mainPanel);
        setPLabelFont(mainPanel);

        DisplayPanel.setLayout(cardLayout);
        cardLayout.show(DisplayPanel, "36e68");

        BackToCameraButton.setVisible(false);
        SAVEEDITButton.setVisible(false);
        CANCELEDITButton.setVisible(false);
        PersonNameEdit.setVisible(false);
        PersonRelEdit.setVisible(false);
        ADDMEETINGNOTESButton.setVisible(false);

        // --- LISTENERS ---
        ViewContactsButton.addActionListener(e -> {
            persons = personManager.loadPersonFile();
            contactListView.displayContacts(persons, mainPanel.getWidth());
            cardLayout.show(DisplayPanel, "5891");
            updateButtonVisibility(false, true);
        });

        TutorialButton.addActionListener(e -> {
            cardLayout.show(DisplayPanel, "7a3bc");
            updateButtonVisibility(false, false);
        });

        BackToCameraButton.addActionListener(e -> {
            cardLayout.show(DisplayPanel, "36e68");
            updateButtonVisibility(true, false);
        });

        CapturePhotoButton.addActionListener(e -> handleCapture());

        EditContactButton.addActionListener(e -> {
            isEditing = !isEditing;
            contactListView.setEditMode(isEditing);
            EditContactButton.setText(isEditing ? "DONE EDIT" : "EDIT LIST");
        });

        EDITCONTACTButton.addActionListener(e -> {
            if (currentDisplayedPerson != null) {
                personDetailsView.showEditDialog(currentDisplayedPerson);
            }
        });

        SavePersonInfoButton.addActionListener(e -> {
            String name = PersonNameField.getText().trim();
            String rel = PersonRelationshipField.getText().trim();

            if (name.isEmpty() || rel.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "Name and Relationship are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Person p = new Person(FileHandler.capitalizeLabel(name), rel);
            p.setId(personManager.generateId(persons));

            imageHandler.saveFaceImage(p.getId(), currentFaceImage);
            persons.add(p);
            personManager.savePersons(persons);
            recognitionService.train(persons);

            setupPersonDetailsView(p);
            cardLayout.show(DisplayPanel, "2d60a");
        });
    }

    private void handleCapture() {
        Rect faceRect = videoProcessor.getCurrentFaceRect();
        Mat frame = videoProcessor.getCurrentFrame();

        if (frame == null || faceRect == null) {
            JOptionPane.showMessageDialog(mainPanel, "No face detected!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentFaceImage = new Mat(frame, faceRect);
        var result = recognitionService.recognize(currentFaceImage);

        if (result.isRecognized()) {
            setupPersonDetailsView(result.getPerson());
            cardLayout.show(DisplayPanel, "2d60a");
            updateButtonVisibility(false, false);
        } else {
            int choice = JOptionPane.showConfirmDialog(mainPanel,
                    "Person not recognized. Would you like to add them?",
                    "Unknown Person",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                showAddPersonDialog();
            }
        }
    }

    private void showAddPersonDialog() {
        cardLayout.show(DisplayPanel, "a68b6");
        updateButtonVisibility(false, false);

        BufferedImage bufferedImage = ImageUtils.matToBufferedImage(currentFaceImage);
        Image scaledImage = bufferedImage.getScaledInstance(200, 200, Image.SCALE_FAST);
        PersonImageLabel.setIcon(new ImageIcon(scaledImage));
        PersonNameField.setText("");
        PersonRelationshipField.setText("");
    }

    private void setupPersonDetailsView(Person p) {
        currentDisplayedPerson = p;
        personDetailsView.setPerson(p);
        EDITCONTACTButton.setVisible(true);
    }

    private void updateButtonVisibility(boolean isCameraView, boolean isContactsListView) {
        CapturePhotoButton.setVisible(isCameraView);
        ViewContactsButton.setVisible(isCameraView);
        TutorialButton.setVisible(isCameraView);
        BackToCameraButton.setVisible(!isCameraView);
        EditContactButton.setVisible(isContactsListView);
        EDITCONTACTButton.setVisible(!isCameraView && !isContactsListView);
    }

    @Override
    public void onContactSelected(Person person) {
        setupPersonDetailsView(person);
        cardLayout.show(DisplayPanel, "2d60a");
        updateButtonVisibility(false, false);
    }

    @Override
    public void onDeleteContact(Person person) {
        int confirm = JOptionPane.showConfirmDialog(mainPanel, "Are you sure you want to delete " + person.getName() + "?");
        if (confirm == JOptionPane.YES_OPTION) {
            persons.remove(person);
            personManager.savePersons(persons);
            imageHandler.deleteFaceImage(person.getId());
            recognitionService.train(persons);
            contactListView.displayContacts(persons, mainPanel.getWidth());
            JOptionPane.showMessageDialog(mainPanel, "Contact Deleted.");
        }
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    void setButtonFont(Container container) {
        if (container == null) return;
        for (Component c1 : container.getComponents()) {
            if (c1 instanceof JButton b) b.setFont(buttonFont);
            if (c1 instanceof Container c2) setButtonFont(c2);
        }
    }

    void setPLabelFont(Container container) {
        if (container == null) return;
        for (Component c1 : container.getComponents()) {
            if (c1 instanceof JLabel || c1 instanceof JTextField || c1 instanceof JTextArea) c1.setFont(PLabelFont);
            if (c1 instanceof Container c2) setPLabelFont(c2);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        DisplayPanel = new JPanel();
        DisplayPanel.setLayout(new CardLayout(0, 0));
        DisplayPanel.setBackground(new Color(-15728877));
        panel1.add(DisplayPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        CameraPanel = new JPanel();
        CameraPanel.setLayout(new BorderLayout(0, 0));
        CameraPanel.setBackground(new Color(-1118482));
        DisplayPanel.add(CameraPanel, "Card1");
        ContactsPanel = new JPanel();
        ContactsPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        ContactsPanel.setBackground(new Color(-1642241));
        DisplayPanel.add(ContactsPanel, "Card2");
        final JLabel label1 = new JLabel();
        label1.setText("List of Contacts");
        ContactsPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        EditContactButton = new JButton();
        EditContactButton.setText("EDIT LIST");
        ContactsPanel.add(EditContactButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ContactsScrollPane = new JScrollPane();
        ContactsPanel.add(ContactsScrollPane, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        PersonPanel = new JPanel();
        PersonPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        ContactsScrollPane.setViewportView(PersonPanel);
        TutorialPanel = new JPanel();
        TutorialPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        DisplayPanel.add(TutorialPanel, "Card3");
        PersonFormPanel = new JPanel();
        PersonFormPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(20, 0, 20, 0), -1, -1));
        PersonFormPanel.setBackground(new Color(-1118482));
        PersonFormPanel.setForeground(new Color(-1118482));
        DisplayPanel.add(PersonFormPanel, "Card4");
        PersonImageLabel = new JLabel();
        PersonImageLabel.setText("img");
        PersonFormPanel.add(PersonImageLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(200, 200), new Dimension(200, 200), new Dimension(200, 200), 0, false));
        SavePersonInfoButton = new JButton();
        SavePersonInfoButton.setText("SAVE CONTACT");
        PersonFormPanel.add(SavePersonInfoButton, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        NamePanel = new JPanel();
        NamePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        PersonFormPanel.add(NamePanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_SOUTH, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        PersonNameLabel = new JLabel();
        PersonNameLabel.setText("Enter Person's Name:");
        NamePanel.add(PersonNameLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(250, -1), new Dimension(250, -1), new Dimension(250, -1), 0, false));
        PersonNameField = new JTextField();
        PersonNameField.setHorizontalAlignment(10);
        NamePanel.add(PersonNameField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), new Dimension(300, -1), new Dimension(300, -1), 0, false));
        RelationshipPanel = new JPanel();
        RelationshipPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        PersonFormPanel.add(RelationshipPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        PersonRelationshipLabel = new JLabel();
        PersonRelationshipLabel.setText("Enter Relationship:");
        RelationshipPanel.add(PersonRelationshipLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(250, -1), new Dimension(250, -1), new Dimension(250, -1), 0, false));
        PersonRelationshipField = new JTextField();
        PersonRelationshipField.setHorizontalAlignment(10);
        RelationshipPanel.add(PersonRelationshipField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), new Dimension(300, -1), new Dimension(300, -1), 0, false));
        PersonDetailsForm = new JPanel();
        PersonDetailsForm.setLayout(new GridBagLayout());
        DisplayPanel.add(PersonDetailsForm, "Card5");
        PersonDetailsTopSection = new JPanel();
        PersonDetailsTopSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 0, 10);
        PersonDetailsForm.add(PersonDetailsTopSection, gbc);
        PersonDetailsImageLabel = new JLabel();
        PersonDetailsImageLabel.setBackground(new Color(-1642448));
        PersonDetailsImageLabel.setForeground(new Color(-8133279));
        PersonDetailsImageLabel.setMaximumSize(new Dimension(200, 200));
        PersonDetailsImageLabel.setMinimumSize(new Dimension(200, 200));
        PersonDetailsImageLabel.setPreferredSize(new Dimension(200, 200));
        PersonDetailsImageLabel.setText("img");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 5;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(20, 20, 0, 0);
        PersonDetailsTopSection.add(PersonDetailsImageLabel, gbc);
        PersonDetailNameLabel = new JLabel();
        PersonDetailNameLabel.setText("Person Name:");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 4;
        gbc.weightx = 0.01;
        gbc.weighty = 0.5;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 20, 5, 0);
        PersonDetailsTopSection.add(PersonDetailNameLabel, gbc);
        PersonDetailRelLabel = new JLabel();
        PersonDetailRelLabel.setText("Your Relation:");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 0.01;
        gbc.weighty = 0.5;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 20, 0, 0);
        PersonDetailsTopSection.add(PersonDetailRelLabel, gbc);
        PersonDetailPersonName = new JLabel();
        PersonDetailPersonName.setText("Label");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 0.3;
        gbc.weighty = 0.5;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 0, 5, 0);
        PersonDetailsTopSection.add(PersonDetailPersonName, gbc);
        PersonDetailPersonRel = new JLabel();
        PersonDetailPersonRel.setText("Label");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 0.3;
        gbc.weighty = 0.5;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 0, 0, 0);
        PersonDetailsTopSection.add(PersonDetailPersonRel, gbc);
        EDITCONTACTButton = new JButton();
        EDITCONTACTButton.setText("EDIT CONTACT");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        PersonDetailsTopSection.add(EDITCONTACTButton, gbc);
        SAVEEDITButton = new JButton();
        SAVEEDITButton.setText("SAVE EDIT");
        SAVEEDITButton.setVisible(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        PersonDetailsTopSection.add(SAVEEDITButton, gbc);
        CANCELEDITButton = new JButton();
        CANCELEDITButton.setText("CANCEL EDIT");
        CANCELEDITButton.setVisible(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        PersonDetailsTopSection.add(CANCELEDITButton, gbc);
        PersonNameEdit = new JTextField();
        PersonNameEdit.setVisible(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 0.3;
        gbc.weighty = 0.5;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        PersonDetailsTopSection.add(PersonNameEdit, gbc);
        PersonRelEdit = new JTextField();
        PersonRelEdit.setVisible(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        gbc.weighty = 0.5;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 0, 0);
        PersonDetailsTopSection.add(PersonRelEdit, gbc);
        PersonDetailsBottomSection = new JPanel();
        PersonDetailsBottomSection.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 10, 10, 10);
        PersonDetailsForm.add(PersonDetailsBottomSection, gbc);
        ADDMEETINGNOTESButton = new JButton();
        ADDMEETINGNOTESButton.setText("ADD MEETING NOTES");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(0, 0, 10, 0);
        PersonDetailsBottomSection.add(ADDMEETINGNOTESButton, gbc);
        MeetingNotesLabel = new JLabel();
        MeetingNotesLabel.setText("Meeting Notes");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 10, 10, 0);
        PersonDetailsBottomSection.add(MeetingNotesLabel, gbc);
        MeetingNotesScrollPane = new JScrollPane();
        MeetingNotesScrollPane.setAutoscrolls(true);
        MeetingNotesScrollPane.setMaximumSize(new Dimension(18, 19));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 10, 0, 10);
        PersonDetailsBottomSection.add(MeetingNotesScrollPane, gbc);
        MeetingNotesTextAreaScrollPane = new JScrollPane();
        MeetingNotesTextAreaScrollPane.setVisible(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 0, 10);
        PersonDetailsBottomSection.add(MeetingNotesTextAreaScrollPane, gbc);
        MeetingNotesTextArea = new JTextArea();
        MeetingNotesTextArea.setForeground(new Color(-1776412));
        MeetingNotesTextArea.setLineWrap(true);
        MeetingNotesTextArea.setMargin(new Insets(10, 10, 10, 10));
        MeetingNotesTextArea.setMaximumSize(new Dimension(1, 17));
        MeetingNotesTextArea.setText("");
        MeetingNotesTextAreaScrollPane.setViewportView(MeetingNotesTextArea);
        ButtonPanel = new JPanel();
        ButtonPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 10, 10, 10), -1, -1));
        ButtonPanel.setBackground(new Color(-2565928));
        panel1.add(ButtonPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        CapturePhotoButton = new JButton();
        CapturePhotoButton.setText("CAPTURE PHOTO");
        ButtonPanel.add(CapturePhotoButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(10, 20), null, 0, false));
        TutorialButton = new JButton();
        TutorialButton.setText("TUTORIAL");
        ButtonPanel.add(TutorialButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ViewContactsButton = new JButton();
        ViewContactsButton.setText("VIEW CONTACTS");
        ButtonPanel.add(ViewContactsButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BackToCameraButton = new JButton();
        BackToCameraButton.setText("BACK TO CAMERA");
        BackToCameraButton.setVisible(false);
        ButtonPanel.add(BackToCameraButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
}