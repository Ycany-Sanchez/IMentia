// >>> FILE: src/main/java/ui/PersonFormDialog.java
package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.bytedeco.opencv.opencv_core.Mat;
import people.FaceData;
import people.Person;
import util.ImageUtils;

public class PersonFormDialog extends JDialog {
    private JTextField nameField;
    private JTextField relationshipField;
    private JButton saveButton;
    private JButton cancelButton;
    private Mat faceImage;
    private Person person;
    private boolean confirmed = false;

    public PersonFormDialog(JFrame parent, Mat faceImage) {
        super(parent, "Add New Person", true);
        this.faceImage = faceImage; // The extracted face image Mat
        this.setupUI();
    }

    private void setupUI() {
        this.setSize(400, 300);
        this.setLocationRelativeTo(this.getParent());
        this.setLayout(new BorderLayout(10, 10));
        JLabel facePreview = new JLabel();
        facePreview.setIcon(new ImageIcon(ImageUtils.matToBufferedImage(this.faceImage)));
        facePreview.setHorizontalAlignment(0);
        this.add(facePreview, "North");
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Arial", 1, 16));
        this.nameField = new JTextField();
        this.nameField.setFont(new Font("Arial", 0, 16));
        JLabel relationshipLabel = new JLabel("Relationship:");
        relationshipLabel.setFont(new Font("Arial", 1, 16));
        this.relationshipField = new JTextField();
        this.relationshipField.setFont(new Font("Arial", 0, 16));
        formPanel.add(nameLabel);
        formPanel.add(this.nameField);
        formPanel.add(relationshipLabel);
        formPanel.add(this.relationshipField);
        this.add(formPanel, "Center");
        JPanel buttonPanel = new JPanel(new FlowLayout(1, 20, 10));
        this.saveButton = new JButton("SAVE");
        this.saveButton.setFont(new Font("Arial", 1, 18));
        this.saveButton.addActionListener((e) -> this.save());
        this.cancelButton = new JButton("CANCEL");
        this.cancelButton.setFont(new Font("Arial", 1, 18));
        this.cancelButton.addActionListener((e) -> this.cancel());
        buttonPanel.add(this.saveButton);
        buttonPanel.add(this.cancelButton);
        this.add(buttonPanel, "South");
    }

    /**
     * Handles the SAVE button click: validates input, creates the Person object,
     * adds the captured face as FaceData, and sets the confirmed flag.
     */
    private void save() {
        String name = this.nameField.getText().trim();
        String relationship = this.relationshipField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name!");
        } else {
            String id = UUID.randomUUID().toString();
            // 1. Create the new Person object
            this.person = new Person(id, name, relationship);

            // 2. Convert the captured face Mat into serializable FaceData
            FaceData faceData = ImageUtils.matToFaceData(this.faceImage, (byte[])null);

            // 3. Add the FaceData to the Person's face list
            this.person.addFace(faceData);

            this.confirmed = true;
            this.dispose(); // Close the dialog
        }
    }

    private void cancel() {
        this.confirmed = false;
        this.dispose();
    }

    public boolean isConfirmed() {
        return this.confirmed;
    }

    public Person getPerson() {
        return this.person;
    }
}
