package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class AnimatedBackgroundPanel extends JPanel {

    private BufferedImage background;
    private int xOffset = 0;
    private final int speed = 1; // pixels per frame

    public AnimatedBackgroundPanel() {
        loadImage();
        startAnimation();
    }

    private void loadImage() {
        try {
            URL url = getClass().getResource("/images/smiling1.jpg");
            if (url == null) {
                throw new RuntimeException("Image not found: /images/smiling1.jpg");
            }
            background = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startAnimation() {
        Timer timer = new Timer(16, e -> { // ~60 FPS
            if (background == null) return;

            xOffset -= speed;

            int scaledWidth = getScaledWidth();
            if (Math.abs(xOffset) >= scaledWidth) {
                xOffset = 0;
            }
            repaint();
        });
        timer.start();
    }

    private int getScaledWidth() {
        int panelHeight = getHeight();
        if (panelHeight == 0 || background == null) return 0;

        int topSpace = 60;
        int bottomSpace = 60;
        int imageHeight = panelHeight - topSpace - bottomSpace;

        return background.getWidth() * imageHeight / background.getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (background == null) return;

        int panelHeight = getHeight();
        int scaledWidth = getScaledWidth();

        // Add vertical spacing
        int topSpace = 60;
        int bottomSpace = 60;
        int imageHeight = panelHeight - topSpace - bottomSpace;
        int scaledWidthWithSpacing = background.getWidth() * imageHeight / background.getHeight();

        // Draw two images for seamless infinite scrolling with vertical spacing
        g2d.drawImage(background, xOffset, topSpace, scaledWidthWithSpacing, imageHeight, this);
        g2d.drawImage(background, xOffset + scaledWidthWithSpacing, topSpace, scaledWidthWithSpacing, imageHeight, this);

        // Add gradient overlay for better depth
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(0, 0, 0, 180),
                0, getHeight(), new Color(0, 0, 0, 120)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Add subtle vignette effect (darker edges)
        RadialGradientPaint vignette = new RadialGradientPaint(
                getWidth() / 2f, getHeight() / 2f,
                Math.max(getWidth(), getHeight()) * 0.8f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 100)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Add white center box for better contrast
        int boxWidth = 550;
        int boxHeight = 550;
        int boxX = (getWidth() - boxWidth) / 2;
        int boxY = ((getHeight() - boxHeight) / 2) + 20;

        // shadow
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRoundRect(boxX + 8, boxY + 8, boxWidth, boxHeight, 35, 35);
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.fillRoundRect(boxX + 12, boxY + 12, boxWidth, boxHeight, 35, 35);

        // white box thingy
        g2d.setColor(new Color(255, 255, 255, 250));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 35, 35);

        // Add a very subtle inner glow/highlight at the top
        GradientPaint innerGlow = new GradientPaint(
                boxX, boxY, new Color(255, 255, 255, 50),
                boxX, boxY + 100, new Color(255, 255, 255, 0)
        );
        g2d.setPaint(innerGlow);
        g2d.fillRoundRect(boxX, boxY, boxWidth, 100, 35, 35);

        // Optional: Add a subtle border
        g2d.setColor(new Color(220, 220, 220, 150));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 35, 35);
    }
}