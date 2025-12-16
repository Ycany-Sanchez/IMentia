package util.exceptions;
/*
 * Thrown when saving a new person fails
 */
public class PersonSaveException extends Exception {
    private final String personName;

    public PersonSaveException(String message, String personName) {
        super(message);
        this.personName = personName;
    }

    public PersonSaveException(String message, String personName, Throwable cause) {
        super(message, cause);
        this.personName = personName;
    }

    public String getPersonName() {
        return personName;
    }

}

