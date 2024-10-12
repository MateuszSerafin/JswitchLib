package com.gmail.genek530.logger;

import java.awt.BorderLayout;
import java.util.logging.*;
import javax.swing.*;

public class LoggingMain {

    private static Logger logger;

    public static Logger getLogger() {
        if(logger == null){
            logger = Logger.getLogger("");
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }
            ActualGui app = new ActualGui();
            CustomSwingHandler customSwingHandler = new CustomSwingHandler(app.getTextArea());
            logger.addHandler(customSwingHandler);
        }
        return logger;
    }
}

class CustomSwingHandler extends Handler {

    private Formatter formatter = new SimpleFormatter();
    private JTextArea area;

    public CustomSwingHandler(JTextArea area){
        this.area = area;
    }

    @Override
    public void publish(LogRecord record) {
        SwingUtilities.invokeLater(() -> {
            area.append(formatter.format(record));
        });
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}

class ActualGui {
    private JFrame frame;
    private JTextArea textArea;

    public ActualGui() {
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("LOGGER");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        textArea = new JTextArea();


        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public JTextArea getTextArea() {
        return textArea;
    }
}