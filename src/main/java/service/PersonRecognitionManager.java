// >>> FILE: src/main/java/service/PersonRecognitionManager.java (Updated)

package service;

import people.Person;
import util.FileHandler;
import util.ImageHandler;
import util.ImageUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import util.PersonDataManager;

import java.io.File;
import java.nio.file.Paths; // Required for Path operations
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
     * Facade Method: Ensures the given Person object has its BufferedImage loaded
     * from disk. If the image is already set, it does nothing.
     * (Needed for OOP-compliant image loading in MainPanel)
     */
    public void ensureImageLoaded(Person person) {
        if (person.getPersonImage() != null) {
            return; // Already loaded
        }

        try {
            // Use the FileHandler (PersonDataManager) to resolve the path
            String directoryPath = fileHandler.getDataFolder() + "/saved_faces";
            String filePath = Paths.get(directoryPath, person.getId() + ".png").toString();

            Mat faceMat = ImageHandler.loadMatFromFile(filePath);

            if (faceMat != null && !faceMat.empty()) {
                person.setPersonImage(ImageUtils.matToBufferedImage(faceMat));
            } else {
                System.out.println("Could not load image file for ID: " + person.getId() + ". File not found or empty.");
            }
        } catch (Exception e) {
            System.err.println("Error loading image for person " + person.getId() + ": " + e.getMessage());
        }
    }


    /**
     * Facade Method: Coordinates deleting a person from memory, file, and disk.
     */
    public void deletePerson(Person person) {
        cachedPersons.remove(person);
        fileHandler.updatePersonFile(cachedPersons);
        recognitionService.train(cachedPersons);

        // Delete face image file
        try {
            File imageFile = new File(fileHandler.getDataFolder() + "/saved_faces", person.getId() + ".png");
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    System.out.println("Manager: Deleted image file for " + person.getId());
                } else {
                    System.err.println("Manager: Failed to delete image file for " + person.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Manager: Error deleting image file: " + e.getMessage());
        }

        // ** NEW: Delete meeting notes file **
        try {
            String notesPath = Paths.get(fileHandler.getDataFolder(), "Meeting_Notes", person.getId() + ".txt").toString();
            File notesFile = new File(notesPath);
            if (notesFile.exists()) {
                if (notesFile.delete()) {
                    System.out.println("Manager: Deleted notes file for " + person.getId());
                } else {
                    System.err.println("Manager: Failed to delete notes file for " + person.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Manager: Error deleting notes file: " + e.getMessage());
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