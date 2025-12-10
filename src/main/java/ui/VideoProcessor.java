// >>> FILE: src/main/java/ui/VideoProcessor.java (COMPLETE & CORRECTED)
package ui;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class VideoProcessor extends JPanel {

    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private JLabel cameraScreen;

    // 1. ADDED: Field to track the camera thread state
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private Mat currentFrame;
    private Rect currentFaceRect;

    public VideoProcessor() {
        setLayout(new BorderLayout());
        cameraScreen = new JLabel();
        cameraScreen.setHorizontalAlignment(SwingConstants.CENTER);
        add(cameraScreen, BorderLayout.CENTER);

        // Initialize logic
        currentFrame = new Mat();
        loadCascade();
    }

    public void startCamera() {
        if (isRunning.get()) return;

        // Start camera in a separate thread to avoid freezing UI
        new Thread(() -> {
            capture = new VideoCapture(0); // 0 is usually the default webcam

            if (!capture.isOpened()) {
                System.err.println("Error: Could not open camera.");
                return;
            }

            isRunning.set(true);
            Mat rawFrame = new Mat();
            Mat grayFrame = new Mat();

            while (isRunning.get()) {
                if (capture.read(rawFrame)) {
                    // 1. Clone for external use
                    synchronized (this) {
                        currentFrame = rawFrame.clone();
                    }

                    // 2. Detect Faces
                    cvtColor(rawFrame, grayFrame, COLOR_BGR2GRAY);
                    org.bytedeco.opencv.opencv_core.RectVector faces = new org.bytedeco.opencv.opencv_core.RectVector();

                    if (faceDetector != null && !faceDetector.empty()) {
                        faceDetector.detectMultiScale(grayFrame, faces, 1.1, 3, 0, null, null);
                    }

                    // 3. Draw Rectangle & Update State
                    if (faces.size() > 0) {
                        Rect face = faces.get(0);

                        synchronized (this) {
                            currentFaceRect = face;
                        }

                        // Draw Green Rectangle on Display
                        rectangle(rawFrame,
                                new Point(face.x(), face.y()),
                                new Point(face.x() + face.width(), face.y() + face.height()),
                                new Scalar(0, 255, 0, 0), // Green
                                2, 0, 0);
                    } else {
                        synchronized (this) {
                            currentFaceRect = null;
                        }
                    }

                    // 4. Convert to Swing Image and Paint
                    BufferedImage image = ImageUtils.matToBufferedImage(rawFrame);
                    SwingUtilities.invokeLater(() -> {
                        if (image != null) {
                            // Scale to fit the panel
                            Image scaled = image.getScaledInstance(getWidth(), getHeight(), Image.SCALE_FAST);
                            cameraScreen.setIcon(new ImageIcon(scaled));
                        }
                    });
                }
            }

            capture.release();
        }).start();
    }

    // 2. ADDED: Public method to stop the camera thread cleanly
    public void stopCamera() {
        isRunning.set(false);
    }

    // 3. ADDED: Public method to check the camera thread state
    public boolean isRunning() {
        return isRunning.get();
    }

    // Thread-safe getter for the Capture button
    public synchronized Rect getCurrentFaceRect() {
        return currentFaceRect;
    }

    // Thread-safe getter for saving the image
    public synchronized Mat getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Loads the HAAR Cascade from resources into a temp file so OpenCV C++ can read it.
     */
    private void loadCascade() {
        try {
            InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) {
                System.err.println("Error: haarcascade xml not found in resources!");
                return;
            }

            File tempFile = File.createTempFile("haarcascade", ".xml");
            tempFile.deleteOnExit();

            try (FileOutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());
            System.out.println("Face detector loaded.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}