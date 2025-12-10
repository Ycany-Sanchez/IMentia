package ui.panels;

import people.Person;
import ui.AppTheme;
import util.ImageHandler;
import util.MeetingNotesHandler;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;

public class PersonDetailsView extends JPanel {

    private ImageHandler imageHandler;
    private MeetingNotesHandler notesHandler;

    private JLabel imageLabel;
    private JLabel nameLabel;
    private JLabel relLabel;

    private JTextArea inputNoteArea;
    private JButton addNoteButton;
    private JPanel notesListPanel;
    private JScrollPane notesScrollPane;

    private Person currentPerson;
    private Runnable onUpdateCallback; // To tell MainPanel data changed

    public PersonDetailsView(ImageHandler imgHandler, MeetingNotesHandler noteHandler, Runnable onUpdate) {
        this.imageHandler = imgHandler;
        this.notesHandler = noteHandler;
        this.onUpdateCallback = onUpdate;

        setLayout(new BorderLayout());
        setupUI();
    }

    private void setupUI() {
        // --- Top Section (Image + Info) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(200, 200));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        nameLabel = new JLabel();
        nameLabel.setFont(AppTheme.H_LABEL_FONT);
        relLabel = new JLabel();
        relLabel.setFont(AppTheme.H_LABEL_FONT);

        JButton editButton = new JButton("EDIT DETAILS");
        editButton.setFont(AppTheme.BUTTON_FONT);
        editButton.addActionListener(e -> showEditDialog(currentPerson));

        infoPanel.add(nameLabel);
        infoPanel.add(relLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(editButton);

        topPanel.add(imageLabel);
        topPanel.add(infoPanel);

        add(topPanel, BorderLayout.NORTH);

        // --- Bottom Section (Notes) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Notes List
        notesListPanel = new JPanel();
        notesListPanel.setLayout(new BoxLayout(notesListPanel, BoxLayout.Y_AXIS));
        notesListPanel.setBackground(Color.WHITE);

        notesScrollPane = new JScrollPane(notesListPanel);
        notesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Input Area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputNoteArea = new JTextArea(3, 20);
        inputNoteArea.setFont(AppTheme.P_LABEL_FONT);
        inputNoteArea.setLineWrap(true);
        inputNoteArea.setText("Add meeting notes here...");
        inputNoteArea.setForeground(Color.GRAY);

        // Focus listener for placeholder
        inputNoteArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if(inputNoteArea.getText().equals("Add meeting notes here...")) {
                    inputNoteArea.setText("");
                    inputNoteArea.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if(inputNoteArea.getText().isEmpty()) {
                    inputNoteArea.setForeground(Color.GRAY);
                    inputNoteArea.setText("Add meeting notes here...");
                }
            }
        });

        addNoteButton = new JButton("ADD NOTE");
        addNoteButton.setFont(AppTheme.BUTTON_FONT);
        addNoteButton.addActionListener(e -> saveNote());

        inputPanel.add(new JScrollPane(inputNoteArea), BorderLayout.CENTER);
        inputPanel.add(addNoteButton, BorderLayout.EAST);

        bottomPanel.add(new JLabel("Previous Notes:"), BorderLayout.NORTH);
        bottomPanel.add(notesScrollPane, BorderLayout.CENTER);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.CENTER);
    }

    public void setPerson(Person p) {
        this.currentPerson = p;
        nameLabel.setText(p.getName());
        relLabel.setText(p.getRelationship());

        ImageIcon icon = imageHandler.loadPersonIcon(p.getId(), 200, 200);
        if(icon != null) imageLabel.setIcon(icon);

        refreshNotes();
    }

    private void refreshNotes() {
        notesListPanel.removeAll();
        List<String> notes = notesHandler.loadNotes(currentPerson);

        if (notes.isEmpty()) {
            JLabel empty = new JLabel("No notes yet.");
            empty.setFont(AppTheme.P_LABEL_FONT);
            notesListPanel.add(empty);
        } else {
            for (String note : notes) {
                notesListPanel.add(createNoteComponent(note));
            }
        }
        notesListPanel.revalidate();
        notesListPanel.repaint();
    }

    private JComponent createNoteComponent(String text) {
        JTextArea noteArea = new JTextArea(text);
        noteArea.setBackground(Color.WHITE);
        noteArea.setFocusable(false);
        noteArea.setEditable(false);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(AppTheme.P_LABEL_FONT);

        // >>> STYLE REQUEST IMPLEMENTATION <<<
        // 1. Bottom Line (custom grey)
        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.NOTE_BORDER_COLOR);
        // 2. Padding
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        // 3. Compound
        noteArea.setBorder(BorderFactory.createCompoundBorder(bottomLine, padding));

        noteArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, noteArea.getPreferredSize().height));

        return noteArea;
    }

    private void saveNote() {
        String text = inputNoteArea.getText().trim();
        if (!text.isEmpty() && !text.equals("Add meeting notes here...")) {
            notesHandler.saveNote(currentPerson, text);
            inputNoteArea.setText("Add meeting notes here...");
            inputNoteArea.setForeground(Color.GRAY);
            refreshNotes();
            JOptionPane.showMessageDialog(this, "Note Saved!");
        }
    }

    public void showEditDialog(Person person) { // <-- CHANGED: Now public and accepts a Person argument
        if (person == null) {
            JOptionPane.showMessageDialog(this, "Error: No contact data available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create the input fields, ensuring font consistency
        JTextField nameField = new JTextField(person.getName(), 15);
        JTextField relField = new JTextField(person.getRelationship(), 15);

        nameField.setFont(AppTheme.P_LABEL_FONT);
        relField.setFont(AppTheme.P_LABEL_FONT);

        // Setup the panel for the dialog
        JPanel editPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        editPanel.add(new JLabel("New Name:"));
        editPanel.add(nameField);
        editPanel.add(new JLabel("New Relationship:"));
        editPanel.add(relField);

        int result = JOptionPane.showConfirmDialog(
                this,
                editPanel,
                "Edit Contact Details",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newRel = relField.getText().trim();

            if (newName.isEmpty() || newRel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Relationship cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 1. Update the person object
            person.setName(util.FileHandler.capitalizeLabel(newName));
            person.setRelationship(newRel);

            // 2. Notify the MainPanel (Controller) to save the changes to disk
            if (onUpdateCallback != null) {
                onUpdateCallback.run();

                // 3. Refresh this view with the new data
                setPerson(person);

                JOptionPane.showMessageDialog(this, "Contact details updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error: Update callback missing.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}