package util;

import people.FaceData;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.indexer.UByteIndexer;
// *** NEW IMPORT ***
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.awt.image.BufferedImage;

/**
 * Utility class for managing conversions between different image formats used
 * in the application:
 * 1. OpenCV's Mat (for processing)
 * 2. Java's BufferedImage (for display in Swing)
 * 3. Custom FaceData (for persistence)
 */
public class ImageUtils {

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

        indexer.release();
        return image;
    }

    /**
     * Converts an OpenCV Mat object (a face image) into a serializable FaceData object.
     * @param mat The source face Mat.
     * @param encoding Optional face encoding bytes (unused in this prototype).
     * @return The resulting FaceData object.
     */
    public static FaceData matToFaceData(Mat mat, byte[] encoding) {
        System.out.println("Converting Mat to FaceData:");
        System.out.println("  Size: " + mat.cols() + "x" + mat.rows());
        System.out.println("  Channels: " + mat.channels());

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        UByteIndexer indexer = mat.createIndexer();
        // Calculate required size for the raw pixel byte array: Width * Height * Channels
        byte[] bytes = new byte[width * height * channels];

        // Linearize the Mat's pixel data into a single byte array
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    // Cast unsigned byte from Mat to signed byte for the array
                    bytes[idx++] = (byte) indexer.get(y, x, c);
                }
            }
        }

        indexer.release();

        System.out.println("  Byte array size: " + bytes.length);

        return new FaceData(bytes, width, height, channels, encoding);
    }

    /**
     * Reconstructs an OpenCV Mat object from the raw data stored in a FaceData object.
     * This is used to load faces from disk for model retraining/recognition.
     * @param faceData The source FaceData object.
     * @return The reconstructed Mat.
     */
    public static Mat faceDataToMat(FaceData faceData) {
        System.out.println("Converting FaceData to Mat:");
        System.out.println("  Stored size: " + faceData.getImageWidth() + "x" + faceData.getImageHeight());
        System.out.println("  Channels: " + faceData.getImageType());

        int width = faceData.getImageWidth();
        int height = faceData.getImageHeight();
        int channels = faceData.getImageType();

        // Determine the OpenCV Mat type based on the channel count
        int cvType = (channels > 1) ? org.bytedeco.opencv.global.opencv_core.CV_8UC3 : // 3-channel, 8-bit, unsigned
                org.bytedeco.opencv.global.opencv_core.CV_8UC1; // 1-channel, 8-bit, unsigned

        // Create an empty Mat with the stored dimensions and type
        Mat mat = new Mat(height, width, cvType);

        UByteIndexer indexer = mat.createIndexer();
        byte[] bytes = faceData.getImageBytes();

        // Copy the linear byte array back into the 3D structure of the Mat via the Indexer
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    // Use & 0xFF to convert signed byte back to unsigned integer for Mat indexer
                    indexer.put(y, x, c, bytes[idx++] & 0xFF);
                }
            }
        }

        indexer.release();

        System.out.println("  Reconstructed: " + mat.cols() + "x" + mat.rows() + ", channels=" + mat.channels());

        return mat;
    }

    /**
     * Loads an image directly from the file system into an OpenCV Mat object.
     * This is required by MainPanel.createPersonEntryPanel to display saved photos.
     * @param filePath The path to the image file (e.g., "saved_faces/ID.png").
     * @return The loaded Mat, or null if loading fails.
     */
    public static Mat loadMatFromFile(String filePath) {
        Mat mat = imread(filePath);
        // Check if the Mat object is empty (meaning the file was not found or failed to load)
        if (mat.empty()) {
            System.err.println("ImageUtils: Failed to load Mat from file: " + filePath);
            return null;
        }
        return mat;
    }
}