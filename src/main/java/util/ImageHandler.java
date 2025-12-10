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

    @Override
    public void updatePersons(List<Person> persons) {
        ensureFacesFolderExists();
        File dir = new File(Paths.get(DATA_FOLDER, FACES_FOLDER).toString());

        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null) return;

        for (File file : files) {
            String id = file.getName().replace(".png", "");
            boolean existsInCsv = persons.stream().anyMatch(p -> p.getId().equals(id));
            if (!existsInCsv) {
                file.delete(); // remove orphaned image
            }
        }
    }

    @Override
    public boolean save(Object obj) {
        if (!(obj instanceof Person)) return false;
        Person p = (Person) obj;
        try {
            String directoryPath = Paths.get(DATA_FOLDER, "saved_faces").toString();
            File dir = new File(directoryPath);
            if (!dir.exists()) dir.mkdirs();

            File imgFile = new File(dir, p.getId() + ".png");
            if (p.getPersonImage() != null) {
                ImageIO.write(p.getPersonImage(), "png", imgFile);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Object load() {
        System.out.println("Bulk image load not implemented");
        return null;
    }

    public static void saveFaceImage(String personID, Mat imageToSave) {
        String directoryPath = "imentia_data/saved_faces/";
        String filePath = directoryPath + personID + ".png";

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        boolean isSaved = imwrite(filePath, imageToSave);

        if (isSaved) {
            System.out.println("Image successfully saved to: " + filePath);
        } else {
            System.out.println("Failed to save image.");
            // JOptionPane.showMessageDialog(mainPanel, "Error saving image to disk.");
        }
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
