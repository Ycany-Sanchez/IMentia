// 1. Create this inner class
package ui;
import javax.swing.*;
import java.awt.Graphics;
import  java.awt.image.BufferedImage;

class VideoPanel extends JPanel {
    private BufferedImage image;

    public void updateImage(BufferedImage newImage) {
        this.image = newImage;
        this.repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
        }
    }
}