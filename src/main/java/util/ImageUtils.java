// >>> FILE: src/main/java/util/ImageUtils.java
package util;

import people.FaceData;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.awt.image.BufferedImage;

public class ImageUtils {

    public static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        int type = (channels > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(width, height, type);

        UByteIndexer indexer = mat.createIndexer();
        // UPDATED: Added try-finally to ensure indexer release
        try {
            if (channels == 3) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int b = indexer.get(y, x, 0);
                        int g = indexer.get(y, x, 1);
                        int r = indexer.get(y, x, 2);

                        int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                        image.setRGB(x, y, rgb);
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int gray = indexer.get(y, x, 0);
                        int rgb = ((gray & 0xFF) << 16) | ((gray & 0xFF) << 8) | (gray & 0xFF);
                        image.setRGB(x, y, rgb);
                    }
                }
            }
        } finally {
            if (indexer != null) {
                indexer.release();
            }
        }
        return image;
    }

    public static FaceData matToFaceData(Mat mat, byte[] encoding) {
        System.out.println("Converting Mat to FaceData:");
        System.out.println("  Size: " + mat.cols() + "x" + mat.rows());
        System.out.println("  Channels: " + mat.channels());

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        UByteIndexer indexer = mat.createIndexer();
        byte[] bytes = new byte[width * height * channels];

        // UPDATED: Added try-finally to ensure indexer release
        try {
            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int c = 0; c < channels; c++) {
                        bytes[idx++] = (byte) indexer.get(y, x, c);
                    }
                }
            }
        } finally {
            if (indexer != null) {
                indexer.release();
            }
        }

        System.out.println("  Byte array size: " + bytes.length);

        return new FaceData(bytes, width, height, channels, encoding);
    }

    public static Mat faceDataToMat(FaceData faceData) {
        System.out.println("Converting FaceData to Mat:");
        System.out.println("  Stored size: " + faceData.getImageWidth() + "x" + faceData.getImageHeight());
        System.out.println("  Channels: " + faceData.getImageType());

        int width = faceData.getImageWidth();
        int height = faceData.getImageHeight();
        int channels = faceData.getImageType();

        int cvType = (channels > 1) ? org.bytedeco.opencv.global.opencv_core.CV_8UC3 :
                org.bytedeco.opencv.global.opencv_core.CV_8UC1;

        Mat mat = new Mat(height, width, cvType);
        UByteIndexer indexer = mat.createIndexer();
        byte[] bytes = faceData.getImageBytes();

        // UPDATED: Added try-finally to ensure indexer release
        try {
            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int c = 0; c < channels; c++) {
                        indexer.put(y, x, c, bytes[idx++] & 0xFF);
                    }
                }
            }
        } finally {
            if (indexer != null) {
                indexer.release();
            }
        }

        System.out.println("  Reconstructed: " + mat.cols() + "x" + mat.rows() + ", channels=" + mat.channels());

        return mat;
    }

    public static Mat loadMatFromFile(String filePath) {
        Mat mat = imread(filePath);
        if (mat.empty()) {
            System.err.println("ImageUtils: Failed to load Mat from file: " + filePath);
            return null;
        }
        return mat;
    }
}