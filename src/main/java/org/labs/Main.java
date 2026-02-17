package org.labs;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends JFrame {
    private JTextArea inputTextBox;
    private JTextArea outputTextBox;

    public Main() {
        setTitle("Java IDE - Lab 1 (Лексичний аналізатор)");
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inputTextBox = new JTextArea(
                "class Test extends Parent {\n" +
                        "    int a = 10;\n" +
                        "    double b = 25.5;\n" +
                        "    string a = \"a1b2c3\";\n" +
                        "    int[] arr; // масив\n\n" +
                        "    void main() {\n" +
                        "        while (a < 20) {\n" +
                        "           switch(a) {\n" +
                        "               case 10: a = a + 1; break;\n" +
                        "           }\n" +
                        "        }\n" +
                        "        /* Багаторядковий\n" +
                        "           коментар */\n" +
                        "    }\n" +
                        "}"
        );

        inputTextBox.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputTextBox);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Вхідний код"));

        outputTextBox = new JTextArea();
        outputTextBox.setEditable(false);
        outputTextBox.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputTextBox);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Результат лексичного аналізу"));

        JButton analyzeButton = new JButton("Виконати лексичний аналіз");
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAnalysis();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
        splitPane.setDividerLocation(300);

        add(splitPane, BorderLayout.CENTER);
        add(analyzeButton, BorderLayout.SOUTH);
    }

    private void startAnalysis() {
        String code = inputTextBox.getText();
        outputTextBox.setText("");

        try {
            var charStream = CharStreams.fromString(code);

            org.labs.javaL lexer = new org.labs.javaL(charStream);

            while (true) {
                Token token = lexer.nextToken();
                if (token.getType() == Token.EOF) {
                    break;
                }

                String ruleName = org.labs.javaL.VOCABULARY.getSymbolicName(token.getType());

                int line = token.getLine();
                int charPosition = token.getCharPositionInLine();

                if ("UNCLOSED_STRING".equals(ruleName) || "UNCLOSED_SINGLE_STRING".equals(ruleName)) {
                    outputTextBox.append(String.format(">>> ПОМИЛКА (Рядок %d, Позиція %d): Пропущені закриваючі лапки для тексту: %s\n",
                            line, charPosition, token.getText()));
                }
                else if ("ERR".equals(ruleName)) {
                    outputTextBox.append(String.format(">>> ПОМИЛКА (Рядок %d, Позиція %d): Недопустимий символ '%s'\n",
                            line, charPosition, token.getText()));
                }
                else {
                    outputTextBox.append(String.format("Token: %-15s | Рядок: %-3d | Value: %s\n",
                            ruleName, line, token.getText()));
                }
            }

            outputTextBox.append("--- Аналіз завершено ---\n");

        } catch (Exception ex) {
            outputTextBox.setText("Помилка: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}