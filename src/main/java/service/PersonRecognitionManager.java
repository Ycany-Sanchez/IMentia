package service;

import people.Person;
import util.FileHandler;
import util.ImageHandler;
import util.ImageUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import util.PersonDataManager;
import util.exceptions.PersonAlreadyExistsException;
import util.exceptions.PersonSaveException;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * FACADE PATTERN IMPLEMENTATION
 *
 * This class acts as the "Manager" Facade. It encapsulates the complexity
 * of synchronizing the FileHandler (Storage) and FaceRecognitionService (AI).
 * Bisag mura siya, by name, going to be mostly about Managing recognition ONLY,
 * it coordinates the data flow to the FaceRecognition
 * This is especially important because the Person class has a lot of data that needs to be handled.
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
     * Coordinates loading data from disk and retraining the AI model.
     */
    public void refreshDataAndTrain() {
        this.cachedPersons = fileHandler.loadPersons();
        this.recognitionService.train(cachedPersons);
    }

    public List<Person> getAllPersons() {
        return cachedPersons;
    }

    /**
     * Pass on the image analysis to the AI service.
     */
    public FaceRecognitionService.RecognitionResult recognizeFace(Mat faceImage) {
        if (faceImage == null || faceImage.empty()) {
            return new FaceRecognitionService.RecognitionResult(null, -1, "No Image");
        }
        return recognitionService.recognize(faceImage);
    }

    /**
     * Encapsulates the complex transaction of creating a new person.
     * 1. Capitalizes inputs
     * 2. Generates ID
     * 3. Sets Image
     * 4. Saves to CSV
     * 5. Saves Image to Disk
     * 6. Retrains face recog
     */
    public Person registerNewPerson(String name, String relationship, Mat faceImage) throws PersonSaveException {

        if (name == null || name.isBlank()) {
            throw new PersonSaveException("Name cannot be empty.", name);
        }

        if (relationship == null || relationship.isBlank()) {
            throw new PersonSaveException("Relationship cannot be empty.", name);
        }

        if (faceImage == null || faceImage.empty()) {
            throw new PersonSaveException("Face image is missing.", name);
        }

        // --- Duplicate check using cachedPersons ---
        for (Person p : cachedPersons) {
            if (p.getName().equalsIgnoreCase(name)) {
                throw new PersonAlreadyExistsException(name);
            }
        }

        String capitalizedName = FileHandler.capitalizeLabel(name);

        try {
            Person newPerson = new Person(capitalizedName, FileHandler.capitalizeLabel(relationship));

            newPerson.setId(fileHandler.generateId(cachedPersons));
            newPerson.setPersonImage(ImageUtils.matToBufferedImage(faceImage));

            cachedPersons.add(newPerson);

            // Save persons to CSV
            if (!fileHandler.savePersons(cachedPersons)) {
                throw new PersonSaveException("Failed to save person data.", capitalizedName);
            }

            // Save face image and retrain model
            saveFaceImageToDisk(newPerson.getId(), faceImage);
            recognitionService.train(cachedPersons);
            return newPerson;

        } catch (PersonAlreadyExistsException e) {
            throw e; // pass through to UI

        } catch (Exception e) {
            throw new PersonSaveException(
                    "Unexpected error while saving person.",
                    capitalizedName,
                    e
            );
        }
    }

    /**
     * Handles updates to person details and persistence.
     */
    public void updatePersonDetails(Person person) {
        fileHandler.updatePersonFile(cachedPersons);
    }

    /**
     * Ensures the given Person object has its BufferedImage loaded
     * from disk. If the image is already set, it does nothing.
     * Needed kay there are times na the image is not loaded.
     * However, this method makes it so that we do not need to do the checks ourselves.
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
     * Coordinates deleting a person from memory, file, and disk.
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