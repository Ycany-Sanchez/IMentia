// >>> FILE: src/main/java/service/PersonRecognitionManager.java
package service;

import people.Person;
import util.FileHandler;
import util.ImageUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import util.PersonDataManager;

import java.io.File;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * FACADE PATTERN IMPLEMENTATION
 *
 * This class acts as the "Manager" Facade. It encapsulates the complexity
 * of synchronizing the FileHandler (Storage) and FaceRecognitionService (AI).
 *
 * The UI (MainPanel) interacts ONLY with this class for data operations,
 * unaware of the underlying CSV parsing, image decoding, or model training.
 */
public class PersonRecognitionManager {

    private final PersonDataManager fileHandler;
    private final FaceRecognitionService recognitionService;
    private List<Person> cachedPersons;

    public PersonRecognitionManager() {
        this.fileHandler = new PersonDataManager();
        this.recognitionService = new FaceRecognitionService();

        // Initialize system state immediately
        refreshDataAndTrain();
    }

    /**
     * Facade Method: Coordinates loading data from disk and retraining the AI model.
     */
    public void refreshDataAndTrain() {
        this.cachedPersons = fileHandler.loadPersons();
        this.recognitionService.train(cachedPersons);
    }

    public List<Person> getAllPersons() {
        return cachedPersons;
    }

    /**
     * Facade Method: Delegates image analysis to the AI service.
     */
    public FaceRecognitionService.RecognitionResult recognizeFace(Mat faceImage) {
        if (faceImage == null || faceImage.empty()) {
            return new FaceRecognitionService.RecognitionResult(null, -1, "No Image");
        }
        return recognitionService.recognize(faceImage);
    }

    /**
     * Facade Method: Encapsulates the complex transaction of creating a new person.
     * 1. Capitalizes inputs
     * 2. Generates ID
     * 3. Sets Image
     * 4. Saves to CSV
     * 5. Saves Image to Disk
     * 6. Retrains AI
     */
    public Person registerNewPerson(String name, String relationship, Mat faceImage) {
        String capitalizedName = FileHandler.capitalizeLabel(name);
        String capitalizedRel = FileHandler.capitalizeLabel(relationship);

        Person newPerson = new Person(capitalizedName, capitalizedRel);

        // 1. Generate ID
        newPerson.setId(fileHandler.generateId(cachedPersons));

        // 2. Set Image for Runtime
        newPerson.setPersonImage(ImageUtils.matToBufferedImage(faceImage));

        // 3. Add to List
        cachedPersons.add(newPerson);

        // 4. Persist to CSV
        if (fileHandler.savePersons(cachedPersons)) {
            // 5. Save Face Image to Disk
            saveFaceImageToDisk(newPerson.getId(), faceImage);

            // 6. Retrain Model
            recognitionService.train(cachedPersons);
            return newPerson;
        }

        return null; // Failed to save
    }

    /**
     * Facade Method: Handles updates to person details and persistence.
     */
    public void updatePersonDetails(Person person) {
        fileHandler.updatePersonFile(cachedPersons);
        // Note: Changing names doesn't strictly require retraining if ID links to image,
        // but re-saving the file is needed.
    }

    /**
     * Facade Method: Coordinates deleting a person from memory, file, and disk.
     */
    public void deletePerson(Person person) {
        cachedPersons.remove(person);
        fileHandler.updatePersonFile(cachedPersons);
        recognitionService.train(cachedPersons);

        // Delete image file
        try {
            File imageFile = new File(fileHandler.getDataFolder() + "/saved_faces", person.getId() + ".png");
            if (imageFile.exists()) {
                imageFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Manager: Failed to delete image file for " + person.getId());
        }
    }

    // Helper method hidden from the UI
    private void saveFaceImageToDisk(String personID, Mat imageToSave) {
        String directoryPath = fileHandler.getDataFolder() + "/saved_faces/";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String filePath = directoryPath + personID + ".png";
        imwrite(filePath, imageToSave);
    }
}
