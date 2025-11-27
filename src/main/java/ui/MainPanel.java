package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainPanel {
    private JPanel mainPanel;
    private JButton CAPTUREPHOTOButton;
    private JButton VIEWCONTACTSButton;
    private JButton TUTORIALButton;
    private JPanel CameraPanel;
    private JPanel ContactsPanel;
    private JPanel TutorialPanel;
    private JPanel PersonForm;
    private JPanel RecognizedForm;
    private JButton EditContactButton;
    private JButton deleteButton;
    private JPanel DisplayPanel;
    private JPanel ButtonPanel;
    private JPanel PersonPanel;
    private JButton BACKTOCAMERAButton;

    private CardLayout cardLayout = new CardLayout();

    private boolean isEditing = false;

    public MainPanel() {
        DisplayPanel.setLayout(cardLayout);
        DisplayPanel.add(ContactsPanel, "1");
        DisplayPanel.add(CameraPanel, "2");
        EditContactButton.setText(String.format(
                "<html><center>" +
                        "<h4 style='font-size: 24px; margin: 10px;'><b>EDIT LIST</b></h1>" +
                        "</center></html>"));

        cardLayout.show(DisplayPanel, "2");

        VIEWCONTACTSButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "1");
                BACKTOCAMERAButton.setVisible(true);
                CAPTUREPHOTOButton.setVisible(false);
                TUTORIALButton.setVisible(false);
                VIEWCONTACTSButton.setVisible(false);
            }
        });


        EditContactButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isEditing = !isEditing;

                if(isEditing) EditContactButton.setText(String.format(
                        "<html><center>" +
                        "<h4 style='font-size: 24px; margin: 10px;'>CANCEL EDIT</h1>" +
                        "</center></html>"));

                else EditContactButton.setText(
                        "<html><center>" +
                                "<h4 style='font-size: 24px; margin: 10px;'><b>EDIT LIST</b></h1>" +
                        "</center></html>");

                for (Component c : PersonPanel.getComponents())
                {
                    if (c instanceof JPanel itemPanel) {
                        for (Component inner : itemPanel.getComponents()) {
                            if (inner instanceof JButton deleteButton) {
                                deleteButton.setVisible(isEditing);
                            }
                            if(inner instanceof JLabel label)
                            {
                                label.setHorizontalAlignment(SwingConstants.LEFT);
                            }
                        }
                    }
                }
            }
        });
        BACKTOCAMERAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "2");
                BACKTOCAMERAButton.setVisible(false);
                CAPTUREPHOTOButton.setVisible(true);
                TUTORIALButton.setVisible(true);
                VIEWCONTACTSButton.setVisible(true);
            }
        });
    }

    public JPanel getPanel(){
        return mainPanel;
    }

}
