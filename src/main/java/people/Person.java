package people;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String relationship;
    private List<FaceData> faces;

    public Person(String id, String name, String relationship) {
        this.id = id;
        this.name = name;
        this.relationship = relationship;
        this.faces = new ArrayList();
    }

    public void addFace(FaceData face) {
        this.faces.add(face);
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

    public List<FaceData> getFaces() {
        return this.faces;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
