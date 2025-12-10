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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class MainPanel extends JPanel implements ContactSelectionListener {

    // Logic / Services
    private PersonDataManager personManager;
    private ImageHandler imageHandler;
    private MeetingNotesHandler notesHandler;
    private FaceRecognitionService recognitionService;
    private VideoProcessor videoProcessor;
    private List<Person> persons;

    // UI Components
    private CardLayout cardLayout = new CardLayout();
    private JPanel displayPanel; // Holds the cards
    private JPanel buttonPanel;  // Holds the navigation buttons

    // Views
    private TutorialView tutorialView;
    private ContactListView contactListView;
    private PersonDetailsView personDetailsView;
    private JPanel cameraWrapperPanel; // Wrapper for VideoProcessor

    // Buttons
    private JButton captureButton;
    private JButton viewContactsButton;
    private JButton tutorialButton;
    private JButton backButton;
    private JButton editListButton; // For toggling delete mode in list

    // State
    private Mat currentFaceImage; // Store captured face

    public MainPanel() {
        // 1. Initialize Managers
        personManager = new PersonDataManager();
        imageHandler = new ImageHandler();
        notesHandler = new MeetingNotesHandler();

        persons = personManager.loadPersonFile();
        recognitionService = new FaceRecognitionService();
        recognitionService.train(persons);

        setLayout(new BorderLayout());

        // 2. Setup Sub-Views
        setupViews();

        // 3. Setup Navigation/Buttons
        setupButtonPanel();

        // 4. Final Layout
        displayPanel = new JPanel(cardLayout);
        displayPanel.add(cameraWrapperPanel, "CAMERA");
        displayPanel.add(new JScrollPane(contactListView), "CONTACTS"); // Wrap list in ScrollPane
        displayPanel.add(personDetailsView, "DETAILS");
        displayPanel.add(tutorialView, "TUTORIAL");

        add(displayPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // 5. Start Camera
        videoProcessor.startCamera();

        // 6. Font Consistency Helper
        applyFonts(this);
    }

    private void setupViews() {
        // Camera View
        videoProcessor = new VideoProcessor();
        cameraWrapperPanel = new JPanel(new BorderLayout());
        cameraWrapperPanel.add(videoProcessor, BorderLayout.CENTER);

        // Tutorial
        tutorialView = new TutorialView();

        // Contact List
        contactListView = new ContactListView(imageHandler, this);

        // Person Details (Pass managers and a callback for when data is edited)
        personDetailsView = new PersonDetailsView(imageHandler, notesHandler, () -> {
            // Callback: When person is edited, save data and retrain
            personManager.savePersons(persons);
            recognitionService.train(persons);
        });
    }

    private void setupButtonPanel() {
        buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setPreferredSize(new Dimension(800, 100)); // Approx height

        captureButton = new JButton("CAPTURE PHOTO");
        viewContactsButton = new JButton("VIEW CONTACTS");
        tutorialButton = new JButton("TUTORIAL");
        backButton = new JButton("BACK TO CAMERA");
        editListButton = new JButton("EDIT LIST");

        // Action Listeners
        captureButton.addActionListener(e -> handleCapture());

        viewContactsButton.addActionListener(e -> {
            // Refresh list before showing
            contactListView.displayContacts(persons, getWidth());
            cardLayout.show(displayPanel, "CONTACTS");
            updateButtonVisibility(false);
            editListButton.setVisible(true);
        });

        tutorialButton.addActionListener(e -> {
            cardLayout.show(displayPanel, "TUTORIAL");
            updateButtonVisibility(false);
        });

        backButton.addActionListener(e -> {
            cardLayout.show(displayPanel, "CAMERA");
            updateButtonVisibility(true);
            contactListView.setEditMode(false); // Reset edit mode
            editListButton.setText("EDIT LIST");
        });

        editListButton.addActionListener(e -> {
            boolean currentMode = editListButton.getText().equals("DONE");
            if(currentMode) {
                editListButton.setText("EDIT LIST");
                contactListView.setEditMode(false);
            } else {
                editListButton.setText("DONE");
                contactListView.setEditMode(true);
            }
        });

        buttonPanel.add(captureButton);
        buttonPanel.add(viewContactsButton);
        buttonPanel.add(tutorialButton);
        buttonPanel.add(backButton);
        buttonPanel.add(editListButton);

        updateButtonVisibility(true); // Set initial state
    }

    private void updateButtonVisibility(boolean isCameraView) {
        captureButton.setVisible(isCameraView);
        viewContactsButton.setVisible(isCameraView);
        tutorialButton.setVisible(isCameraView);

        backButton.setVisible(!isCameraView);
        editListButton.setVisible(!isCameraView && displayPanel.getComponent(1).isVisible()); // Only show on List view logic

        // Specific fix: EditList button only relevant on contacts screen
        // We can handle this better by checking currently visible card,
        // but for now, we'll just toggle it in the action listeners above.
        if(isCameraView) editListButton.setVisible(false);
    }

    private void handleCapture() {
        Rect faceRect = videoProcessor.getCurrentFaceRect();
        Mat frame = videoProcessor.getCurrentFrame();

        if (frame == null || faceRect == null) {
            JOptionPane.showMessageDialog(this, "No face detected!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentFaceImage = new Mat(frame, faceRect);
        var result = recognitionService.recognize(currentFaceImage);

        if (result.isRecognized()) {
            personDetailsView.setPerson(result.getPerson());
            cardLayout.show(displayPanel, "DETAILS");
            updateButtonVisibility(false);
        } else {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Person not recognized. Add them?", "New Face", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                showAddPersonDialog();
            }
        }
    }

    private void showAddPersonDialog() {
        JTextField nameField = new JTextField();
        JTextField relField = new JTextField();
        nameField.setFont(AppTheme.P_LABEL_FONT);
        relField.setFont(AppTheme.P_LABEL_FONT);

        Object[] msg = {"Name:", nameField, "Relationship:", relField};

        int op = JOptionPane.showConfirmDialog(this, msg, "Add New Person", JOptionPane.OK_CANCEL_OPTION);
        if (op == JOptionPane.OK_OPTION) {
            String name = util.FileHandler.capitalizeLabel(nameField.getText().trim());
            String rel = relField.getText().trim();

            Person p = new Person(name, rel);
            p.setId(personManager.generateId(persons));
            // Save image using ImageHandler
            imageHandler.saveFaceImage(p.getId(), currentFaceImage);

            // Allow Person object to store a BufferedImage for runtime display if needed
            p.setPersonImage(ImageUtils.matToBufferedImage(currentFaceImage));

            persons.add(p);
            personManager.savePersons(persons);
            recognitionService.train(persons);

            // Go to details
            personDetailsView.setPerson(p);
            cardLayout.show(displayPanel, "DETAILS");
            updateButtonVisibility(false);
        }
    }

    // --- Interface Implementation ---

    @Override
    public void onContactSelected(Person person) {
        personDetailsView.setPerson(person);
        cardLayout.show(displayPanel, "DETAILS");
        editListButton.setVisible(false); // Hide the list edit button
    }

    @Override
    public void onDeleteContact(Person person) {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + person.getName() + "?");
        if(confirm == JOptionPane.YES_OPTION) {
            persons.remove(person);
            personManager.savePersons(persons);
            imageHandler.deleteFaceImage(person.getId());
            recognitionService.train(persons);

            // Refresh list
            contactListView.displayContacts(persons, getWidth());
        }
    }

    // Recursively apply fonts (Backup if components miss the Theme)
    private void applyFonts(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JButton) comp.setFont(AppTheme.BUTTON_FONT);
            else if (comp instanceof JTextField) comp.setFont(AppTheme.P_LABEL_FONT);
            else if (comp instanceof Container) applyFonts((Container) comp);
        }
    }
}