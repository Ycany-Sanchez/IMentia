package ui;

import util.exceptions.NoCamException;
import javax.swing.*;
import java.awt.*;

public class CameraLifecycleManager {

    private final VideoProcessor videoProcessor;
    private final Component parentComponent;
    private boolean active = false;

    public CameraLifecycleManager(VideoProcessor videoProcessor, Component parentComponent) {
        this.videoProcessor = videoProcessor;
        this.parentComponent = parentComponent;
    }

    public void startCamera() {
        if (!active) {
            try {
                videoProcessor.startCamera();
                active = true;
            } catch (NoCamException e) {
                JOptionPane.showMessageDialog(parentComponent,
                        e.getMessage(),
                        "Camera Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    public void stopCamera() {
        if (active) {
            videoProcessor.stopCamera();
            active = false;
        }
    }
}