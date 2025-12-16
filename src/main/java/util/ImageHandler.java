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

    private static final String FACES_FOLDER = "imentia_data/saved_faces/";

    // Ensure the faces folder exists
    public void ensureFacesFolderExists() {
        File dir = new File(Paths.get(DATA_FOLDER, FACES_FOLDER).toString());
        if (!dir.exists()) dir.mkdirs();
    }


    public static Mat loadMatFromFile(String filePath) {
        Mat mat = imread(filePath);
        // Check if the Mat object is empty (meaning the file was not found or failed to load)
        if (mat.empty()) {
            System.err.println("ImageUtils: Failed to load Mat from file: " + filePath);
            return null;
        }
        return mat;
    }

    public BufferedImage loadImage(String personId) {
        try {
            String directoryPath = Paths.get(DATA_FOLDER, "saved_faces").toString();
            File imgFile = new File(directoryPath, personId + ".png");
            if (imgFile.exists()) {
                return ImageIO.read(imgFile);
            }
        } catch (IOException e) {
            System.out.println("Could not load image for " + personId);
        }
        return null;
    }
}
