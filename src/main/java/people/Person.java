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
    private String id;
    private String name;
    private String relationship;
    private BufferedImage personImage;
    private MeetingRecord lastestConv;


    public Person(String name, String relationship) {
        this.name = name;
        this.relationship = relationship;
        this.lastestConv = null;
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

    public String toString(){
        return this.id + "," + this.id + ".png" + "," + this.name + "," + this.relationship;
    }
}