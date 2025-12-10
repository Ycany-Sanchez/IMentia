package util;

import org.bytedeco.opencv.opencv_core.Mat;
import util.ImageUtils;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import javax.swing.ImageIcon;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class ImageHandler extends FileHandler {
    private static final String IMG_FOLDER = "saved_faces";

    public ImageHandler() {
        File folder = Paths.get(DATA_FOLDER, IMG_FOLDER).toFile();
        if (!folder.exists()) folder.mkdirs();
    }

    public void saveFaceImage(String personId, Mat image) {
        String path = Paths.get(DATA_FOLDER, IMG_FOLDER, personId + ".png").toString();
        imwrite(path, image);
    }

    public void deleteFaceImage(String personId) {
        File file = Paths.get(DATA_FOLDER, IMG_FOLDER, personId + ".png").toFile();
        if (file.exists()) file.delete();
    }

    public ImageIcon loadPersonIcon(String personId, int width, int height) {
        String path = Paths.get(DATA_FOLDER, IMG_FOLDER, personId + ".png").toString();
        File file = new File(path);

        if (file.exists()) {
            Mat faceMat = ImageUtils.loadMatFromFile(path);
            if (faceMat != null && !faceMat.empty()) {
                BufferedImage bi = ImageUtils.matToBufferedImage(faceMat);
                Image scaled = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        }
        return null; // Handle default image in UI if null
    }
}