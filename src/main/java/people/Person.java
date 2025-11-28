// >>> FILE: src/main/java/people/Person.java
package people;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Person is the model for a recognized individual, storing their metadata
 * and a list of captured face samples (FaceData) used for training.
 */
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String relationship;
    private FaceData face; // List of face samples for this person

    public Person(String id, String name, String relationship) {
        this.id = id;
        this.name = name;
        this.relationship = relationship;
        this.face = null;
    }

    /**
     * Adds a new FaceData sample to this person's training set.
     * @param face The FaceData object to add.
     */
    public void addFace(FaceData face) {
        this.face = face;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getRelationship() {
        return this.relationship;
    }

    public FaceData getFace() {
        return face;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}