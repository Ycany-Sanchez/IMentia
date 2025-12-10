// >>> FILE: src/main/java/people/Person.java
package people;

import java.awt.image.BufferedImage;
import java.io.Serializable;

/**
 * Person is the model for a recognized individual.
 * It is now a lightweight "Plain Old Java Object" (POJO).
 * * - Images are handled by util.ImageHandler
 * - Notes are handled by util.MeetingNotesHandler
 */
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String relationship;

    // "transient" prevents this from being serialized if you ever save this object directly.
    // We store this here only for UI display purposes during runtime.
    private transient BufferedImage personImage;

    public Person(String name, String relationship) {
        this.name = name;
        this.relationship = relationship;
    }

    // --- GETTERS AND SETTERS ---

    public String getId() {
        return this.id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return this.relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public BufferedImage getPersonImage(){
        return personImage;
    }

    public void setPersonImage(BufferedImage image){
        personImage = image;
    }

    @Override
    public String toString(){
        return "ID: " + this.id + " | Name: " + this.name;
    }
}