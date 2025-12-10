// >>> FILE: src/main/java/Main.java
import ui.MainPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Run UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            JFrame myFrame = new JFrame("IMentia");

            // MainPanel is now a JPanel, so we add it directly
            MainPanel mainPanel = new MainPanel();

            myFrame.setContentPane(mainPanel);
            myFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            myFrame.setVisible(true);
            myFrame.setResizable(false);
        });
    }
}