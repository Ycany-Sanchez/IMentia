package util.exceptions;

public class PersonAlreadyExistsException extends PersonSaveException {
    private final String personName;

    public PersonAlreadyExistsException(String personName) {
        super("Name already exists in contacts.", personName);
        this.personName = personName;
    }

    public String getPersonName() {
        return personName;
    }
}
