package util;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.javacpp.indexer.UByteIndexer;

import java.awt.image.BufferedImage;

public class ImageUtils {
    /** Standard face size fed to the recognizer. */
    public static final int FACE_SIZE = 100;

    /**
     * Standard preprocessing applied to every face before training AND
     * recognition (both paths must match): grayscale, resize to
     * FACE_SIZE x FACE_SIZE, histogram equalization for lighting robustness.
     * @param face BGR or grayscale face crop. Not modified.
     * @return New single-channel FACE_SIZE x FACE_SIZE Mat. Caller owns it.
     */
    public static Mat preprocessFace(Mat face) {
        Mat gray = new Mat();
        if (face.channels() > 1) {
            opencv_imgproc.cvtColor(face, gray, opencv_imgproc.COLOR_BGR2GRAY);
        } else {
            gray = face.clone();
        }

        Mat resized = new Mat();
        opencv_imgproc.resize(gray, resized, new Size(FACE_SIZE, FACE_SIZE));

        Mat equalized = new Mat();
        opencv_imgproc.equalizeHist(resized, equalized);
        return equalized;
    }

    // Note: this class had some more methods we did not end up using or stopped using.
    /**
     * Converts an OpenCV Mat object into a Java BufferedImage for display.
     * This method manually copies pixel data using an Indexer.
     * @param mat The source Mat.
     * @return The resulting BufferedImage.
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        int type = (channels > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(width, height, type);

        // UByteIndexer provides fast, direct access to the Mat's underlying pixel data
        UByteIndexer indexer = mat.createIndexer();

        try
        {
            if (channels == 3) { // 3-channel (BGR/Color) image
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // OpenCV Mat stores pixels in BGR order
                        int b = indexer.get(y, x, 0);
                        int g = indexer.get(y, x, 1);
                        int r = indexer.get(y, x, 2);

                        // Java BufferedImage stores pixels in ARGB/RGB order
                        int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                        image.setRGB(x, y, rgb);
                    }
                }
            } else { // 1-channel (Grayscale) image
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int gray = indexer.get(y, x, 0);
                        // Set all R, G, B components to the same gray value
                        int rgb = ((gray & 0xFF) << 16) | ((gray & 0xFF) << 8) | (gray & 0xFF);
                        image.setRGB(x, y, rgb);
                    }
                }
            }
        }
        finally
        {
            if (indexer != null) {
                indexer.release();
            }
        }
        return image;
    }
}