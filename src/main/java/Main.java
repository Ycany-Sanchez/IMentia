import ui.MainPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame myFrame = new JFrame("IMentia");

            // Create the controller
            MainPanel mainController = new MainPanel();

            // Fix: Get the bound panel from the controller
            if (mainController.getPanel() != null) {
                myFrame.setContentPane(mainController.getPanel());
            } else {
                System.err.println("CRITICAL ERROR: MainPanel failed to initialize UI.");
            }

            myFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            myFrame.setVisible(true);
            myFrame.setResizable(true);
        });
    }
}