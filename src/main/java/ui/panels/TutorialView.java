package ui.panels;

import ui.AppTheme;
import javax.swing.*;
import java.awt.*;

public class TutorialView extends JPanel {

    public TutorialView() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setupUI();
    }

    private void setupUI() {
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));

        JLabel titleLabel = new JLabel("Welcome to IMentia");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        headerPanel.add(titleLabel);

        add(headerPanel, BorderLayout.NORTH);

        String htmlContent = "<html><body style='width: 100%; font-family: sans-serif;'>" +
                "<div style='padding: 60px;'>" +
                "<p style='font-size: 18px; color: #444;'><b>IMentia</b> is a supportive memory assistant...</p>" +
                "<hr style='margin-top: 30px; margin-bottom: 30px;'>" +
                "<h3>How to use:</h3>" +
                "<p><b>1. Position yourself...</b></p>" + // (Abbreviated for brevity, paste your full HTML here)
                "</div></body></html>";

        JLabel textLabel = new JLabel(htmlContent);
        textLabel.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scrollPane = new JScrollPane(textLabel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }
}