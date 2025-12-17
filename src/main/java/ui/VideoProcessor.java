package ui;

import util.exceptions.NoCamException;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

class VideoProcessor extends JPanel {
    private BufferedImage image;
    private VideoCapture camera;
    private Mat currentFrame;
    private CascadeClassifier faceDetector;
    private Rect currentFaceRect;

    public VideoProcessor() {
        try {
            faceDetector = loadFaceDetector();
        } catch (IOException e) {
            System.err.println("Failed to load face detector: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateImage(BufferedImage newImage) {
        this.image = newImage;
        this.repaint();
    }

    public void startCamera() throws NoCamException {
        System.out.println("Starting camera...");
        camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("ERROR: Cannot open camera!");
            throw new NoCamException("Hardware Error: Cannot open camera (index 0). Please check connection.");
        }

        System.out.println("Camera opened successfully");

        Thread cameraThread = new Thread(() -> {
            System.out.println("Camera thread started");
            Mat frame = new Mat();
            Mat grayFrame = new Mat();

            int frameCount = 0;
            int missedDetectionCount = 0;
            int maxMissedDetections = 60;

            while (true) {
                if (!camera.read(frame)) {
                    continue;
                }

                currentFrame = frame.clone();

                if (frameCount % 4 == 0) { // Detects every 4 frames. 5 is too inaccurate.
                    cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

                    RectVector detections = new RectVector();
                    if (faceDetector != null && !faceDetector.empty()) {
                        faceDetector.detectMultiScale(grayFrame, detections);
                    }

                    int numFaces = (int) detections.size();

                    if (numFaces > 0) {
                        Rect[] faces = new Rect[numFaces];
                        for (int i = 0; i < numFaces; i++) {
                            Rect temp = detections.get(i);

                            // Needed to recreate Rect bypass some weird c++ memory limit thing to improve performance
                            faces[i] = new Rect(temp.x(), temp.y(), temp.height(), temp.width());
                        }
                        currentFaceRect = getBiggestFace(faces);
                        missedDetectionCount = 0;
                    } else {
                        missedDetectionCount++;
                        if (missedDetectionCount > maxMissedDetections) {
                            currentFaceRect = null;
                        }
                    }
                }
                frameCount++;

                if (currentFaceRect != null) {
                    rectangle(
                            frame,
                            new Point(currentFaceRect.x(), currentFaceRect.y()),
                            new Point(currentFaceRect.x() + currentFaceRect.width(),
                                    currentFaceRect.y() + currentFaceRect.height()),
                            new Scalar(0, 255, 0, 0),
                            3, LINE_8, 0
                    );
                }

                BufferedImage bufferedImage = ImageUtils.matToBufferedImage(frame);

                SwingUtilities.invokeLater(() -> {
                    updateImage(bufferedImage);
                });

                try {
                    Thread.sleep(16); // 60fps camera feed, performance effect not known for now
                } catch (InterruptedException e) {
                    System.out.println("Camera thread interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            camera.release();
            System.out.println("Camera thread stopped");
        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private static CascadeClassifier loadFaceDetector() throws IOException {
        System.out.println("Loading face detector from resources...");
        InputStream is = MainPanel.class.getResourceAsStream("/haarcascade_frontalface_default.xml");

        if (is == null) {
            System.out.println("Could not find haarcascade file in resources!");
            return null;
        }

        // Needed temp file because haarcascade is on the resources folder, as by the InputStream
        File tempFile = File.createTempFile("haarcascade", ".xml");
        tempFile.deleteOnExit();

        try (FileOutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } finally {
            is.close();
        }

        System.out.println("Temp file created at: " + tempFile.getAbsolutePath());

        CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());

        if (classifier.empty()) {
            System.out.println("Classifier is empty! XML loading failed.");
            return null;
        } else {
            return classifier;
        }
    }

    private Rect getBiggestFace(Rect[] faces) {
        if (faces == null || faces.length == 0) return null;
        Rect biggest = faces[0];
        int maxArea = biggest.width() * biggest.height();

        for (int i = 1; i < faces.length; i++) {
            int area = faces[i].width() * faces[i].height();
            if (area > maxArea) {
                maxArea = area;
                biggest = faces[i];
            }
        }
        return biggest;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }

    public Rect getCurrentFaceRect() {
        return currentFaceRect;
    }

    public Mat getCurrentFrame() {
        return currentFrame;
    }
}