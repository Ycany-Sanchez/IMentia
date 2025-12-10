package ui;

import people.Person;
import people.MeetingRecord;

import javax.swing.*;
import java.awt.*;
import java.util.List;


import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

/**
 * Abstract base class for panels that handle
 * camera display, contacts, and person details.
 * Concrete subclasses (like MainPanel) must implement
 * the abstract methods to provide actual behavior.
 */
public abstract class AbstractMainPanel extends JPanel {

    // Core UI panels
    protected JPanel mainPanel;
    protected JPanel cameraPanel;
    protected JPanel contactsPanel;
    protected JPanel displayPanel;
    protected JPanel buttonPanel;

    // Core buttons
    protected JButton capturePhotoButton;
    protected JButton viewContactsButton;
    protected JButton tutorialButton;
    protected JButton backToCameraButton;
    protected JButton savePersonInfoButton;
    protected JButton addMeetingNotesButton;

    // Core labels
    protected JLabel personNameLabel;
    protected JLabel personImageLabel;
    protected JLabel personRelationshipLabel;
    protected JLabel meetingNotesLabel;

    // Core text fields and areas
    protected JTextField personNameField;
    protected JTextField personRelationshipField;
    protected JTextArea meetingNotesTextArea;

    // Fonts
    protected Font buttonFont = new Font("", Font.BOLD, 24);
    protected Font headerLabelFont = new Font("", Font.BOLD, 20);
    protected Font plainLabelFont = new Font("", Font.PLAIN, 20);

    // State
    protected Person currentDisplayedPerson;
    protected List<Person> persons;
    protected CardLayout cardLayout = new CardLayout();

    // === Abstract methods ===

    /** Initialize all UI components and layouts. */
    protected abstract void setUpUI();

    /** Refresh the contacts panel with the latest person list. */
    protected abstract void refreshContactsPanel();

    /** Capture a face from the camera feed. */
    protected abstract boolean captureFace();

    /** Save a face image to disk. */
    protected abstract void saveFaceImage(String personID, Mat image);

    /** Show details for a given person. */
    protected abstract void setupPersonDetailsForm(Person p);

    /** Display meeting notes for a given person. */
    protected abstract void displayMeetingNotes(Person person);

    /** Delete a contact from the list. */
    protected abstract void deleteContact(Person personToDelete);

    /** Update panel sizes when the window is resized. */
    protected abstract void updatePanelSizes();

    /** Toggle delete buttons in the contacts list. */
    protected abstract void toggleDeleteButton();

    /** Get the root panel for embedding in a frame. */
    public JPanel getPanel() {
        return mainPanel;
    }
}