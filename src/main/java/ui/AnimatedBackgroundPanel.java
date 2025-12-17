package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

public class AnimatedBackgroundPanel extends JPanel {

    private BufferedImage background;
    private int xOffset = 0;
    private final int speed = 1; // pixels per frame
    private int scaledWidth;


    public AnimatedBackgroundPanel() {
        loadImage();
        startAnimation();
    }

    private void loadImage() {
        try {
            // Need to use URL instead of our usual Paths.get because we have the image thingy inside the resources for JAR.
            URL url = getClass ().getResource( "/images/smiling1.jpg");
            if (url == null) {
                throw new FileNotFoundException("Image not found: /images/smiling1.jpg");
            }
            background = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startAnimation() {
        // made it slower, down to around I think 14.9 fps ish para dili labad sa ulo
        // and 67
        Timer timer = new Timer(67, e -> {
            if (background == null) return;

            xOffset -= speed;

            if (Math.abs(xOffset) >= scaledWidth) {
                xOffset = 0;
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (background == null) return;

        // Removed method getScaledWidth and integrated it directly as an attribute para di na mag sigeg call method
        // For imageHeight, we are doing 60 padding for top and bottom, so 120. Adjust this if needed more padding
        int imageHeight = getHeight() - 120;
        scaledWidth = background.getWidth() * imageHeight / background.getHeight();

        int padding = 60;
        // Draw two images for seamless infinite scrolling with vertical spacing
        // Like... image1 first, then image2, then image1, and so on
        g2d.drawImage(background, xOffset, padding, scaledWidth, imageHeight, this);
        g2d.drawImage(background, xOffset + scaledWidth, padding, scaledWidth, imageHeight, this);

        // For shadow
        g2d.setPaint(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // For box with greyish white
        int boxWidth = 550;
        int boxHeight = 550;
        int boxX = (getWidth() - boxWidth) / 2;
        int boxY = ((getHeight() - boxHeight) / 2) + 20;

        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 35, 35);
        g2d.fillRoundRect(boxX, boxY, boxWidth, 100, 35, 35);
    }
}