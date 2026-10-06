import javax.swing.*;

import ui.MainPanel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static void main(String[] args) {
        JFrame myFrame = new JFrame("IMentia");
        MainPanel mainPanel = new MainPanel();
        myFrame.setContentPane(mainPanel.getPanel());
        myFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainPanel.shutdown();
            }
        });

        myFrame.setVisible(true);
        myFrame.setResizable(false);

        // For Facade Design Pattern Implementations, they are usually documented in a more formatted comment
        // Check on the PersonRecognitionManager class
    }
}
