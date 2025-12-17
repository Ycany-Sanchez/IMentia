// src/main/java/util/ImageHandler.java
package util;

import org.bytedeco.opencv.opencv_core.Mat;
import people.Person;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class ImageHandler extends FileHandler {



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
