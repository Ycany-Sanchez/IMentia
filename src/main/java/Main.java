import javax.swing.*;

import ui.MainPanel;

public class Main {
    public static void main(String[] args) {
        JFrame myFrame = new JFrame("IMentia");
        MainPanel mainPanel = new MainPanel();
        myFrame.setContentPane(mainPanel.getPanel());
        myFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        myFrame.setVisible(true);
        myFrame.setResizable(false);

        // For Facade Design Pattern Implementations, they are usually documented in a more formatted comment
        // Check on the PersonRecognitionManager class
    }
}
