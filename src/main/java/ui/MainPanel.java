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
    private JFrame tempFrame = new JFrame();


    public MainPanel() {
        mainPanel.removeAll();
        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        gbc.weighty = 0.8;
        mainPanel.add(DisplayPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.2;
        mainPanel.add(ButtonPanel, gbc);


        DisplayPanel.setLayout(cardLayout);

        DisplayPanel.add(CameraPanel, "1");
        DisplayPanel.add(ContactsPanel, "2");


        tempFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        System.out.println(tempFrame.getWidth() + " " + tempFrame.getHeight());

        DisplayPanel.setMaximumSize(new Dimension(0, 0));
        DisplayPanel.setMaximumSize(new Dimension(tempFrame.getWidth(), tempFrame.getHeight()));
        ButtonPanel.setMaximumSize(new Dimension(tempFrame.getWidth(), 1));



        cardLayout.show(DisplayPanel, "1");
        EditContactButton.setFont(new Font("", Font.BOLD, 24));

        VIEWCONTACTSButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "2");
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

                if(isEditing){
                    EditContactButton.setText("CANCEL EDIT");
                    EditContactButton.setFont(new Font("", Font.BOLD, 24));
                }


                else{
                    EditContactButton.setText("EDIT LIST");
                    EditContactButton.setFont(new Font("", Font.BOLD, 24));
                }

                for (Component c : PersonPanel.getComponents())
                {
                    if (c instanceof JPanel itemPanel) {
                        for (Component inner : itemPanel.getComponents()) {
                            if (inner instanceof JButton deleteButton) {
                                deleteButton.setVisible(isEditing);
                            }
                        }
                    }
                }
            }
        });
        BACKTOCAMERAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(DisplayPanel, "1");
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
