// src/main/java/util/ImageHandler.java
package util;

import people.Person;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ImageHandler extends FileHandler {

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
