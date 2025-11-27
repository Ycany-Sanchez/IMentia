// >>> FILE: src/main/java/people/FaceData.java
package people;

import java.io.Serializable;
import java.util.Date;

/**
 * FaceData is a serializable container for a captured face image, designed for
 * persistence and transport within the application.
 */
public class FaceData implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] imageBytes; // Raw pixel data of the face image
    private int imageWidth;    // Width of the face image
    private int imageHeight;   // Height of the face image
    private int imageType;     // Number of channels (e.g., 3 for BGR, 1 for GRAY)
    private byte[] encoding;   // Placeholder for a potential face vector/encoding
    private Date capturedAt;   // Timestamp of capture

    public FaceData(byte[] imageBytes, int width, int height, int type, byte[] encoding) {
        this.imageBytes = imageBytes;
        this.imageWidth = width;
        this.imageHeight = height;
        this.imageType = type;
        this.encoding = encoding;
        this.capturedAt = new Date();
    }

    public byte[] getImageBytes() {
        return this.imageBytes;
    }

    public int getImageWidth() {
        return this.imageWidth;
    }

    public int getImageHeight() {
        return this.imageHeight;
    }

    public int getImageType() {
        return this.imageType;
    }

    public byte[] getEncoding() {
        return this.encoding;
    }

    public Date getCapturedAt() {
        return this.capturedAt;
    }
}