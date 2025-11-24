package people;

import java.io.Serializable;
import java.util.Date;

public class FaceData implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] imageBytes;
    private int imageWidth;
    private int imageHeight;
    private int imageType;
    private byte[] encoding;
    private Date capturedAt;

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
