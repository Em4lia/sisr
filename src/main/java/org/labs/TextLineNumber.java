package org.labs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.*;

public class TextLineNumber extends JPanel {
    private final JTextArea textArea;

    public TextLineNumber(JTextArea textArea) {
        this.textArea = textArea;
        setBackground(new Color(230, 230, 230));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updatePanel(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updatePanel(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updatePanel(); }
        });
    }

    private void updatePanel() {
        setPreferredSize(new Dimension(35, textArea.getHeight()));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Font textFont = textArea.getFont();
        g.setFont(textFont);

        FontMetrics fm = g.getFontMetrics(textFont);
        int lineHeight = fm.getHeight();
        int startY = textArea.getInsets().top + fm.getAscent();

        Element root = textArea.getDocument().getDefaultRootElement();
        int lineCount = root.getElementCount();

        if (getPreferredSize().height != textArea.getHeight()) {
            setPreferredSize(new Dimension(35, textArea.getHeight()));
            revalidate();
        }

        for (int i = 0; i < lineCount; i++) {
            int y = startY + (i * lineHeight);
            g.drawString(String.valueOf(i + 1), 5, y);
        }
    }
}