import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import javax.swing.SwingUtilities;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import ui.MainWindow;

public class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true));
        System.setErr(new PrintStream(System.err, true));
        System.out.println("===== APPLICATION STARTING =====");
        System.out.println("OpenCV Version: " + opencv_core.CV_VERSION);
        CascadeClassifier faceDetector = loadFaceDetector();
        if (faceDetector == null) {
            System.out.println("Error loading face detector!");
        } else {
            System.out.println("Face detector loaded successfully");
            SwingUtilities.invokeLater(() -> {
                System.out.println("Creating main window...");
                MainWindow window = new MainWindow(faceDetector);
                window.setVisible(true);
                System.out.println("Window displayed");
            });
        }
    }

    private static CascadeClassifier loadFaceDetector() {
        try {
            System.out.println("Loading face detector from resources...");
            InputStream is = Main.class.getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) {
                System.out.println("Could not find haarcascade file in resources!");
                return null;
            } else {
                File tempFile = File.createTempFile("haarcascade", ".xml");
                tempFile.deleteOnExit();
                FileOutputStream os = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];

                int bytesRead;
                while((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
                System.out.println("Temp file created at: " + tempFile.getAbsolutePath());
                CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());
                if (classifier.empty()) {
                    System.out.println("Classifier is empty!");
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
