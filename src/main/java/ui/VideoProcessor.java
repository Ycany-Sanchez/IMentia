// 1. Create this inner class
package ui;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_objdetect.BaseCascadeClassifier;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.opencv.video.Video;
import util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.Graphics;
import  java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;

class VideoProcessor extends JPanel {
    private BufferedImage image;
    private VideoCapture camera;
    private Mat currentFrame;
    private CascadeClassifier faceDetector;
    private Component dialogParent;
    private Rect currentFaceRect ;


    public VideoProcessor(){
        faceDetector = loadFaceDetector();
    }

    public void updateImage(BufferedImage newImage) {
        this.image = newImage;
        this.repaint();
    }

    public void startCamera() {
        System.out.println("Starting camera...");
        camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("ERROR: Cannot open camera!");
            JOptionPane.showMessageDialog(this.dialogParent, "Cannot open camera!");
            return;
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


                if(frameCount % 4 == 0){
                    cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

                    RectVector detections = new RectVector();
                    faceDetector.detectMultiScale(grayFrame, detections);

                    int numFaces = (int)detections.size();

                    if (numFaces > 0) {
                        Rect[] faces = new Rect[(int)numFaces];
                        for (int i = 0; i < numFaces; i++) {
                            Rect temp = detections.get(i);
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
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    break;
                }
            }

            camera.release();
            System.out.println("Camera thread stopped");
        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private static CascadeClassifier loadFaceDetector() {
        try {
            System.out.println("Loading face detector from resources...");
            InputStream is = MainPanel.class.getResourceAsStream("/haarcascade_frontalface_default.xml");

            if (is == null) {
                System.out.println("Could not find haarcascade file in resources!");
                return null;
            } else {
                File tempFile = File.createTempFile("haarcascade", ".xml");
                tempFile.deleteOnExit();
                FileOutputStream os = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];

                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
                System.out.println("Temp file created at: " + tempFile.getAbsolutePath());

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

    private Rect getBiggestFace(Rect[] faces) {
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
            // Draw the image scaled to fit the entire panel area
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