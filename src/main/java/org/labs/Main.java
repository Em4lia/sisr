package org.labs;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.gui.TreeViewer;
import java.util.Arrays;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends JFrame {
    private JTextArea inputTextBox;
    private JTextArea outputTextBox;

    public Main() {
        setTitle("Java IDE - Lab 2 (Лексичний + синтаксичний аналізатор)");
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inputTextBox = new JTextArea(
                "class Main extends Parent {\n" +
                        "    int[] arr;\n" +
                        "    \n" +
                        "    void process() {\n" +
                        "        // Перевірка масивів\n" +
                        "        arr[0] = 10;\n" +
                        "        \n" +
                        "        // Перевірка обробки об'єктів\n" +
                        "        Point obj = new Point();\n" +
                        "        obj.x = 25.5;\n" +
                        "        obj.calculate();\n" +
                        "        \n" +
                        "        // Перевірка циклів і switch\n" +
                        "        while(arr[0] < 20) {\n" +
                        "            switch(arr[0]) {\n" +
                        "                case 10: arr[0] = arr[0] + 1; break;\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );

        inputTextBox.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputTextBox);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Вхідний код"));

        TextLineNumber tln = new TextLineNumber(inputTextBox);
        inputScroll.setRowHeaderView(tln);

        outputTextBox = new JTextArea();
        outputTextBox.setEditable(false);
        outputTextBox.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputTextBox);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Результат аналізу"));

        JButton analyzeButton = new JButton("Виконати аналіз");
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
            javaLLexer lexer = new javaLLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            javaLParser parser = new javaLParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(new org.antlr.v4.runtime.BaseErrorListener() {
                @Override
                public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        org.antlr.v4.runtime.RecognitionException e) {
                    outputTextBox.append(String.format( ">>> СИНТАКСИЧНА ПОМИЛКА (Рядок %d:%d): %s\n", line, charPositionInLine, msg));
                }
            });

            ParseTree tree = parser.program();

            if (parser.getNumberOfSyntaxErrors() == 0) {
                outputTextBox.append("Синтаксичний аналіз успішно завершено!\n");
                outputTextBox.append("Дерево побудовано. Відкриття вікна візуалізації...\n");

                showTreeWindow(parser, tree);
            }

        } catch (Exception ex) {
            outputTextBox.append("Критична помилка: " + ex.getMessage());
        }
    }

    private void showTreeWindow(javaLParser parser, ParseTree tree) {
        JFrame treeFrame = new JFrame("Abstract Syntax Tree");
        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
        viewer.setScale(1.5);
        JScrollPane scrollPane = new JScrollPane(viewer);
        treeFrame.add(scrollPane);
        treeFrame.setSize(600, 400);
        treeFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}