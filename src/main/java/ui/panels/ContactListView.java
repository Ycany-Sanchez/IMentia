package ui.panels;

import people.Person;
import ui.AppTheme;
import ui.listeners.ContactSelectionListener;
import util.ImageHandler;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ContactListView extends JPanel {
    private ContactSelectionListener listener;
    private ImageHandler imageHandler;
    private boolean isEditMode = false;

    public ContactListView(ImageHandler imageHandler, ContactSelectionListener listener) {
        this.imageHandler = imageHandler;
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        // Re-render components to show/hide delete buttons
        revalidate();
        repaint();
    }

    public void displayContacts(List<Person> persons, int parentWidth) {
        removeAll();
        int numPersons = persons.size();

        for (int i = 0; i < numPersons; i += 2) {
            Box rowBox = Box.createHorizontalBox();
            rowBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel card1 = createPersonCard(persons.get(i), parentWidth);
            rowBox.add(card1);
            rowBox.add(Box.createHorizontalStrut(20));

            if (i + 1 < numPersons) {
                JPanel card2 = createPersonCard(persons.get(i + 1), parentWidth);
                rowBox.add(card2);
                rowBox.add(Box.createHorizontalGlue());
            } else {
                rowBox.add(Box.createHorizontalGlue());
            }
            add(rowBox);
        }
        revalidate();
        repaint();
    }

    private JPanel createPersonCard(Person person, int parentWidth) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        final int FIXED_HEIGHT = 200;
        final int MIN_WIDTH = (parentWidth - 60) / 2;
        panel.setPreferredSize(new Dimension(MIN_WIDTH, FIXED_HEIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Image
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(160, 160));
        ImageIcon icon = imageHandler.loadPersonIcon(person.getId(), 160, 160);
        if (icon != null) imageLabel.setIcon(icon);
        else imageLabel.setText("No Image");

        // Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(person.getName());
        nameLabel.setFont(AppTheme.H_LABEL_FONT);
        JLabel relLabel = new JLabel("Relationship: " + person.getRelationship());
        relLabel.setFont(AppTheme.P_LABEL_FONT);

        infoPanel.add(nameLabel);
        infoPanel.add(relLabel);

        // Delete Button
        JButton deleteButton = new JButton("DELETE");
        deleteButton.setFont(AppTheme.H_LABEL_FONT);
        deleteButton.setForeground(Color.RED);
        // We override paintComponent or use logic in parent to toggle visibility
        // But for simplicity, we can just set visibility here based on a flag passed down
        // However, standard Swing is easier if we just toggle it later.
        // For this refactor, let's keep it visible if isEditMode is true.
        deleteButton.setVisible(isEditMode);

        deleteButton.addActionListener(e -> {
            if(listener != null) listener.onDeleteContact(person);
        });

        // Layout Constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.VERTICAL; gbc.insets = new Insets(0,0,0,10);
        panel.add(imageLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(infoPanel, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(deleteButton, gbc);

        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if(listener != null) listener.onContactSelected(person);
            }
        });

        return panel;
    }
}