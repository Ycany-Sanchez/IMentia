import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import javax.swing.*;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import ui.MainPanel;
import ui.MainWindow;

public class Main {
    public static void main(String[] args) {
//        JFrame myFrame = new JFrame("IMentia");
//        MainPanel mainPanel = new MainPanel();
//        myFrame.setContentPane(mainPanel.getPanel());
//        myFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        myFrame.setVisible(true);
//        myFrame.setResizable(false);



        // --- APPLICATION INITIALIZATION ---
        // Direct System.out and System.err to ensure proper logging/output handling.
        System.setOut(new PrintStream(System.out, true));
        System.setErr(new PrintStream(System.err, true));
        System.out.println("===== APPLICATION STARTING =====");
        System.out.println("OpenCV Version: " + opencv_core.CV_VERSION);

        // 1. Load the pre-trained Haar Cascade XML file for face detection.
        CascadeClassifier faceDetector = loadFaceDetector();

        if (faceDetector == null) {
            System.out.println("Error loading face detector! Application cannot start.");
        } else {
            System.out.println("Face detector loaded successfully");

            // 2. Start the Swing UI on the Event Dispatch Thread (EDT).
            SwingUtilities.invokeLater(() -> {
                System.out.println("Creating main window...");
                // Pass the loaded CascadeClassifier to the main window.
                MainWindow window = new MainWindow(faceDetector);
                window.setVisible(true);
                System.out.println("Window displayed");
            });
        }
    }

    /**
     * Loads the Haar Cascade XML file from the classpath resources and creates a physical
     * temporary file, as Bytedeco/OpenCV's CascadeClassifier requires a file path, not a stream.
     * @return A loaded CascadeClassifier or null if loading failed.
     */
    private static CascadeClassifier loadFaceDetector() {
        try {
            System.out.println("Loading face detector from resources...");
            // STEP 1: Get the resource as an InputStream (from JAR/classpath)
            InputStream is = Main.class.getResourceAsStream("/haarcascade_frontalface_default.xml");

            if (is == null) {
                System.out.println("Could not find haarcascade file in resources!");
                return null;
            } else {
                // STEP 2: Create a temporary file on the filesystem
                File tempFile = File.createTempFile("haarcascade", ".xml");
                tempFile.deleteOnExit(); // Ensure the temp file is deleted on exit
                FileOutputStream os = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];

                // STEP 3: Copy data from resource stream to the temporary file
                int bytesRead;
                while((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
                System.out.println("Temp file created at: " + tempFile.getAbsolutePath());

                // STEP 4: Initialize the CascadeClassifier using the temp file's path
                CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());

                if (classifier.empty()) {
                    System.out.println("Classifier is empty! XML loading failed.");
                    return null;
                } else {
                    return classifier;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while loading face detector:");
            e.printStackTrace();
            return null;
        }
    }
}
