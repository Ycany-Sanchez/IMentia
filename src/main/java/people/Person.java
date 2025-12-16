// >>> FILE: src/main/java/people/Person.java
package people;

import java.awt.image.BufferedImage;
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
    private BufferedImage personImage;
    private MeetingRecord lastestConv;


    public Person(String name, String relationship) {
        this.name = name;
        this.relationship = relationship;
        this.lastestConv = null;
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

    public BufferedImage getPersonImage(){
        return personImage;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setPersonImage(BufferedImage image){
        personImage = image;
    }

    public MeetingRecord newConversation(String conv){

        MeetingRecord information = new MeetingRecord(this, conv);
        this.lastestConv = information;
        return information;

    }

    public MeetingRecord getMeetingRecord(){
        return lastestConv;

    }
    public String toString(){
        return this.id + "," + this.id + ".png" + "," + this.name + "," + this.relationship;
    }
}